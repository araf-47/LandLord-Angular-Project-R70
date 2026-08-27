package com.landlord.backend.tenant;

import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.dto.request.UserRegistrationRequest;
import com.idb.auth.model.User;
import com.idb.auth.service.UserService;
import com.landlord.backend.billing.Invoice;
import com.landlord.backend.billing.InvoiceRepository;
import com.landlord.backend.property.PropertyRepository;
import com.landlord.backend.sync.BariVaraSyncService;
import com.landlord.backend.unit.Unit;
import com.landlord.backend.unit.UnitRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantRepository tenants;
    private final RentalAgreementRepository agreements;
    private final UnitRepository units;
    private final InvoiceRepository invoices;
    private final PropertyRepository properties;
    private final BariVaraSyncService syncService;
    private final UserService userService;

    public TenantController(TenantRepository tenants, RentalAgreementRepository agreements, UnitRepository units,
            InvoiceRepository invoices, PropertyRepository properties, BariVaraSyncService syncService,
            UserService userService) {
        this.tenants = tenants;
        this.agreements = agreements;
        this.units = units;
        this.invoices = invoices;
        this.properties = properties;
        this.syncService = syncService;
        this.userService = userService;
    }

    @GetMapping
    public List<Tenant> all() {
        return tenants.findAll();
    }

    @GetMapping("/{id}")
    public Tenant one(@PathVariable Long id) {
        return tenants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/me")
    public Tenant me(@AuthenticationPrincipal User principal) {
        return tenants.findFirstByAuthUserId(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tenant record linked to this account"));
    }

    @GetMapping("/me/agreement")
    public ResponseEntity<RentalAgreement> myAgreement(@AuthenticationPrincipal User principal) {
        Tenant me = tenants.findFirstByAuthUserId(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tenant record linked to this account"));
        Optional<RentalAgreement> found = agreements.findFirstByTenantIdOrderByIdDesc(me.getId());
        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/active-by-nid")
    public ResponseEntity<Tenant> activeByNationalId(@RequestParam String nationalId) {
        Optional<Tenant> found = tenants.findFirstByNationalIdAndStatus(nationalId.trim(), "active");
        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{tenantId}/agreement")
    public ResponseEntity<RentalAgreement> agreementForTenant(@PathVariable Long tenantId) {
        Optional<RentalAgreement> found = agreements.findFirstByTenantIdOrderByIdDesc(tenantId);
        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record RegisterRequest(
        String name, String phone, String email, String nationalId,
        Long unitId, String terms, Double deposit, String password
    ) {}

    @PostMapping("/register")
    public ResponseEntity<Tenant> register(@RequestBody RegisterRequest request) {
        if (tenants.findFirstByNationalIdAndStatus(request.nationalId().trim(), "active").isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "National ID already registered to an active tenant");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setPhone(request.phone());
        tenant.setEmail(request.email());
        tenant.setNationalId(request.nationalId());
        tenant.setUnitId(request.unitId());
        tenant.setStatus("active");

        // Real login credentials for the tenant, handed to them in person by the
        // landlord at registration time (per your call: no separate self-service
        // signup step for LandLord-side tenants). Username is the phone number.
        if (request.password() != null && !request.password().isBlank()) {
            UserRegistrationRequest userRequest = new UserRegistrationRequest();
            userRequest.setUsername(request.phone().replaceAll("[^a-zA-Z0-9]", ""));
            userRequest.setPassword(request.password());
            userRequest.setRoles(List.of("TENANT"));
            userRequest.setEmail(request.email());
            userRequest.setPhone(request.phone());
            try {
                User createdUser = userService.registerUser(userRequest);
                tenant.setAuthUserId(createdUser.getId());
            } catch (LogOnlyException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, e.getResponse().getMessage());
            }
        }

        Tenant savedTenant = tenants.save(tenant);

        RentalAgreement agreement = new RentalAgreement();
        agreement.setTenantId(savedTenant.getId());
        agreement.setUnitId(request.unitId());
        agreement.setTerms(request.terms() == null || request.terms().isBlank() ? "Standard lease" : request.terms());
        agreement.setDeposit(request.deposit() == null ? 0 : request.deposit());
        agreements.save(agreement);

        Unit unit = units.findById(request.unitId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));
        unit.setStatus("occupied");
        unit.setVacantSince(null);
        unit.setAdReminderSentAt(null);
        units.save(unit);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedTenant);
    }

    @PutMapping("/{id}")
    public Tenant update(@PathVariable Long id, @Valid @RequestBody Tenant update) {
        Tenant existing = tenants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existing.setName(update.getName());
        existing.setPhone(update.getPhone());
        existing.setEmail(update.getEmail());
        existing.setNationalId(update.getNationalId());
        existing.setUnitId(update.getUnitId());
        existing.setStatus(update.getStatus());
        return tenants.save(existing);
    }

    public record UpdateAgreementRequest(String terms) {}

    @PutMapping("/{tenantId}/agreement")
    public RentalAgreement updateAgreement(@PathVariable Long tenantId, @RequestBody UpdateAgreementRequest request) {
        RentalAgreement agreement = agreements.findFirstByTenantIdOrderByIdDesc(tenantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No agreement for tenant"));
        agreement.setTerms(request.terms());
        return agreements.save(agreement);
    }

    public record MoveOutResult(double outstandingBalance) {}

    @PostMapping("/{id}/move-out")
    public MoveOutResult moveOut(@PathVariable Long id) {
        Tenant tenant = tenants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        double outstandingBalance = invoices.findByTenantIdAndStatusNot(id, "paid").stream()
            .mapToDouble(Invoice::getBalance)
            .sum();

        if (tenant.getUnitId() != null) {
            units.findById(tenant.getUnitId()).ifPresent(unit -> {
                unit.setStatus("vacant");
                unit.setVacantSince(Instant.now());
                unit.setAdReminderSentAt(null);
                Unit saved = units.save(unit);
                properties.findById(saved.getPropertyId()).ifPresent(property -> syncService.postVacancyAd(saved, property));
            });
        }

        tenant.setStatus("inactive");
        tenant.setUnitId(null);
        tenants.save(tenant);

        return new MoveOutResult(outstandingBalance);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!tenants.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        tenants.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
