package com.barivara.backend.property;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/owner-properties")
public class PropertyController {

    private final OwnerPropertyRepository repository;

    public PropertyController(OwnerPropertyRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<OwnerProperty> all(@RequestParam(required = false) Long ownerId) {
        return ownerId != null ? repository.findByOwnerId(ownerId) : repository.findAll();
    }

    @GetMapping("/{id}")
    public OwnerProperty one(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<OwnerProperty> create(@Valid @RequestBody OwnerProperty property) {
        OwnerProperty saved = repository.save(property);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public OwnerProperty update(@PathVariable Long id, @Valid @RequestBody OwnerProperty update) {
        OwnerProperty existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existing.setName(update.getName());
        existing.setAddress(update.getAddress());
        existing.setDistrict(update.getDistrict());
        existing.setArea(update.getArea());
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
