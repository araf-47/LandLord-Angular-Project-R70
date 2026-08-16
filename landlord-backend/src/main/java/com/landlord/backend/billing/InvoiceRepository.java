package com.landlord.backend.billing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByTenantId(Long tenantId);

    List<Invoice> findByTenantIdAndStatusNot(Long tenantId, String status);

    Optional<Invoice> findFirstByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<Invoice> findByPeriod(String period);
}
