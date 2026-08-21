package com.landlord.backend.maintenance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByPropertyId(Long propertyId);

    List<Expense> findByTenantId(Long tenantId);
}
