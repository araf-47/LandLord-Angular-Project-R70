package com.barivara.backend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantProfileRepository extends JpaRepository<TenantProfile, Long> {
}
