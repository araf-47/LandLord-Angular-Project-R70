package com.idb.auth.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.idb.auth.model.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Query(value = "SELECT * FROM permissions WHERE name IN (:names) AND is_active = true", nativeQuery = true)
    List<Permission> findByNameIn(List<String> names);

    @Query(value = "SELECT * FROM permissions WHERE is_active = true", nativeQuery = true)
    List<Permission> findAllActive();

    @Query(value = """
            SELECT p.*
            FROM permissions p
                JOIN role_permissions rp ON p.id = rp.permission_id
                JOIN roles r ON r.id = rp.role_id
                JOIN user_roles ur ON r.id = ur.role_id
                JOIN users u ON u.id = ur.user_id
            WHERE u.username = :username AND p.is_active = true
            """, nativeQuery = true)
    List<Permission> findByUsername(String username);
}
