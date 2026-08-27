package com.barivara.backend.unit;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/owner-units")
public class UnitController {

    private final OwnerUnitRepository repository;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

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

    @PostMapping("/{id}/photo")
    public ResponseEntity<OwnerUnit> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        OwnerUnit unit = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file uploaded");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + extension;

        try {
            Path targetDir = Path.of(uploadsDir, "owner-units", String.valueOf(id));
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store photo", e);
        }

        unit.setPhotoUrl("/uploads/owner-units/" + id + "/" + filename);
        return ResponseEntity.ok(repository.save(unit));
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
