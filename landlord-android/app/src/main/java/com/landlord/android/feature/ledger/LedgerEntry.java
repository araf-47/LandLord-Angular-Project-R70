package com.landlord.android.feature.ledger;

/** Pure client-side composition over already-synced Payments/Invoices/Expenses
 *  - there is no backend ledger endpoint (matches the Angular ledger.component.ts
 *  pattern: composed in the client, not a server resource). */
public class LedgerEntry {
    public long date;
    public String type; // "Payment", "Invoice", "Expense"
    public String description;
    public double amount; // positive = inflow (payment), negative = outflow (expense)
    public String syncBadge; // null if synced

    public LedgerEntry(long date, String type, String description, double amount, String syncBadge) {
        this.date = date;
        this.type = type;
        this.description = description;
        this.amount = amount;
        this.syncBadge = syncBadge;
    }
}
