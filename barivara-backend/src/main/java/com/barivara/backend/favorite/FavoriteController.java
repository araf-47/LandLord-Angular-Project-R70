package com.barivara.backend.favorite;

import com.barivara.backend.profile.TenantProfileRepository;
import com.idb.auth.model.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class FavoriteController {

    private final FavoriteRepository repository;
    private final TenantProfileRepository tenantProfiles;

    public FavoriteController(FavoriteRepository repository, TenantProfileRepository tenantProfiles) {
        this.repository = repository;
        this.tenantProfiles = tenantProfiles;
    }

    /** A tenant's own linked profile id always wins over the client-supplied
     *  tenantId - permissions.json only checks role-vs-URL, not row ownership. */
    private Long ownTenantId(User principal) {
        return tenantProfiles.findFirstByAuthUserId(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tenant profile linked to this account"))
            .getId();
    }

    @GetMapping
    public List<Favorite> list(@AuthenticationPrincipal User principal, @RequestParam Long tenantId) {
        return repository.findByTenantId(ownTenantId(principal));
    }

    public record NewFavorite(Long tenantId, Long listingId) {}

    @PostMapping
    public ResponseEntity<Favorite> add(@AuthenticationPrincipal User principal, @RequestBody NewFavorite body) {
        Long tenantId = ownTenantId(principal);
        return repository.findByTenantIdAndListingId(tenantId, body.listingId())
                .map(existing -> ResponseEntity.status(HttpStatus.OK).body(existing))
                .orElseGet(() -> {
                    Favorite favorite = new Favorite();
                    favorite.setTenantId(tenantId);
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
