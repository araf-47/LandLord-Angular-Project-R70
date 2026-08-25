package com.idb.auth.controller;

import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_LIST;
import static com.idb.auth.common.constant.OperationStatus.SUCCESS;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_IP_BLOCK_UNBLOCK;
import static com.idb.auth.constant.AuthConstants.URL_USER_BLOCK_CONTROLLER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.idb.auth.common.dto.request.SingleParamRequest;
import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.dto.response.BlockedUserResponse;
import com.idb.auth.model.User;
import com.idb.auth.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(URL_USER_BLOCK_CONTROLLER)
@RequiredArgsConstructor
public class UserBlockController {

    private final UserService userService;

    @GetMapping(value = ENDPOINT_LIST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<BlockedUserResponse>>> listBlockedUsers() {
        List<BlockedUserResponse> response = userService.findAllBlockedUsers().stream()
                .map(this::convertToResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.<List<BlockedUserResponse>>builder()
                .status(SUCCESS)
                .message("Blocked users retrieved successfully")
                .data(response)
                .build());
    }

    @PostMapping(value = ENDPOINT_IP_BLOCK_UNBLOCK, consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> unblockUser(@Valid @RequestBody SingleParamRequest<String> request)
            throws TraceableException {
        boolean unblocked = userService.unblockUser(request.getId());
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(SUCCESS)
                .message(unblocked
                        ? "User " + request.getId() + " unblocked successfully"
                        : "User " + request.getId() + " was not blocked or could not be unblocked")
                .build());
    }

    private BlockedUserResponse convertToResponse(User user) {
        return BlockedUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lockedUntil(user.getLockedUntil())
                .lastFailedLoginAt(user.getLastFailedLoginAt())
                .build();
    }
}
