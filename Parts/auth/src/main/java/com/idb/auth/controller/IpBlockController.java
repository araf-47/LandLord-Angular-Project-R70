package com.idb.auth.controller;

import static com.idb.auth.common.constant.OperationStatus.SUCCESS;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_IP_BLOCK_LIST;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_IP_BLOCK_UNBLOCK;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_IP_BLOCK_UNBLOCK_USER;
import static com.idb.auth.constant.AuthConstants.IP_BLOCKING_ENABLED;
import static com.idb.auth.constant.AuthConstants.URL_IP_BLOCK_CONTROLLER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.idb.auth.common.dto.request.ApiPageRequest;
import com.idb.auth.common.dto.request.SingleParamRequest;
import com.idb.auth.common.dto.response.ApiPageResponse;
import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.dto.response.BlockedIpResponse;
import com.idb.auth.service.IpBlockingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(URL_IP_BLOCK_CONTROLLER)
@RequiredArgsConstructor
@ConditionalOnProperty(name = IP_BLOCKING_ENABLED, havingValue = "true")
public class IpBlockController {

    private final IpBlockingService ipBlockingService;

    @PostMapping(value = ENDPOINT_IP_BLOCK_LIST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiPageResponse<BlockedIpResponse>> listBlockedIps(
            @RequestBody ApiPageRequest<BlockedIpResponse> request) {
        return ResponseEntity.ok(ipBlockingService.findBlockedIps(request));
    }

    @PostMapping(value = ENDPOINT_IP_BLOCK_UNBLOCK, consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> unblockIp(@Valid @RequestBody SingleParamRequest<String> request)
            throws TraceableException {
        boolean unblocked = ipBlockingService.unblockIp(request.getId());
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(SUCCESS)
                .message(unblocked
                        ? "IP " + request.getId() + " unblocked successfully"
                        : "IP " + request.getId() + " was not blocked or could not be unblocked")
                .build());
    }

    @PostMapping(value = ENDPOINT_IP_BLOCK_UNBLOCK_USER, consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> unblockUser(@Valid @RequestBody SingleParamRequest<String> request)
            throws TraceableException {
        int unblockedCount = ipBlockingService.unblockAllForUser(request.getId());
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(SUCCESS)
                .message(unblockedCount > 0
                        ? "Unblocked " + unblockedCount + " IP entries for user " + request.getId()
                        : "No blocked IP entries found for user " + request.getId())
                .build());
    }
}
