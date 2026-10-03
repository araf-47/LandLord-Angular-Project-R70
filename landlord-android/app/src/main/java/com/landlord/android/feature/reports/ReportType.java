package com.landlord.android.feature.reports;

public enum ReportType {
    INCOME_STATEMENT("Income statement", true, true, false, false, true),
    EXPENSE_REPORT("Expense report", true, true, false, true, true),
    OCCUPANCY_REPORT("Occupancy report", false, true, false, false, true),
    TENANT_LEDGER("Tenant ledger", true, false, true, false, true),
    FULL_REPORT("Full report (combined)", true, true, false, false, false),
    /** Computed entirely on-device from already-synced Invoice/Tenant tables -
     *  no backend endpoint exists for this one, so PDF/XLSX export isn't offered. */
    TENANT_DUES("Tenant dues", false, false, false, false, true);

    public final String label;
    public final boolean needsDateRange;
    public final boolean needsProperty;
    public final boolean needsTenant;
    public final boolean needsCategory;
    public final boolean hasJsonView;

    ReportType(String label, boolean needsDateRange, boolean needsProperty,
               boolean needsTenant, boolean needsCategory, boolean hasJsonView) {
        this.label = label;
        this.needsDateRange = needsDateRange;
        this.needsProperty = needsProperty;
        this.needsTenant = needsTenant;
        this.needsCategory = needsCategory;
        this.hasJsonView = hasJsonView;
    }
}
