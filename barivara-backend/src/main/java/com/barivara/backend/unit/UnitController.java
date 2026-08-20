package com.barivara.backend.unit;

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
@RequestMapping("/api/owner-units")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class UnitController {

    private final OwnerUnitRepository repository;

    public UnitController(OwnerUnitRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<OwnerUnit> all(@RequestParam(required = false) Long propertyId) {
        return propertyId != null ? repository.findByPropertyId(propertyId) : repository.findAll();
    }

    @GetMapping("/{id}")
    public OwnerUnit one(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<OwnerUnit> create(@Valid @RequestBody OwnerUnit unit) {
        OwnerUnit saved = repository.save(unit);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public OwnerUnit update(@PathVariable Long id, @Valid @RequestBody OwnerUnit update) {
        OwnerUnit existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existing.setUnitNumber(update.getUnitNumber());
        existing.setRent(update.getRent());
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
