package com.idb.auth.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.idb.auth.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query(value = "SELECT * FROM roles WHERE name = :name AND is_active = true", nativeQuery = true)
    Optional<Role> findByName(String name);

    @Query(value = "SELECT * FROM roles WHERE name IN (:names) AND is_active = true", nativeQuery = true)
    List<Role> findByNameIn(List<String> names);

    @Query(value = "SELECT name FROM roles WHERE is_active = true", nativeQuery = true)
    List<String> findActiveNames();
}
