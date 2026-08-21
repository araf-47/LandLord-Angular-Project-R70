package com.landlord.backend.sync;

import com.landlord.backend.property.Property;
import com.landlord.backend.unit.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Phase 15 outbound half: tells BariVara's backend about vacancy/status changes on
 * this app's units. Best-effort only — BariVara being down or unreachable must never
 * break a landlord's own request (move-out, unit edit, marketplace approval), so
 * every call is caught and logged, never rethrown.
 */
@Service
public class BariVaraSyncService {

    private static final Logger log = LoggerFactory.getLogger(BariVaraSyncService.class);

    private final RestClient restClient;

    public BariVaraSyncService(@Value("${barivara.backend.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public record VacancyAdSyncRequest(
        Long unitId, String propertyName, String address, String district,
        String area, String propertyType, Double rent, String photoUrl
    ) {}

    public record UnitStatusSyncRequest(Long unitId, String status, boolean adPaused) {}

    /**
     * c3040 "Auto-post ad to BariVara.com". Skipped (not an error) if the property is
     * missing district/area/propertyType — those didn't exist on Property before
     * Phase 15, so older properties silently don't get ads until edited.
     */
    public void postVacancyAd(Unit unit, Property property) {
        if (property.getDistrict() == null || property.getArea() == null || property.getPropertyType() == null) {
            log.info("Skipping BariVara auto-post for unit {} — property {} missing district/area/propertyType",
                unit.getId(), property.getId());
            return;
        }
        if (unit.getRent() == null) {
            log.info("Skipping BariVara auto-post for unit {} — no rent set", unit.getId());
            return;
        }

        var request = new VacancyAdSyncRequest(
            unit.getId(), property.getName(), property.getAddress(), property.getDistrict(),
            property.getArea(), property.getPropertyType(), unit.getRent(), unit.getPhotoUrl()
        );

        try {
            restClient.post()
                .uri("/api/listings/sync/vacancy-ad")
                .body(request)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to sync vacancy ad for unit {} to BariVara: {}", unit.getId(), e.getMessage());
        }
    }

    /** e5125 "Approve, mark unit filled, take down ad" — also used for manual
     *  ad-pause/ad-repost, same underlying signal. No-ops harmlessly on BariVara's
     *  side if this unit was never synced there (no landlord-linked listing found). */
    public void pushUnitStatus(Long unitId, String status, boolean adPaused) {
        try {
            restClient.put()
                .uri("/api/listings/sync/unit-status")
                .body(new UnitStatusSyncRequest(unitId, status, adPaused))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to sync unit status for unit {} to BariVara: {}", unitId, e.getMessage());
        }
    }
}
