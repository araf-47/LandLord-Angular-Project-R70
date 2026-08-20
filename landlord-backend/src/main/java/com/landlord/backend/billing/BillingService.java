package com.landlord.backend.billing;

import com.landlord.backend.tenant.Tenant;
import com.landlord.backend.tenant.TenantRepository;
import com.landlord.backend.unit.Unit;
import com.landlord.backend.unit.UnitRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Bill-generation math, shared by the manual "Generate bills" button
 * (BillingController) and the 1st-of-month auto-generation job
 * (MonthlyBillingScheduler) — same logic either way, just a different trigger.
 */
@Service
public class BillingService {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final InvoiceRepository invoices;
    private final TenantRepository tenants;
    private final UnitRepository units;

    public BillingService(InvoiceRepository invoices, TenantRepository tenants, UnitRepository units) {
        this.invoices = invoices;
        this.tenants = tenants;
        this.units = units;
    }

    public Invoice generateInvoice(Long tenantId, Double utilitiesTotalOrNull) {
        Tenant tenant = tenants.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        if (tenant.getUnitId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant has no assigned unit");
        }
        Unit unit = units.findById(tenant.getUnitId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));

        String period = YearMonth.now().format(PERIOD_FORMAT);
        double prevUnpaidRolled = invoices.findByTenantIdAndStatusNot(tenant.getId(), "paid").stream()
            .mapToDouble(Invoice::getBalance)
            .sum();
        double rent = unit.getRent() == null ? 0 : unit.getRent();
        double utilitiesTotal = utilitiesTotalOrNull == null ? 0 : utilitiesTotalOrNull;
        double amount = rent + utilitiesTotal + prevUnpaidRolled;

        Invoice invoice = new Invoice();
        invoice.setTenantId(tenant.getId());
        invoice.setUnitId(unit.getId());
        invoice.setPeriod(period);
        invoice.setRent(rent);
        invoice.setUtilitiesTotal(utilitiesTotal);
        invoice.setPrevUnpaidRolled(prevUnpaidRolled);
        invoice.setAmount(amount);
        invoice.setBalance(amount);
        invoice.setStatus("unpaid");
        invoice.setDueDate(LocalDate.now().withDayOfMonth(Math.min(5, LocalDate.now().lengthOfMonth())));

        return invoices.save(invoice);
    }
}
