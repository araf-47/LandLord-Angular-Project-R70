package com.barivara.backend.profile;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantProfileRepository extends JpaRepository<TenantProfile, Long> {
    Optional<TenantProfile> findFirstByAuthUserId(Long authUserId);
}
