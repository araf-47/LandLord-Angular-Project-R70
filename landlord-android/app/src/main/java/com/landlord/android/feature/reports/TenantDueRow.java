package com.landlord.android.feature.reports;

/** One row of the on-device "Tenant dues" report - every active tenant who
 *  currently owes something, with their total outstanding balance across all
 *  non-paid invoices (same aggregation as the Payments tab's Monthly Bills
 *  "Total due", just across every tenant instead of the current month only). */
public class TenantDueRow {
    public final String tenant;
    public final double totalDue;
    public final String status; // "unpaid" or "partial"

    public TenantDueRow(String tenant, double totalDue, String status) {
        this.tenant = tenant;
        this.totalDue = totalDue;
        this.status = status;
    }
}
