package com.idb.auth.service;

import com.idb.auth.common.dto.request.ApiPageRequest;
import com.idb.auth.common.dto.response.ApiPageResponse;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.constant.AttemptType;
import com.idb.auth.dto.response.BlockedIpResponse;
import com.idb.auth.model.BlockedIp;

public interface IpBlockingService {

    ApiPageResponse<BlockedIpResponse> findBlockedIps(ApiPageRequest<BlockedIpResponse> request);

    boolean isIpBlocked(String ipAddress);

    /**
     * Records one failed attempt and blocks the IP once the threshold for that
     * {@link AttemptType} is reached.
     *
     * @return the persisted row, or {@code null} when IP blocking is disabled
     */
    BlockedIp recordFailedAttempt(String ipAddress, String endpoint, String username, String requestBody,
            AttemptType reason) throws TraceableException;

    boolean unblockIp(String ipAddress) throws TraceableException;

    int unblockAllForUser(String username) throws TraceableException;

    BlockedIp getBlockedIp(String ipAddress);
}
