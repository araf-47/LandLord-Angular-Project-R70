package com.landlord.backend.unit;

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
@RequestMapping("/api/units")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class UnitController {

    private final UnitRepository repository;

    public UnitController(UnitRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Unit> all(@RequestParam(required = false) Long propertyId) {
        return propertyId == null ? repository.findAll() : repository.findByPropertyId(propertyId);
    }

    @GetMapping("/{id}")
    public Unit one(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Unit> create(@Valid @RequestBody Unit unit) {
        Unit saved = repository.save(unit);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Unit update(@PathVariable Long id, @Valid @RequestBody Unit update) {
        Unit existing = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean turningVacant = !"vacant".equals(existing.getStatus()) && "vacant".equals(update.getStatus());
        existing.setUnitNumber(update.getUnitNumber());
        existing.setRent(update.getRent());
        existing.setStatus(update.getStatus());
        existing.setPropertyId(update.getPropertyId());
        if (turningVacant) {
            existing.setAdPaused(false);
        }
        return repository.save(existing);
    }

    @PutMapping("/{id}/ad-pause")
    public Unit pauseAd(@PathVariable Long id) {
        Unit unit = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        unit.setAdPaused(true);
        return repository.save(unit);
    }

    @PutMapping("/{id}/ad-repost")
    public Unit repostAd(@PathVariable Long id) {
        Unit unit = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        unit.setAdPaused(false);
        return repository.save(unit);
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
