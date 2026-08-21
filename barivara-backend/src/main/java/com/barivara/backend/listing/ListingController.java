package com.barivara.backend.listing;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/listings")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class ListingController {

    private final ListingRepository repository;

    public ListingController(ListingRepository repository) {
        this.repository = repository;
    }

    /**
     * Public search/filter (Phase 14.1). Only ever returns "active" listings —
     * paused/taken ads aren't for public browsing. ownerId bypasses the
     * active-only filter so an owner's "manage listings" page can see everything.
     */
    @GetMapping
    public List<Listing> search(
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String propertyType) {
        List<Listing> base = ownerId != null ? repository.findByOwnerId(ownerId) : repository.findByStatus("active");
        return base.stream()
                .filter(l -> district == null || district.equals(l.getDistrict()))
                .filter(l -> area == null || area.equals(l.getArea()))
                .filter(l -> propertyType == null || propertyType.equals(l.getPropertyType()))
                .toList();
    }

    @GetMapping("/{id}")
    public Listing one(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Listing> create(@Valid @RequestBody Listing listing) {
        Listing saved = repository.save(listing);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Listing update(@PathVariable Long id, @Valid @RequestBody Listing update) {
        Listing existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existing.setTitle(update.getTitle());
        existing.setAddress(update.getAddress());
        existing.setDistrict(update.getDistrict());
        existing.setArea(update.getArea());
        existing.setPropertyType(update.getPropertyType());
        existing.setRent(update.getRent());
        existing.setPhotoUrl(update.getPhotoUrl());
        return repository.save(existing);
    }

    /** Phase 15 inbound: c3040 "Auto-post ad to BariVara.com". LandLord calls this
     *  when a unit goes vacant. Idempotent — updates the existing landlord-linked
     *  listing for that unit if one already exists (e.g. a unit that's cycled
     *  vacant → occupied → vacant again) instead of creating a duplicate. */
    public record VacancyAdSyncRequest(
            Long unitId, String propertyName, String address, String district,
            String area, String propertyType, Double rent, String photoUrl) {}

    @PostMapping("/sync/vacancy-ad")
    public ResponseEntity<Listing> syncVacancyAd(@RequestBody VacancyAdSyncRequest body) {
        Listing listing = repository.findByLandlordUnitId(body.unitId()).orElseGet(Listing::new);
        listing.setLandlordUnitId(body.unitId());
        listing.setOwnerId(null);
        listing.setSource("landlord-linked");
        listing.setTitle(body.propertyName());
        listing.setAddress(body.address());
        listing.setDistrict(body.district());
        listing.setArea(body.area());
        listing.setPropertyType(body.propertyType());
        listing.setRent(body.rent());
        listing.setPhotoUrl(body.photoUrl());
        listing.setStatus("active");
        return ResponseEntity.ok(repository.save(listing));
    }

    /** Phase 15 inbound: e5125 "Approve, mark unit filled, take down ad" (also used
     *  for manual ad-pause/repost). No-ops (404, swallowed by the caller) if this
     *  unit was never synced here in the first place. */
    public record UnitStatusSyncRequest(Long unitId, String status, boolean adPaused) {}

    @PutMapping("/sync/unit-status")
    public ResponseEntity<Listing> syncUnitStatus(@RequestBody UnitStatusSyncRequest body) {
        Listing listing = repository.findByLandlordUnitId(body.unitId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if ("occupied".equals(body.status())) {
            listing.setStatus("taken");
        } else {
            listing.setStatus(body.adPaused() ? "paused" : "active");
        }
        return ResponseEntity.ok(repository.save(listing));
    }

    public record StatusUpdate(String status) {}

    @PutMapping("/{id}/status")
    public Listing setStatus(@PathVariable Long id, @RequestBody StatusUpdate body) {
        Listing existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existing.setStatus(body.status());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
