package com.barivara.backend.profile;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, Long> {
    Optional<OwnerProfile> findFirstByAuthUserId(Long authUserId);
}
