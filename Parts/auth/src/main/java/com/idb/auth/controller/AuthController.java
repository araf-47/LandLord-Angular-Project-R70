package com.idb.auth.controller;

import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_FORGOT_PASSWORD;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_GENERATE_OTP;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_LOGIN;
import static com.idb.auth.constant.AuthConstants.URL_AUTH_CONTROLLER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.idb.auth.common.dto.request.SingleParamRequest;
import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.dto.request.ForgotPasswordRequest;
import com.idb.auth.dto.request.LoginRequest;
import com.idb.auth.dto.response.AuthResponse;
import com.idb.auth.service.AuthService;
import com.idb.auth.service.OtpService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Public endpoints - see {@code AuthConstants.AUTH_PUBLIC_URLS}. */
@RestController
@RequestMapping(URL_AUTH_CONTROLLER)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping(value = ENDPOINT_LOGIN, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request)
            throws TraceableException {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping(value = ENDPOINT_FORGOT_PASSWORD, consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request)
            throws TraceableException {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping(value = ENDPOINT_GENERATE_OTP, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<?>> generateOtp(@Valid @RequestBody SingleParamRequest<String> request)
            throws LogOnlyException, TraceableException {
        return ResponseEntity.ok(otpService.generateOtp(request.getId()));
    }
}
