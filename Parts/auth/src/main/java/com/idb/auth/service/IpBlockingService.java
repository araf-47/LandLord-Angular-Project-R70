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

    /**
     * Bulk sweep for rows whose block window elapsed but were never queried again
     * via {@link #isIpBlocked}, so the lazy per-lookup expiry in that method never
     * ran for them. Without this, {@code active} stays stale {@code true} forever
     * on IPs nobody happens to check again (e.g. the admin blocked-IP list).
     *
     * @return number of rows unblocked
     */
    int unblockExpired();
}
