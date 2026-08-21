package com.landlord.backend.unit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findByPropertyId(Long propertyId);

    List<Unit> findByStatusAndAdPausedFalse(String status);
}
