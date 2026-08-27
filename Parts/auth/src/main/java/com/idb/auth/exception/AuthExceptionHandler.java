package com.idb.auth.exception;

import static com.idb.auth.common.constant.OperationStatus.ACCESS_DENIED;
import static com.idb.auth.common.constant.OperationStatus.ACCOUNT_LOCKED;
import static com.idb.auth.common.constant.OperationStatus.BAD_CREDENTIALS;
import static com.idb.auth.common.constant.OperationStatus.INVALID_OTP;
import static com.idb.auth.common.constant.OperationStatus.INVALID_TOKEN_IN_HEADER;
import static com.idb.auth.common.constant.OperationStatus.IP_BLOCKED;
import static com.idb.auth.common.constant.OperationStatus.OTP_REQUIRED;
import static com.idb.auth.common.constant.OperationStatus.SESSION_EXPIRED;
import static com.idb.auth.common.constant.OperationStatus.USER_NOT_FOUND;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.util.RequestContextUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Maps authentication and authorization failures raised from inside the MVC
 * dispatch (i.e. by {@code AuthService}) onto the {@code ApiResponse} contract.
 * Failures raised earlier, by the security filter chain, are rendered by
 * {@code AuthEntryPoint} instead.
 *
 * <p>Ordered HIGHEST so these win over {@code BaseExceptionHandler}'s
 * {@code @ExceptionHandler(Exception.class)}.
 *
 * <p>Scoped to {@code com.idb.auth} only - see {@code BaseExceptionHandler}'s
 * javadoc for why an unscoped advice is unsafe on a module embedded as a
 * library into a host application.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.idb.auth")
public class AuthExceptionHandler {

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleInsufficientAuthentication(InsufficientAuthenticationException e) {
        log.error("Insufficient authentication from: {}", RequestContextUtil.getClientIp());
        return unauthorized(ACCESS_DENIED, e.getMessage());
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ApiResponse<?>> handleSessionExpired(SessionExpiredException e) {
        return unauthorized(SESSION_EXPIRED, e.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleUsernameNotFound(UsernameNotFoundException e) {
        log.error("Username not found from: {}", RequestContextUtil.getClientIp());
        return unauthorized(USER_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials(BadCredentialsException e) {
        log.error("Invalid credentials from: {}. Error: {}", RequestContextUtil.getClientIp(), e.getMessage());
        return unauthorized(BAD_CREDENTIALS, e.getMessage());
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<?>> handleLockedAccount(LockedException e) {
        log.warn("Attempt to access locked account from: {}", RequestContextUtil.getClientIp());
        return unauthorized(ACCOUNT_LOCKED, "Your account is temporarily locked due to too many failed login "
                + "attempts. You can reset your password to unlock it, or try again later.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException e) {
        log.error("Access denied from: {}. Error: {}", RequestContextUtil.getClientIp(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.builder().status(ACCESS_DENIED).message(e.getMessage()).build());
    }

    @ExceptionHandler(InvalidTokenInHeaderException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidTokenInHeader(InvalidTokenInHeaderException e) {
        log.error("Invalid token in header from: {}. Error: {}", RequestContextUtil.getClientIp(), e.getMessage());
        return unauthorized(INVALID_TOKEN_IN_HEADER, e.getMessage());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleCredentialsNotFound(AuthenticationCredentialsNotFoundException e) {
        log.error("Authentication credentials not found from: {}. Error: {}",
                RequestContextUtil.getClientIp(), e.getMessage());
        return unauthorized(INVALID_TOKEN_IN_HEADER, e.getMessage());
    }

    @ExceptionHandler(IpBlockedException.class)
    public ResponseEntity<ApiResponse<?>> handleIpBlocked(IpBlockedException e) {
        log.warn("Access attempt from blocked IP: {}", e.getIpAddress());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.builder()
                .status(IP_BLOCKED)
                .message(("Your IP address has been blocked due to too many failed authentication attempts. "
                        + "Please try again after %d hours.").formatted(e.getBlockDurationHours()))
                .build());
    }

    @ExceptionHandler(OtpRequiredException.class)
    public ResponseEntity<ApiResponse<?>> handleOtpRequired(OtpRequiredException e) {
        return ResponseEntity.ok(ApiResponse.builder().status(OTP_REQUIRED).message(e.getMessage()).build());
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidOtp(InvalidOtpException e) {
        log.warn("Invalid OTP attempt from: {}", RequestContextUtil.getClientIp());
        return unauthorized(INVALID_OTP, e.getMessage());
    }

    /**
     * Fallback for any other {@link AuthenticationException}. Declared last so the
     * more specific handlers above take precedence.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException e) {
        return unauthorized(ACCESS_DENIED, e.getMessage());
    }

    private ResponseEntity<ApiResponse<?>> unauthorized(
            com.idb.auth.common.constant.OperationStatus status, String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.builder().status(status).message(message).build());
    }
}
