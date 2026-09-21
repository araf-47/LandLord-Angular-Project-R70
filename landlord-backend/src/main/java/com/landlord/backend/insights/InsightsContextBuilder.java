package com.landlord.backend.insights;

import com.landlord.backend.billing.Invoice;
import com.landlord.backend.billing.InvoiceRepository;
import com.landlord.backend.maintenance.MaintenanceTicket;
import com.landlord.backend.maintenance.MaintenanceTicketRepository;
import com.landlord.backend.report.ExpenseReport;
import com.landlord.backend.report.ExpenseReportRow;
import com.landlord.backend.report.IncomeStatementReport;
import com.landlord.backend.report.OccupancyReport;
import com.landlord.backend.report.ReportService;
import com.landlord.backend.tenant.Tenant;
import com.landlord.backend.tenant.TenantRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds a small, pre-aggregated, pre-filtered text summary of the
 * landlord's portfolio to ground the AI chat's answers - never passes raw
 * entity lists to the LLM. This is what keeps the prompt bounded (a few KB,
 * not a database dump) and is the mechanism that scopes what the model can
 * see: only numbers already computed server-side land in the prompt.
 */
@Component
public class InsightsContextBuilder {

    private static final int MAX_OVERDUE_TENANTS = 20;

    private final ReportService reportService;
    private final InvoiceRepository invoices;
    private final TenantRepository tenants;
    private final MaintenanceTicketRepository tickets;

    public InsightsContextBuilder(ReportService reportService, InvoiceRepository invoices, TenantRepository tenants,
            MaintenanceTicketRepository tickets) {
        this.reportService = reportService;
        this.invoices = invoices;
        this.tenants = tenants;
        this.tickets = tickets;
    }

    public String build() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        OccupancyReport occupancy = reportService.occupancyReport(null);
        IncomeStatementReport income = reportService.incomeStatement(monthStart, today, null);
        ExpenseReport expense = reportService.expenseReport(monthStart, today, null, null);

        StringBuilder sb = new StringBuilder();
        sb.append("Portfolio snapshot as of ").append(today).append(":\n\n");

        sb.append("Occupancy: ").append(occupancy.totalUnits()).append(" units total, ")
            .append(occupancy.totalOccupied()).append(" occupied, ")
            .append(occupancy.totalUnits() - occupancy.totalOccupied()).append(" vacant (")
            .append(occupancy.occupancyRatePercent()).append("% occupancy rate).\n\n");

        sb.append("This month (").append(monthStart).append(" to ").append(today).append("): billed Tk")
            .append(income.totalBilled()).append(", collected Tk").append(income.totalCollected())
            .append(", outstanding Tk").append(income.totalOutstanding()).append(" (")
            .append(income.collectionRatePercent()).append("% collection rate).\n\n");

        sb.append("This month's expenses by category:\n");
        if (expense.rows().isEmpty()) {
            sb.append("  (none recorded)\n");
        }
        for (ExpenseReportRow row : expense.rows()) {
            sb.append("  - ").append(row.category()).append(": Tk").append(row.total())
                .append(" (").append(row.count()).append(" entries)\n");
        }
        sb.append("\n");

        appendOverdueTenants(sb);
        appendMaintenanceSummary(sb);

        return sb.toString();
    }

    private void appendOverdueTenants(StringBuilder sb) {
        List<Invoice> overdue = invoices.findAll().stream()
            .filter(inv -> !"paid".equals(inv.getStatus()) && inv.getBalance() != null && inv.getBalance() > 0)
            .sorted(Comparator.comparingDouble(Invoice::getBalance).reversed())
            .limit(MAX_OVERDUE_TENANTS)
            .toList();

        sb.append("Overdue tenants (top ").append(MAX_OVERDUE_TENANTS)
            .append(" by balance, ").append(overdue.size()).append(" shown):\n");
        if (overdue.isEmpty()) {
            sb.append("  (none - all invoices paid)\n\n");
            return;
        }

        Map<Long, Tenant> tenantsById = tenants.findAll().stream()
            .collect(Collectors.toMap(Tenant::getId, t -> t));

        for (Invoice inv : overdue) {
            Tenant tenant = tenantsById.get(inv.getTenantId());
            String name = tenant == null ? "Unknown tenant" : tenant.getName();
            sb.append("  - ").append(name).append(": Tk").append(inv.getBalance())
                .append(" overdue, period ").append(inv.getPeriod())
                .append(", due ").append(inv.getDueDate())
                .append(", status ").append(inv.getStatus()).append("\n");
        }
        sb.append("\n");
    }

    private void appendMaintenanceSummary(StringBuilder sb) {
        List<MaintenanceTicket> all = tickets.findAll();
        long pending = all.stream().filter(t -> "pending".equals(t.getStatus())).count();
        long inProgress = all.stream().filter(t -> "in-progress".equals(t.getStatus())).count();
        long resolved = all.stream().filter(t -> "resolved".equals(t.getStatus())).count();

        sb.append("Maintenance tickets: ").append(pending).append(" pending, ")
            .append(inProgress).append(" in progress, ").append(resolved).append(" resolved.\n");
    }
}
