package com.idb.auth.controller;

import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_CLEAR_OTP_CACHE;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_REGISTER;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_TOGGLE_2FA;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_UPDATE;
import static com.idb.auth.common.constant.OperationStatus.SUCCESS;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_CHANGE_PASSWORD;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_LOGOUT_ALL;
import static com.idb.auth.constant.AuthConstants.URL_USER_CONTROLLER;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.idb.auth.common.dto.request.SingleParamRequest;
import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.dto.request.ChangePasswordRequest;
import com.idb.auth.dto.request.UserRegistrationRequest;
import com.idb.auth.service.OtpService;
import com.idb.auth.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(URL_USER_CONTROLLER)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OtpService otpService;

    @PostMapping(ENDPOINT_REGISTER)
    public ResponseEntity<ApiResponse<String>> register(@RequestBody UserRegistrationRequest request)
            throws LogOnlyException {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(SUCCESS)
                .message("User registered successfully")
                .data(userService.registerUser(request).getUsername())
                .build());
    }

    @PostMapping(ENDPOINT_UPDATE)
    public ResponseEntity<ApiResponse<String>> update(@RequestBody UserRegistrationRequest request)
            throws LogOnlyException {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(SUCCESS)
                .message("User updated successfully")
                .data(userService.update(request, null).getUsername())
                .build());
    }

    @PostMapping(ENDPOINT_CHANGE_PASSWORD)
    public ResponseEntity<ApiResponse<?>> changePassword(@Valid @RequestBody ChangePasswordRequest request)
            throws LogOnlyException, TraceableException {
        return ResponseEntity.ok(userService.changePassword(request));
    }

    @PostMapping(ENDPOINT_CLEAR_OTP_CACHE)
    public ResponseEntity<ApiResponse<?>> clearOtpCache(@Valid @RequestBody SingleParamRequest<String> request)
            throws TraceableException {
        otpService.clearCache(request.getId());
        return ResponseEntity.ok(ApiResponse.builder()
                .status(SUCCESS)
                .message("OTP cache cleared successfully")
                .build());
    }

    @PostMapping(ENDPOINT_TOGGLE_2FA)
    public ResponseEntity<ApiResponse<String>> toggleTwoFactorAuth(
            @Valid @RequestBody SingleParamRequest<Boolean> request) throws TraceableException {
        return ResponseEntity.ok(userService.toggleTwoFactorAuth(request.getId()));
    }

    /** Revokes every token issued to the caller before now. */
    @PostMapping(ENDPOINT_LOGOUT_ALL)
    public ResponseEntity<ApiResponse<String>> logoutAll() throws LogOnlyException {
        return ResponseEntity.ok(userService.revokeAllSessions());
    }
}
