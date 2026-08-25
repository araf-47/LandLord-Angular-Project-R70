package com.idb.auth.model;

import java.time.LocalDateTime;

import com.idb.auth.common.model.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One row per source IP. {@code active} (from {@link BaseModel}) is the "is
 * currently blocked" flag; the counters below accumulate across attempt types.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "blocked_ip", indexes = {
        @Index(name = "idx_blocked_ip_username", columnList = "username")
})
public class BlockedIp extends BaseModel {

    @Column(nullable = false, unique = true)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime blockedAt;

    @Column
    private LocalDateTime unblockAt;

    @Column(nullable = false)
    private String endpoint;

    @Column
    private String username;

    @Column(columnDefinition = "TEXT")
    private String requestBody;

    @Column
    private String reason;

    @Column
    private Integer failedAttempts;

    @Column
    private Integer failedLoginAttempts;

    @Column
    private Integer failedUnauthenticatedAttempts;

    @Column
    private String lastFailureType;

    @Column
    private LocalDateTime lastAttemptAt;
}
