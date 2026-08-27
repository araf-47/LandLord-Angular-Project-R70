package com.landlord.backend.marketplace;

import com.landlord.backend.sync.BariVaraSyncService;
import com.landlord.backend.unit.Unit;
import com.landlord.backend.unit.UnitRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/marketplace-requests")
public class MarketplaceController {

    private final MarketplaceRequestRepository requests;
    private final UnitRepository units;
    private final BariVaraSyncService syncService;

    public MarketplaceController(MarketplaceRequestRepository requests, UnitRepository units, BariVaraSyncService syncService) {
        this.requests = requests;
        this.units = units;
        this.syncService = syncService;
    }

    @GetMapping
    public List<MarketplaceRequest> list(@RequestParam(required = false) Long unitId, @RequestParam(required = false) String status) {
        if (unitId != null) return requests.findByUnitId(unitId);
        if (status != null) return requests.findByStatus(status);
        return requests.findAll();
    }

    @GetMapping("/{id}")
    public MarketplaceRequest get(@PathVariable Long id) {
        return requests.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    public record NewRequestRequest(Long unitId, String applicantName, Long tenantId) {}

    @PostMapping
    public ResponseEntity<MarketplaceRequest> create(@RequestBody NewRequestRequest request) {
        MarketplaceRequest marketplaceRequest = new MarketplaceRequest();
        marketplaceRequest.setUnitId(request.unitId());
        marketplaceRequest.setApplicantName(request.applicantName());
        marketplaceRequest.setTenantId(request.tenantId());
        marketplaceRequest.setStatus("pending");
        return ResponseEntity.status(HttpStatus.CREATED).body(requests.save(marketplaceRequest));
    }

    /**
     * e5045 "Sync to LandLord core Marketplace & Leads" — BariVara calls this when
     * someone requests a landlord-linked listing. `barivaraTenantId` is BariVara's
     * own tenant id, not a real LandLord tenant (this app has no record of them
     * unless/until approved), so `tenantId` stays null on the created request.
     */
    public record FromBariVaraRequest(Long unitId, String applicantName, Long barivaraTenantId, String message) {}

    @PostMapping("/from-barivara")
    public ResponseEntity<MarketplaceRequest> createFromBariVara(@RequestBody FromBariVaraRequest request) {
        MarketplaceRequest marketplaceRequest = new MarketplaceRequest();
        marketplaceRequest.setUnitId(request.unitId());
        marketplaceRequest.setApplicantName(request.applicantName());
        marketplaceRequest.setBarivaraTenantId(request.barivaraTenantId());
        marketplaceRequest.setMessage(request.message());
        marketplaceRequest.setStatus("pending");
        return ResponseEntity.status(HttpStatus.CREATED).body(requests.save(marketplaceRequest));
    }

    public record DecisionRequest(String status) {}

    @PutMapping("/{id}/status")
    public MarketplaceRequest decide(@PathVariable Long id, @RequestBody DecisionRequest decision) {
        MarketplaceRequest marketplaceRequest = requests.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        marketplaceRequest.setStatus(decision.status());
        requests.save(marketplaceRequest);

        if ("approved".equals(decision.status()) && marketplaceRequest.getUnitId() != null) {
            Unit unit = units.findById(marketplaceRequest.getUnitId()).orElse(null);
            if (unit != null) {
                unit.setStatus("occupied");
                unit.setVacantSince(null);
                unit.setAdReminderSentAt(null);
                units.save(unit);
                // e5125 "Approve, mark unit filled, take down ad" — no-ops on BariVara's
                // side if this unit was never synced there (owner-posted / not vacant-synced).
                syncService.pushUnitStatus(unit.getId(), unit.getStatus(), unit.isAdPaused());
            }
        }

        return marketplaceRequest;
    }
}
