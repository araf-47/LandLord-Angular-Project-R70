package com.landlord.android.feature.payments;

/** One row of the current month's bills table - mirrors the web
 *  generate-bills.component.ts table (Tenant/Rent/Utilities/Rolled over/Total due/Status),
 *  joining an InvoiceEntity for the current period to its tenant's name. */
public class BillRow {
    public final String invoiceLocalId;
    public final String tenantName;
    public final double rent;
    public final double utilitiesTotal;
    public final double prevUnpaidRolled;
    public final double balance;
    public final double amount;
    public final String status; // "unpaid" | "partial" | "paid"

    public BillRow(String invoiceLocalId, String tenantName, double rent, double utilitiesTotal,
                    double prevUnpaidRolled, double balance, double amount, String status) {
        this.invoiceLocalId = invoiceLocalId;
        this.tenantName = tenantName;
        this.rent = rent;
        this.utilitiesTotal = utilitiesTotal;
        this.prevUnpaidRolled = prevUnpaidRolled;
        this.balance = balance;
        this.amount = amount;
        this.status = status;
    }
}
