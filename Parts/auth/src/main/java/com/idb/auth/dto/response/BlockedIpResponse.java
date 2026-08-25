package com.idb.auth.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedIpResponse {
    private Long id;
    private String ipAddress;
    private LocalDateTime blockedAt;
    private LocalDateTime unblockAt;
    private String endpoint;
    private String username;
    private String reason;
    private Boolean active;
    private Integer failedAttempts;
    private Integer failedLoginAttempts;
    private Integer failedUnauthenticatedAttempts;
    private String lastFailureType;
    private LocalDateTime lastAttemptAt;
}
