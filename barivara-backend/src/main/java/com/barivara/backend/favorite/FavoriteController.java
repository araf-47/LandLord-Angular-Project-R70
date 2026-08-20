package com.barivara.backend.favorite;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class FavoriteController {

    private final FavoriteRepository repository;

    public FavoriteController(FavoriteRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Favorite> list(@RequestParam Long tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public record NewFavorite(Long tenantId, Long listingId) {}

    @PostMapping
    public ResponseEntity<Favorite> add(@RequestBody NewFavorite body) {
        return repository.findByTenantIdAndListingId(body.tenantId(), body.listingId())
                .map(existing -> ResponseEntity.status(HttpStatus.OK).body(existing))
                .orElseGet(() -> {
                    Favorite favorite = new Favorite();
                    favorite.setTenantId(body.tenantId());
                    favorite.setListingId(body.listingId());
                    return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(favorite));
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
