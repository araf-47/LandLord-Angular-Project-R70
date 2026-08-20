package com.barivara.backend.listing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByOwnerId(Long ownerId);

    List<Listing> findByStatus(String status);
}
