package com.idb.auth.service.impl;

import static com.idb.auth.constant.AuthConstants.IP_BLOCKING_ENABLED;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.idb.auth.common.dto.request.ApiPageRequest;
import com.idb.auth.common.dto.response.ApiPageResponse;
import com.idb.auth.constant.AttemptType;
import com.idb.auth.dto.response.BlockedIpResponse;
import com.idb.auth.model.BlockedIp;
import com.idb.auth.service.IpBlockingService;

import lombok.extern.slf4j.Slf4j;

/**
 * Selected when {@code auth.ip.block.enabled} is false or absent, so the rest of
 * the auth flow can call {@link IpBlockingService} unconditionally.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = IP_BLOCKING_ENABLED, havingValue = "false", matchIfMissing = true)
public class NoOpIpBlockingServiceImpl implements IpBlockingService {

    @Override
    public boolean isIpBlocked(String ipAddress) {
        return false;
    }

    @Override
    public BlockedIp recordFailedAttempt(String ipAddress, String endpoint, String username, String requestBody,
            AttemptType attemptType) {
        log.debug("Ignoring failed authentication attempt from IP: {} (IP blocking disabled)", ipAddress);
        return null;
    }

    @Override
    public boolean unblockIp(String ipAddress) {
        return false;
    }

    @Override
    public ApiPageResponse<BlockedIpResponse> findBlockedIps(ApiPageRequest<BlockedIpResponse> request) {
        return ApiPageResponse.fromPage(Page.empty(), null);
    }

    @Override
    public int unblockAllForUser(String username) {
        return 0;
    }

    @Override
    public BlockedIp getBlockedIp(String ipAddress) {
        return null;
    }
}
