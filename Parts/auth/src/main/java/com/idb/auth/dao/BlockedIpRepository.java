package com.idb.auth.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.idb.auth.model.BlockedIp;

@Repository
public interface BlockedIpRepository extends JpaRepository<BlockedIp, Long> {

    Optional<BlockedIp> findByIpAddress(String ipAddress);

    List<BlockedIp> findByUsername(String username);

    @Query("""
            SELECT b FROM BlockedIp b
            WHERE (:ipAddress IS NULL OR b.ipAddress LIKE %:ipAddress%)
              AND (:username IS NULL OR b.username LIKE %:username%)
              AND (:active IS NULL OR b.active = :active)
            """)
    Page<BlockedIp> search(Pageable pageable,
            @Param("ipAddress") String ipAddress,
            @Param("username") String username,
            @Param("active") Boolean active);
}
