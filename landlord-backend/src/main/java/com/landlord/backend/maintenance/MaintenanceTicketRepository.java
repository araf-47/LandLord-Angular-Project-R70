package com.landlord.backend.maintenance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {
    List<MaintenanceTicket> findByTenantId(Long tenantId);

    List<MaintenanceTicket> findByUnitId(Long unitId);
}
