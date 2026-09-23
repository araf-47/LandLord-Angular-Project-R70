package com.landlord.backend.report;

import com.landlord.backend.billing.Invoice;
import com.landlord.backend.billing.InvoiceRepository;
import com.landlord.backend.billing.Payment;
import com.landlord.backend.billing.PaymentRepository;
import com.landlord.backend.maintenance.Expense;
import com.landlord.backend.maintenance.ExpenseRepository;
import com.landlord.backend.property.Property;
import com.landlord.backend.property.PropertyRepository;
import com.landlord.backend.tenant.Tenant;
import com.landlord.backend.tenant.TenantRepository;
import com.landlord.backend.unit.Unit;
import com.landlord.backend.unit.UnitRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Aggregates in-memory over the existing repositories rather than adding
 * @Query SUM/COUNT projections - portfolios are small enough at this stage
 * that loading full entity lists and reducing in Java is simpler and no repo
 * has aggregation precedent yet. Revisit with real projections if data
 * volume grows.
 */
@Service
public class ReportService {

    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final UnitRepository units;
    private final PropertyRepository properties;
    private final ExpenseRepository expenses;
    private final TenantRepository tenants;

    public ReportService(InvoiceRepository invoices, PaymentRepository payments, UnitRepository units,
            PropertyRepository properties, ExpenseRepository expenses, TenantRepository tenants) {
        this.invoices = invoices;
        this.payments = payments;
        this.units = units;
        this.properties = properties;
        this.expenses = expenses;
        this.tenants = tenants;
    }

    public IncomeStatementReport incomeStatement(LocalDate startDate, LocalDate endDate, Long propertyId) {
        Map<Long, Unit> unitsById = units.findAll().stream()
            .collect(Collectors.toMap(Unit::getId, u -> u));
        Map<Long, Property> propertiesById = properties.findAll().stream()
            .collect(Collectors.toMap(Property::getId, p -> p));

        Map<Long, Double> collectedByInvoiceId = payments.findAll().stream()
            .filter(p -> "confirmed".equals(p.getStatus()))
            .collect(Collectors.groupingBy(Payment::getInvoiceId, Collectors.summingDouble(this::amountOf)));

        List<Invoice> scoped = invoices.findAll().stream()
            .filter(inv -> withinRange(inv.getDueDate(), startDate, endDate))
            .filter(inv -> propertyIdOf(inv, unitsById) != null)
            .filter(inv -> propertyId == null || propertyId.equals(propertyIdOf(inv, unitsById)))
            .toList();

        Map<Long, List<Invoice>> byProperty = scoped.stream()
            .collect(Collectors.groupingBy(inv -> propertyIdOf(inv, unitsById)));

        List<IncomeStatementRow> rows = byProperty.entrySet().stream()
            .map(entry -> {
                Long propId = entry.getKey();
                double billed = entry.getValue().stream().mapToDouble(this::amountOf).sum();
                double outstanding = entry.getValue().stream().mapToDouble(this::balanceOf).sum();
                double collected = entry.getValue().stream()
                    .mapToDouble(inv -> collectedByInvoiceId.getOrDefault(inv.getId(), 0.0))
                    .sum();
                Property property = propertiesById.get(propId);
                String name = property == null ? "Unknown property" : property.getName();
                return new IncomeStatementRow(propId, name, billed, collected, outstanding, rate(collected, billed));
            })
            .sorted(Comparator.comparing(IncomeStatementRow::propertyName))
            .toList();

        double totalBilled = rows.stream().mapToDouble(IncomeStatementRow::billed).sum();
        double totalCollected = rows.stream().mapToDouble(IncomeStatementRow::collected).sum();
        double totalOutstanding = rows.stream().mapToDouble(IncomeStatementRow::outstanding).sum();

        return new IncomeStatementReport(startDate, endDate, rows, totalBilled, totalCollected, totalOutstanding,
            rate(totalCollected, totalBilled));
    }

    public ExpenseReport expenseReport(LocalDate startDate, LocalDate endDate, Long propertyId, String category) {
        List<Expense> scoped = expenses.findAll().stream()
            .filter(e -> withinRange(e.getDate(), startDate, endDate))
            .filter(e -> propertyId == null || propertyId.equals(e.getPropertyId()))
            .filter(e -> category == null || category.equalsIgnoreCase(e.getCategory()))
            .toList();

        Map<String, List<Expense>> byCategory = scoped.stream()
            .collect(Collectors.groupingBy(e -> e.getCategory() == null ? "Uncategorized" : e.getCategory()));

        List<ExpenseReportRow> rows = byCategory.entrySet().stream()
            .map(entry -> {
                double landlordAmount = entry.getValue().stream()
                    .filter(e -> "landlord".equals(e.getBearer())).mapToDouble(this::amountOf).sum();
                double tenantAmount = entry.getValue().stream()
                    .filter(e -> "tenant".equals(e.getBearer())).mapToDouble(this::amountOf).sum();
                double total = entry.getValue().stream().mapToDouble(this::amountOf).sum();
                return new ExpenseReportRow(entry.getKey(), landlordAmount, tenantAmount, total, entry.getValue().size());
            })
            .sorted(Comparator.comparing(ExpenseReportRow::category))
            .toList();

        double totalAmount = rows.stream().mapToDouble(ExpenseReportRow::total).sum();
        return new ExpenseReport(startDate, endDate, rows, totalAmount);
    }

    public OccupancyReport occupancyReport(Long propertyId) {
        Map<Long, Property> propertiesById = properties.findAll().stream()
            .collect(Collectors.toMap(Property::getId, p -> p));

        List<Unit> scoped = units.findAll().stream()
            .filter(u -> propertyId == null || propertyId.equals(u.getPropertyId()))
            .toList();

        Map<Long, List<Unit>> byProperty = scoped.stream()
            .filter(u -> u.getPropertyId() != null)
            .collect(Collectors.groupingBy(Unit::getPropertyId));

        List<OccupancyReportRow> rows = byProperty.entrySet().stream()
            .map(entry -> {
                Long propId = entry.getKey();
                int total = entry.getValue().size();
                int occupied = (int) entry.getValue().stream().filter(u -> "occupied".equals(u.getStatus())).count();
                Property property = propertiesById.get(propId);
                String name = property == null ? "Unknown property" : property.getName();
                return new OccupancyReportRow(propId, name, total, occupied, total - occupied, rate(occupied, total));
            })
            .sorted(Comparator.comparing(OccupancyReportRow::propertyName))
            .toList();

        int totalUnits = rows.stream().mapToInt(OccupancyReportRow::totalUnits).sum();
        int totalOccupied = rows.stream().mapToInt(OccupancyReportRow::occupiedUnits).sum();
        return new OccupancyReport(rows, totalUnits, totalOccupied, rate(totalOccupied, totalUnits));
    }

    public TenantLedgerReport tenantLedgerReport(Long tenantId, LocalDate startDate, LocalDate endDate) {
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId is required");
        }
        Tenant tenant = tenants.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Map<Long, Double> collectedByInvoiceId = payments.findByTenantId(tenantId).stream()
            .filter(p -> "confirmed".equals(p.getStatus()))
            .collect(Collectors.groupingBy(Payment::getInvoiceId, Collectors.summingDouble(this::amountOf)));

        List<TenantLedgerRow> rows = invoices.findByTenantId(tenantId).stream()
            .filter(inv -> withinRange(inv.getDueDate(), startDate, endDate))
            .sorted(Comparator.comparing(Invoice::getPeriod))
            .map(inv -> new TenantLedgerRow(inv.getPeriod(), amountOf(inv),
                collectedByInvoiceId.getOrDefault(inv.getId(), 0.0), balanceOf(inv), inv.getStatus(), inv.getDueDate()))
            .toList();

        double totalInvoiced = rows.stream().mapToDouble(TenantLedgerRow::amount).sum();
        double totalPaid = rows.stream().mapToDouble(TenantLedgerRow::paid).sum();
        double totalOutstanding = rows.stream().mapToDouble(TenantLedgerRow::balance).sum();
        return new TenantLedgerReport(tenantId, tenant.getName(), rows, totalInvoiced, totalPaid, totalOutstanding);
    }

    public FullReport fullReport(LocalDate startDate, LocalDate endDate, Long propertyId) {
        return new FullReport(
            incomeStatement(startDate, endDate, propertyId),
            expenseReport(startDate, endDate, propertyId, null),
            occupancyReport(propertyId)
        );
    }

    private Long propertyIdOf(Invoice invoice, Map<Long, Unit> unitsById) {
        Unit unit = invoice.getUnitId() == null ? null : unitsById.get(invoice.getUnitId());
        return unit == null ? null : unit.getPropertyId();
    }

    private double amountOf(Invoice invoice) {
        return invoice.getAmount() == null ? 0.0 : invoice.getAmount();
    }

    private double balanceOf(Invoice invoice) {
        return invoice.getBalance() == null ? 0.0 : invoice.getBalance();
    }

    private double amountOf(Payment payment) {
        return payment.getAmount() == null ? 0.0 : payment.getAmount();
    }

    private double amountOf(Expense expense) {
        return expense.getAmount() == null ? 0.0 : expense.getAmount();
    }

    private boolean withinRange(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null) return start == null && end == null;
        if (start != null && date.isBefore(start)) return false;
        if (end != null && date.isAfter(end)) return false;
        return true;
    }

    private double rate(double collected, double billed) {
        return billed == 0 ? 0 : Math.round((collected / billed) * 10000) / 100.0;
    }
}
