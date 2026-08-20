package com.barivara.backend.favorite;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByTenantId(Long tenantId);

    Optional<Favorite> findByTenantIdAndListingId(Long tenantId, Long listingId);
}
