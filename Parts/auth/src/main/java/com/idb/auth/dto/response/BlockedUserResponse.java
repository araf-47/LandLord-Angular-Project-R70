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
public class BlockedUserResponse {
    private Long id;
    private String username;
    private String email;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastFailedLoginAt;
}
