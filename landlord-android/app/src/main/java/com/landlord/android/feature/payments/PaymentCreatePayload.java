package com.landlord.android.feature.payments;

/** Outbox payload for a queued payment record - carries parent Invoice/Tenant
 *  LOCAL ids, resolved to server ids only when pushed. */
public class PaymentCreatePayload {
    public String invoiceLocalId;
    public String tenantLocalId;
    public double amount;
    public String method;
}
