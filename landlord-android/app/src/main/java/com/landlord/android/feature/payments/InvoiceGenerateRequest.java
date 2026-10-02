package com.landlord.android.feature.payments;

/** Wire shape for POST /api/invoices/generate - BillingController.GenerateInvoiceRequest. */
public class InvoiceGenerateRequest {
    public Long tenantId;
    public Double utilitiesTotal;
}
