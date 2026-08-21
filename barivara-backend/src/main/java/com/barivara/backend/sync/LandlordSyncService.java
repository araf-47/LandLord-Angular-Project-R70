package com.barivara.backend.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Phase 15 outbound half: tells the LandLord backend about booking requests made
 * against a landlord-linked listing. Best-effort only — LandLord being down or
 * unreachable must never break a tenant's own booking request here, so every call
 * is caught and logged, never rethrown.
 */
@Service
public class LandlordSyncService {

    private static final Logger log = LoggerFactory.getLogger(LandlordSyncService.class);

    private final RestClient restClient;

    public LandlordSyncService(@Value("${landlord.backend.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public record BookingRequestSync(Long unitId, String applicantName, Long barivaraTenantId, String message) {}

    /** e5045 "Sync to LandLord core Marketplace & Leads". */
    public void pushBookingRequest(Long landlordUnitId, String applicantName, Long tenantId, String message) {
        try {
            restClient.post()
                .uri("/api/marketplace-requests/from-barivara")
                .body(new BookingRequestSync(landlordUnitId, applicantName, tenantId, message))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to sync booking request for landlord unit {} to LandLord: {}", landlordUnitId, e.getMessage());
        }
    }
}
