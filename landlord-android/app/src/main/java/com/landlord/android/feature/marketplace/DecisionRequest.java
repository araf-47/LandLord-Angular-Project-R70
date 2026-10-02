package com.landlord.android.feature.marketplace;

/** Wire shape for PUT /api/marketplace-requests/{id}/status. */
public class DecisionRequest {
    public String status;

    public DecisionRequest(String status) {
        this.status = status;
    }
}
