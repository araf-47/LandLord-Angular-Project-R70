package com.landlord.backend.marketplace;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceRequestRepository extends JpaRepository<MarketplaceRequest, Long> {
    List<MarketplaceRequest> findByUnitId(Long unitId);

    List<MarketplaceRequest> findByStatus(String status);
}
