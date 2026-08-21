package com.landlord.backend.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalAgreementRepository extends JpaRepository<RentalAgreement, Long> {
    Optional<RentalAgreement> findFirstByTenantIdOrderByIdDesc(Long tenantId);
}
