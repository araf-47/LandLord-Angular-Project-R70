package com.landlord.backend.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findFirstByNationalIdAndStatus(String nationalId, String status);
}
