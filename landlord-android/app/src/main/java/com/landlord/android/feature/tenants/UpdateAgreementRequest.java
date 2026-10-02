package com.landlord.android.feature.tenants;

/** Wire shape for PUT /api/tenants/{tenantId}/agreement - only terms is editable. */
public class UpdateAgreementRequest {
    public String terms;

    public UpdateAgreementRequest(String terms) {
        this.terms = terms;
    }
}
