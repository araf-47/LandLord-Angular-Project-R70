package com.landlord.backend.report;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/** Converts each fixed report's typed JSON shape into the generic ReportTable that PDF/Excel export share. */
@Component
public class ReportTables {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public ReportTable of(IncomeStatementReport report) {
        List<List<String>> rows = report.rows().stream()
            .map(r -> List.of(r.propertyName(), taka(r.billed()), taka(r.collected()), taka(r.outstanding()),
                r.collectionRatePercent() + "%"))
            .toList();
        List<String> totals = List.of("Total", taka(report.totalBilled()), taka(report.totalCollected()),
            taka(report.totalOutstanding()), report.collectionRatePercent() + "%");
        return new ReportTable("Income Statement", rangeLabel(report.startDate(), report.endDate()),
            List.of("Property", "Billed", "Collected", "Outstanding", "Rate"), rows, totals);
    }

    public ReportTable of(ExpenseReport report) {
        List<List<String>> rows = report.rows().stream()
            .map(r -> List.of(r.category(), taka(r.landlordAmount()), taka(r.tenantAmount()), taka(r.total()),
                String.valueOf(r.count())))
            .toList();
        List<String> totals = List.of("Total", "", "", taka(report.totalAmount()), "");
        return new ReportTable("Expense Report", rangeLabel(report.startDate(), report.endDate()),
            List.of("Category", "Landlord-borne", "Tenant-borne", "Total", "Count"), rows, totals);
    }

    public ReportTable of(OccupancyReport report) {
        List<List<String>> rows = report.rows().stream()
            .map(r -> List.of(r.propertyName(), String.valueOf(r.totalUnits()), String.valueOf(r.occupiedUnits()),
                String.valueOf(r.vacantUnits()), r.occupancyRatePercent() + "%"))
            .toList();
        List<String> totals = List.of("Total", String.valueOf(report.totalUnits()),
            String.valueOf(report.totalOccupied()), String.valueOf(report.totalUnits() - report.totalOccupied()),
            report.occupancyRatePercent() + "%");
        return new ReportTable("Occupancy Report", "Current snapshot",
            List.of("Property", "Units", "Occupied", "Vacant", "Rate"), rows, totals);
    }

    public ReportTable of(TenantLedgerReport report) {
        List<List<String>> rows = report.rows().stream()
            .map(r -> List.of(r.period(), taka(r.amount()), taka(r.paid()), taka(r.balance()), r.status(),
                r.dueDate() == null ? "-" : DATE_FORMAT.format(r.dueDate())))
            .toList();
        List<String> totals = List.of("Total", taka(report.totalInvoiced()), taka(report.totalPaid()),
            taka(report.totalOutstanding()), "", "");
        return new ReportTable("Tenant Ledger - " + report.tenantName(), "All invoices",
            List.of("Period", "Invoiced", "Paid", "Balance", "Status", "Due date"), rows, totals);
    }

    public List<ReportTable> of(FullReport report) {
        return List.of(of(report.incomeStatement()), of(report.expenseReport()), of(report.occupancyReport()));
    }

    private String rangeLabel(LocalDate start, LocalDate end) {
        if (start == null && end == null) return "All time";
        String from = start == null ? "start" : DATE_FORMAT.format(start);
        String to = end == null ? "now" : DATE_FORMAT.format(end);
        return from + " to " + to;
    }

    /** "Tk" not "৳" - standard PDF/Excel base fonts don't reliably cover the Bengali Taka glyph. */
    private String taka(double amount) {
        return String.format("Tk %,.2f", amount);
    }
}
