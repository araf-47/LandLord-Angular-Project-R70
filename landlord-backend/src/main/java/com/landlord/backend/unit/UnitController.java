package com.landlord.backend.unit;

import com.landlord.backend.property.Property;
import com.landlord.backend.property.PropertyRepository;
import com.landlord.backend.sync.BariVaraSyncService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/units")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class UnitController {

    private final UnitRepository repository;
    private final PropertyRepository properties;
    private final BariVaraSyncService syncService;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    public UnitController(UnitRepository repository, PropertyRepository properties, BariVaraSyncService syncService) {
        this.repository = repository;
        this.properties = properties;
        this.syncService = syncService;
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
        if ("vacant".equals(saved.getStatus())) {
            saved.setVacantSince(Instant.now());
            saved = repository.save(saved);
            Unit toSync = saved;
            properties.findById(saved.getPropertyId()).ifPresent(property -> syncService.postVacancyAd(toSync, property));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Unit update(@PathVariable Long id, @Valid @RequestBody Unit update) {
        Unit existing = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean turningVacant = !"vacant".equals(existing.getStatus()) && "vacant".equals(update.getStatus());
        boolean turningOccupied = !"vacant".equals(update.getStatus()) && "vacant".equals(existing.getStatus());
        existing.setUnitNumber(update.getUnitNumber());
        existing.setRent(update.getRent());
        existing.setStatus(update.getStatus());
        existing.setPropertyId(update.getPropertyId());
        if (turningVacant) {
            existing.setAdPaused(false);
            existing.setVacantSince(Instant.now());
            existing.setAdReminderSentAt(null);
        } else if (turningOccupied) {
            existing.setVacantSince(null);
            existing.setAdReminderSentAt(null);
        }
        Unit saved = repository.save(existing);
        if (turningVacant) {
            properties.findById(saved.getPropertyId()).ifPresent(property -> syncService.postVacancyAd(saved, property));
        }
        return saved;
    }

    @PutMapping("/{id}/ad-pause")
    public Unit pauseAd(@PathVariable Long id) {
        Unit unit = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        unit.setAdPaused(true);
        Unit saved = repository.save(unit);
        syncService.pushUnitStatus(saved.getId(), saved.getStatus(), true);
        return saved;
    }

    @PutMapping("/{id}/ad-repost")
    public Unit repostAd(@PathVariable Long id) {
        Unit unit = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        unit.setAdPaused(false);
        Unit saved = repository.save(unit);
        syncService.pushUnitStatus(saved.getId(), saved.getStatus(), false);
        return saved;
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<Unit> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Unit unit = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file uploaded");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + extension;

        try {
            Path targetDir = Path.of(uploadsDir, "units", String.valueOf(id));
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store photo", e);
        }

        unit.setPhotoUrl("/uploads/units/" + id + "/" + filename);
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
