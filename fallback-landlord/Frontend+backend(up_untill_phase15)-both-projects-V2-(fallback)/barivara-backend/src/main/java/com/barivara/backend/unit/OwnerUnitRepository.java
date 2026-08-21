package com.barivara.backend.unit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerUnitRepository extends JpaRepository<OwnerUnit, Long> {
    List<OwnerUnit> findByPropertyId(Long propertyId);
}
