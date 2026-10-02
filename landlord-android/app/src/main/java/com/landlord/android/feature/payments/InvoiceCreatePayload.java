package com.landlord.android.feature.payments;

/** Outbox payload for a queued invoice generate - carries the parent
 *  Tenant's LOCAL id, resolved to a server id only when pushed. */
public class InvoiceCreatePayload {
    public String tenantLocalId;
    public double utilitiesTotal;
}
