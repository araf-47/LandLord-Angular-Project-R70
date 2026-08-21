package com.barivara.backend.booking;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {
    List<BookingRequest> findByListingId(Long listingId);

    List<BookingRequest> findByTenantId(Long tenantId);

    List<BookingRequest> findByListingIdIn(List<Long> listingIds);
}
