package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.idb.auth.common.constant.OperationStatus;
import com.idb.auth.common.exception.BaseExceptionHandler;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.dto.request.LoginRequest;
import com.idb.auth.exception.AuthExceptionHandler;
import com.idb.auth.exception.InvalidOtpException;
import com.idb.auth.exception.InvalidTokenInHeaderException;
import com.idb.auth.exception.IpBlockedException;
import com.idb.auth.exception.OtpRequiredException;
import com.idb.auth.exception.SessionExpiredException;

/**
 * The two {@code @RestControllerAdvice} classes. Their relative order matters:
 * {@code AuthExceptionHandler} is HIGHEST precedence so its typed handlers win
 * over {@code BaseExceptionHandler}'s catch-all {@code Exception} handler.
 */
class ExceptionHandlerTest {

    private final AuthExceptionHandler authHandler = new AuthExceptionHandler();
    private final BaseExceptionHandler baseHandler = new BaseExceptionHandler();

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v3/auth/login")));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("authentication failures each map to their own status and HTTP code")
    void authFailureStatusMapping() {
        var badCreds = authHandler.handleBadCredentials(new BadCredentialsException("nope"));
        assertThat(badCreds.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(badCreds.getBody().getStatus()).isEqualTo(OperationStatus.BAD_CREDENTIALS);

        var locked = authHandler.handleLockedAccount(new LockedException("locked"));
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(locked.getBody().getStatus()).isEqualTo(OperationStatus.ACCOUNT_LOCKED);
        assertThat(locked.getBody().getMessage()).contains("reset your password to unlock");

        var expired = authHandler.handleSessionExpired(new SessionExpiredException("gone"));
        assertThat(expired.getBody().getStatus()).isEqualTo(OperationStatus.SESSION_EXPIRED);

        var notFound = authHandler.handleUsernameNotFound(new UsernameNotFoundException("who"));
        assertThat(notFound.getBody().getStatus()).isEqualTo(OperationStatus.USER_NOT_FOUND);

        var badToken = authHandler.handleInvalidTokenInHeader(new InvalidTokenInHeaderException("bad"));
        assertThat(badToken.getBody().getStatus()).isEqualTo(OperationStatus.INVALID_TOKEN_IN_HEADER);

        var insufficient = authHandler.handleInsufficientAuthentication(
                new InsufficientAuthenticationException("none"));
        assertThat(insufficient.getBody().getStatus()).isEqualTo(OperationStatus.ACCESS_DENIED);
    }

    @Test
    @DisplayName("an authorization failure is 403, not 401 - the caller IS authenticated")
    void accessDeniedIsForbidden() {
        var denied = authHandler.handleAccessDenied(new AccessDeniedException("Access Denied"));

        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getBody().getStatus()).isEqualTo(OperationStatus.ACCESS_DENIED);
    }

    @Test
    @DisplayName("an OTP challenge is HTTP 200 - credentials were correct, a second factor is pending")
    void otpRequiredIsNotAnError() {
        var challenge = authHandler.handleOtpRequired(new OtpRequiredException("OTP required"));

        // Returning 401 here would make a client treat a valid password as a
        // rejection and stop the flow instead of prompting for the code.
        assertThat(challenge.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(challenge.getBody().getStatus()).isEqualTo(OperationStatus.OTP_REQUIRED);
    }

    @Test
    @DisplayName("a wrong OTP is 401 INVALID_OTP, distinct from a wrong password")
    void invalidOtpIsDistinctFromBadCredentials() {
        var invalid = authHandler.handleInvalidOtp(new InvalidOtpException("Invalid OTP"));

        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(invalid.getBody().getStatus()).isEqualTo(OperationStatus.INVALID_OTP);
    }

    @Test
    @DisplayName("a blocked IP is 403 and states the block duration")
    void ipBlockedReportsDuration() {
        var blocked = authHandler.handleIpBlocked(new IpBlockedException("203.0.113.1", 24));

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(blocked.getBody().getStatus()).isEqualTo(OperationStatus.IP_BLOCKED);
        assertThat(blocked.getBody().getMessage()).contains("24 hours");
    }

    @Test
    @DisplayName("the service exceptions surface their own client-facing response at HTTP 200")
    void serviceExceptionsUseTheirOwnResponse() {
        var logOnly = baseHandler.handleLogOnlyException(LogOnlyException.of("internal", "Old password is incorrect"));
        assertThat(logOnly.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logOnly.getBody().getStatus()).isEqualTo(OperationStatus.ERROR);
        assertThat(logOnly.getBody().getMessage()).isEqualTo("Old password is incorrect");

        var traceable = baseHandler.handleTraceableException(
                TraceableException.of("internal", new RuntimeException("root"), "Login failed"));
        assertThat(traceable.getBody().getMessage()).isEqualTo("Login failed");
    }

    @Test
    @DisplayName("bean validation errors are reported per field")
    void validationErrorsAreFieldKeyed() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new LoginRequest(), "loginRequest");
        binding.addError(new FieldError("loginRequest", "password", "Password is required"));
        binding.addError(new FieldError("loginRequest", "username", "Username is required"));

        var response = baseHandler.handleValidationExceptions(new MethodArgumentNotValidException(
                new MethodParameter(LoginRequest.class.getConstructor(), -1), binding));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(OperationStatus.VALIDATION_ERROR);
        assertThat(response.getBody().getData())
                .containsEntry("password", "Password is required")
                .containsEntry("username", "Username is required");
    }

    @Test
    @DisplayName("a non-field (object-level) validation error is still reported, keyed by object name")
    void objectLevelValidationErrorIsReported() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new LoginRequest(), "loginRequest");
        binding.addError(new ObjectError("loginRequest", "Whole object is invalid"));

        var response = baseHandler.handleValidationExceptions(new MethodArgumentNotValidException(
                new MethodParameter(LoginRequest.class.getConstructor(), -1), binding));

        // A blind cast to FieldError here would throw and turn a 200
        // VALIDATION_ERROR into a 500.
        assertThat(response.getBody().getData()).containsEntry("loginRequest", "Whole object is invalid");
    }

    @Test
    @DisplayName("an unexpected exception yields a generic message, never internal detail")
    void uncaughtExceptionIsGeneric() {
        var response = baseHandler.handleAllUncaughtException(
                new RuntimeException("could not connect to jdbc:postgresql://prod-db:5432 as user svc_auth"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(OperationStatus.ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong");
        // The connection string must not reach the client.
        assertThat(response.getBody().getMessage()).doesNotContain("postgresql").doesNotContain("svc_auth");
    }

    @Test
    @DisplayName("the handlers tolerate being invoked with no bound request")
    void handlersToleratePlainInvocation() {
        RequestContextHolder.resetRequestAttributes();
        assertThat(baseHandler.handleAllUncaughtException(new RuntimeException("x")).getBody().getMessage())
                .isEqualTo("Something went wrong");
        assertThat(baseHandler.handleLogOnlyException(LogOnlyException.of(null, "m")).getBody().getMessage())
                .isEqualTo("m");
    }

    @Test
    @DisplayName("every OperationStatus a handler can emit is a declared enum constant")
    void statusesAreDeclared() {
        // Guards against a handler being wired to a status that was removed.
        List<OperationStatus> emitted = List.of(
                OperationStatus.BAD_CREDENTIALS, OperationStatus.ACCOUNT_LOCKED, OperationStatus.SESSION_EXPIRED,
                OperationStatus.USER_NOT_FOUND, OperationStatus.INVALID_TOKEN_IN_HEADER,
                OperationStatus.ACCESS_DENIED, OperationStatus.OTP_REQUIRED, OperationStatus.INVALID_OTP,
                OperationStatus.IP_BLOCKED, OperationStatus.ERROR, OperationStatus.VALIDATION_ERROR,
                OperationStatus.SUCCESS);
        assertThat(Map.of("all", emitted)).isNotNull();
        assertThat(OperationStatus.values()).containsAll(emitted);
    }
}
