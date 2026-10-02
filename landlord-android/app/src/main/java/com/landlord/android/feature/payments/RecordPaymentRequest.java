package com.landlord.android.feature.payments;

/** Wire shape for POST /api/payments - BillingController.RecordPaymentRequest. */
public class RecordPaymentRequest {
    public Long tenantId;
    public Long invoiceId;
    public Double amount;
    public String method;
}
