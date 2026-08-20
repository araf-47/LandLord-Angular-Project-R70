package com.barivara.backend.profile;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * No real auth yet (Phase 7 on hold, same as LandLord). Registration just
 * creates a real profile row; the frontend keeps a hardcoded "current" id
 * the same way LandLord's CURRENT_TENANT_ID_REAL stopgap works.
 */
@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class ProfileController {

    private final TenantProfileRepository tenantRepository;
    private final OwnerProfileRepository ownerRepository;

    public ProfileController(TenantProfileRepository tenantRepository, OwnerProfileRepository ownerRepository) {
        this.tenantRepository = tenantRepository;
        this.ownerRepository = ownerRepository;
    }

    @GetMapping("/api/tenant-profiles/{id}")
    public TenantProfile tenant(@PathVariable Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/api/tenant-profiles")
    public ResponseEntity<TenantProfile> registerTenant(@Valid @RequestBody TenantProfile profile) {
        TenantProfile saved = tenantRepository.save(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/api/owner-profiles/{id}")
    public OwnerProfile owner(@PathVariable Long id) {
        return ownerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/api/owner-profiles")
    public ResponseEntity<OwnerProfile> registerOwner(@Valid @RequestBody OwnerProfile profile) {
        OwnerProfile saved = ownerRepository.save(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
