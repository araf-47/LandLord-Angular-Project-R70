package com.idb.auth.service.impl;

import static com.idb.auth.constant.AuthConstants.IP_BLOCKING_BLOCK_DURATION_HOURS;
import static com.idb.auth.constant.AuthConstants.IP_BLOCKING_ENABLED;
import static com.idb.auth.constant.AuthConstants.IP_BLOCKING_MAX_FAILED_ATTEMPTS;
import static com.idb.auth.constant.AuthConstants.IP_BLOCKING_MAX_INVALID_JWT_ATTEMPTS;
import static com.idb.auth.constant.AuthConstants.IP_BLOCKING_MAX_INVALID_OTP_ATTEMPTS;
import static com.idb.auth.constant.AuthConstants.IP_BLOCKING_MAX_UNAUTHENTICATED_ATTEMPTS;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idb.auth.common.dto.request.ApiPageRequest;
import com.idb.auth.common.dto.response.ApiPageResponse;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.constant.AttemptType;
import com.idb.auth.dao.BlockedIpRepository;
import com.idb.auth.dto.response.BlockedIpResponse;
import com.idb.auth.model.BlockedIp;
import com.idb.auth.service.IpBlockingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = IP_BLOCKING_ENABLED, havingValue = "true")
public class IpBlockingServiceImpl implements IpBlockingService {

    private final BlockedIpRepository blockedIpRepository;
    private final Environment environment;

    @Override
    @Transactional(readOnly = true)
    public ApiPageResponse<BlockedIpResponse> findBlockedIps(ApiPageRequest<BlockedIpResponse> request) {
        BlockedIpResponse filters = request.getFilter();
        Page<BlockedIp> blockedIps = blockedIpRepository.search(
                request.getPageable(),
                filters == null ? null : filters.getIpAddress(),
                filters == null ? null : filters.getUsername(),
                filters == null ? null : filters.getActive());
        return ApiPageResponse.fromPage(blockedIps, this::convertToResponse);
    }

    @Override
    @Transactional
    public boolean isIpBlocked(String ipAddress) {
        return blockedIpRepository.findByIpAddress(ipAddress)
                .map(blockedIp -> {
                    // Lazy expiry: the block window elapsing is what unblocks an IP,
                    // there is no scheduled sweep.
                    if (blockedIp.getUnblockAt() != null && LocalDateTime.now().isAfter(blockedIp.getUnblockAt())) {
                        blockedIp.setActive(false);
                        blockedIpRepository.save(blockedIp);
                        log.info("IP {} automatically unblocked, block duration expired", ipAddress);
                        return false;
                    }
                    return blockedIp.isActive();
                })
                .orElse(false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlockedIp recordFailedAttempt(String ipAddress, String endpoint, String username, String requestBody,
            AttemptType attemptType) throws TraceableException {
        try {
            BlockedIp blockedIp = blockedIpRepository.findByIpAddress(ipAddress)
                    .orElseGet(() -> BlockedIp.builder()
                            .ipAddress(ipAddress)
                            .endpoint(endpoint)
                            .failedAttempts(0)
                            .failedLoginAttempts(0)
                            .failedUnauthenticatedAttempts(0)
                            .blockedAt(LocalDateTime.now())
                            .lastAttemptAt(LocalDateTime.now())
                            .active(false)
                            .build());

            blockedIp.setFailedAttempts(nullSafe(blockedIp.getFailedAttempts()) + 1);
            blockedIp.setLastAttemptAt(LocalDateTime.now());
            blockedIp.setLastFailureType(attemptType.name());
            blockedIp.setUsername(username);
            blockedIp.setRequestBody(requestBody);
            blockedIp.setReason(determineReason(attemptType));

            int blockDurationHours = environment.getProperty(IP_BLOCKING_BLOCK_DURATION_HOURS, Integer.class, 1440);

            // UNAUTHENTICATED has its own counter; the other three share
            // failedLoginAttempts but are gated by their own thresholds.
            String thresholdKey;
            int defaultThreshold;
            int counter;
            switch (attemptType) {
                case UNAUTHENTICATED -> {
                    blockedIp.setFailedUnauthenticatedAttempts(
                            nullSafe(blockedIp.getFailedUnauthenticatedAttempts()) + 1);
                    counter = blockedIp.getFailedUnauthenticatedAttempts();
                    thresholdKey = IP_BLOCKING_MAX_UNAUTHENTICATED_ATTEMPTS;
                    defaultThreshold = 15;
                }
                case INVALID_JWT -> {
                    blockedIp.setFailedLoginAttempts(nullSafe(blockedIp.getFailedLoginAttempts()) + 1);
                    counter = blockedIp.getFailedLoginAttempts();
                    thresholdKey = IP_BLOCKING_MAX_INVALID_JWT_ATTEMPTS;
                    defaultThreshold = 15;
                }
                case INVALID_OTP -> {
                    blockedIp.setFailedLoginAttempts(nullSafe(blockedIp.getFailedLoginAttempts()) + 1);
                    counter = blockedIp.getFailedLoginAttempts();
                    thresholdKey = IP_BLOCKING_MAX_INVALID_OTP_ATTEMPTS;
                    defaultThreshold = 5;
                }
                case LOGIN -> {
                    blockedIp.setFailedLoginAttempts(nullSafe(blockedIp.getFailedLoginAttempts()) + 1);
                    counter = blockedIp.getFailedLoginAttempts();
                    thresholdKey = IP_BLOCKING_MAX_FAILED_ATTEMPTS;
                    defaultThreshold = 10;
                }
                default -> throw new IllegalStateException("Unhandled attempt type " + attemptType);
            }

            int threshold = environment.getProperty(thresholdKey, Integer.class, defaultThreshold);
            if (counter >= threshold) {
                blockedIp.setActive(true);
                blockedIp.setBlockedAt(LocalDateTime.now());
                blockedIp.setUnblockAt(LocalDateTime.now().plusHours(blockDurationHours));
                log.warn("IP {} blocked after {} {} attempts", ipAddress, counter, attemptType);
            }

            return blockedIpRepository.save(blockedIp);
        } catch (Exception e) {
            throw TraceableException.of("Failed to record authentication attempt for IP %s", e,
                    "Failed to record authentication attempt", ipAddress);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unblockIp(String ipAddress) throws TraceableException {
        try {
            return blockedIpRepository.findByIpAddress(ipAddress)
                    .map(blockedIp -> {
                        blockedIpRepository.delete(blockedIp);
                        log.info("IP {} has been manually unblocked", ipAddress);
                        return true;
                    })
                    .orElse(false);
        } catch (Exception e) {
            throw TraceableException.of("Failed to unblock IP %s", e, "Failed to unblock IP", ipAddress);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unblockAllForUser(String username) throws TraceableException {
        List<BlockedIp> blockedIps = blockedIpRepository.findByUsername(username);
        if (blockedIps.isEmpty()) {
            return 0;
        }
        blockedIpRepository.deleteAll(blockedIps);
        log.info("{} IP entries for user {} have been unblocked", blockedIps.size(), username);
        return blockedIps.size();
    }

    @Override
    @Transactional(readOnly = true)
    public BlockedIp getBlockedIp(String ipAddress) {
        return blockedIpRepository.findByIpAddress(ipAddress).orElse(null);
    }

    @Override
    @Transactional
    public int unblockExpired() {
        List<BlockedIp> expired = blockedIpRepository.findByActiveTrueAndUnblockAtBefore(LocalDateTime.now());
        for (BlockedIp blockedIp : expired) {
            blockedIp.setActive(false);
        }
        blockedIpRepository.saveAll(expired);
        return expired.size();
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private String determineReason(AttemptType attemptType) {
        if (attemptType == null) {
            return "Unauthenticated access attempt";
        }
        return switch (attemptType) {
            case INVALID_JWT -> "Invalid JWT token";
            case UNAUTHENTICATED -> "Unauthenticated access attempt";
            case INVALID_OTP -> "Invalid OTP";
            case LOGIN -> "Invalid credentials";
        };
    }

    private BlockedIpResponse convertToResponse(BlockedIp blockedIp) {
        return BlockedIpResponse.builder()
                .id(blockedIp.getId())
                .ipAddress(blockedIp.getIpAddress())
                .blockedAt(blockedIp.getBlockedAt())
                .unblockAt(blockedIp.getUnblockAt())
                .endpoint(blockedIp.getEndpoint())
                .username(blockedIp.getUsername())
                .reason(blockedIp.getReason())
                .active(blockedIp.isActive())
                .failedAttempts(blockedIp.getFailedAttempts())
                .failedLoginAttempts(blockedIp.getFailedLoginAttempts())
                .failedUnauthenticatedAttempts(blockedIp.getFailedUnauthenticatedAttempts())
                .lastFailureType(blockedIp.getLastFailureType())
                .lastAttemptAt(blockedIp.getLastAttemptAt())
                .build();
    }
}
