package com.landlord.android.feature.payments;

/** Mirrors landlord-backend's Invoice @Entity - returned by GET /api/invoices
 *  and POST /api/invoices/generate. */
public class InvoiceDto {
    public Long id;
    public Long tenantId;
    public Long unitId;
    public String period;
    public Double rent;
    public Double utilitiesTotal;
    public Double prevUnpaidRolled;
    public Double amount;
    public Double balance;
    public String status;
    public String dueDate;
}
