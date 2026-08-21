package com.barivara.backend.listing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByOwnerId(Long ownerId);

    List<Listing> findByStatus(String status);

    Optional<Listing> findByLandlordUnitId(Long landlordUnitId);
}
