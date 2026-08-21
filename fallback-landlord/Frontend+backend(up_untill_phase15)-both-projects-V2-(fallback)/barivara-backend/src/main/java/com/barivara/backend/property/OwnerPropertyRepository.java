package com.barivara.backend.property;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerPropertyRepository extends JpaRepository<OwnerProperty, Long> {
    List<OwnerProperty> findByOwnerId(Long ownerId);
}
