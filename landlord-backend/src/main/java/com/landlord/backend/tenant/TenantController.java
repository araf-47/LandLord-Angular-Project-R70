package com.landlord.backend.tenant;

import com.landlord.backend.billing.Invoice;
import com.landlord.backend.billing.InvoiceRepository;
import com.landlord.backend.unit.Unit;
import com.landlord.backend.unit.UnitRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
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
@RequestMapping("/api/tenants")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class TenantController {

    private final TenantRepository tenants;
    private final RentalAgreementRepository agreements;
    private final UnitRepository units;
    private final InvoiceRepository invoices;

    public TenantController(TenantRepository tenants, RentalAgreementRepository agreements, UnitRepository units, InvoiceRepository invoices) {
        this.tenants = tenants;
        this.agreements = agreements;
        this.units = units;
        this.invoices = invoices;
    }

    @GetMapping
    public List<Tenant> all() {
        return tenants.findAll();
    }

    @GetMapping("/{id}")
    public Tenant one(@PathVariable Long id) {
        return tenants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
        Long unitId, String terms, Double deposit
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
        Tenant savedTenant = tenants.save(tenant);

        RentalAgreement agreement = new RentalAgreement();
        agreement.setTenantId(savedTenant.getId());
        agreement.setUnitId(request.unitId());
        agreement.setTerms(request.terms() == null || request.terms().isBlank() ? "Standard lease" : request.terms());
        agreement.setDeposit(request.deposit() == null ? 0 : request.deposit());
        agreements.save(agreement);

        Unit unit = units.findById(request.unitId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));
        unit.setStatus("occupied");
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
                units.save(unit);
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
