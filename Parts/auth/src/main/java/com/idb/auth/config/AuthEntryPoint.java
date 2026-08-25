package com.idb.auth.config;

import static com.idb.auth.common.constant.CommonConstants.ACCESS_TOKEN_HEADER;
import static com.idb.auth.common.constant.CommonConstants.EMPTY_STRING;
import static com.idb.auth.common.constant.CommonConstants.REFRESH_TOKEN_HEADER;
import static com.idb.auth.common.constant.OperationStatus.ACCESS_DENIED;
import static com.idb.auth.common.constant.OperationStatus.BAD_CREDENTIALS;
import static com.idb.auth.common.constant.OperationStatus.CREDENTIALS_EXPIRED;
import static com.idb.auth.common.constant.OperationStatus.IP_BLOCKED;
import static com.idb.auth.common.constant.OperationStatus.SESSION_EXPIRED;
import static com.idb.auth.common.constant.OperationStatus.USER_EXPIRED;
import static com.idb.auth.common.constant.OperationStatus.USER_INACTIVE;
import static com.idb.auth.common.constant.OperationStatus.USER_LOCKED;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.idb.auth.common.constant.OperationStatus;
import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.util.ExceptionUtil;
import com.idb.auth.common.util.RequestContextUtil;
import com.idb.auth.exception.InvalidTokenInHeaderException;
import com.idb.auth.exception.IpBlockedException;
import com.idb.auth.exception.SessionExpiredException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Renders authentication failures raised inside the security filter chain (i.e.
 * before MVC dispatch) as the same {@code ApiResponse} envelope the controllers
 * use, so a client never has to parse two error shapes.
 *
 * <p>On a suspicious failure the stale token headers are blanked, prompting
 * clients that cache them to drop the credentials rather than keep retrying.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // A service-layer exception in the cause chain already carries a
        // client-facing message; prefer it.
        ApiResponse<?> serviceExceptionResponse = ExceptionUtil.extractServiceException(authException);
        if (serviceExceptionResponse != null) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(objectMapper.writeValueAsString(serviceExceptionResponse));
            return;
        }

        if (authException instanceof IpBlockedException ipBlocked) {
            writeIpBlocked(response, ipBlocked);
            return;
        }

        boolean isSuspicious = isSuspiciousException(authException);
        ApiResponse<?> errorResponse = handleAuthenticationException(authException);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        if (isSuspicious) {
            response.setHeader(ACCESS_TOKEN_HEADER, EMPTY_STRING);
            response.setHeader(REFRESH_TOKEN_HEADER, EMPTY_STRING);
            logSuspiciousActivity(request, authException);
        }

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private void writeIpBlocked(HttpServletResponse response, IpBlockedException e) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.builder()
                .status(IP_BLOCKED)
                .message(("Your IP address has been blocked due to too many failed authentication attempts. "
                        + "Please try again after %d hours.").formatted(e.getBlockDurationHours()))
                .build()));
    }

    private ApiResponse<?> handleAuthenticationException(AuthenticationException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;

        OperationStatus status;
        String message;
        if (cause instanceof SessionExpiredException || e instanceof SessionExpiredException) {
            status = SESSION_EXPIRED;
            message = cause.getMessage();
        } else if (cause instanceof BadCredentialsException) {
            status = BAD_CREDENTIALS;
            message = e.getMessage();
        } else if (cause instanceof DisabledException) {
            status = USER_INACTIVE;
            message = "Account is disabled";
        } else if (cause instanceof LockedException) {
            status = USER_LOCKED;
            message = "Account is locked";
        } else if (cause instanceof AccountExpiredException) {
            status = USER_EXPIRED;
            message = "Account has expired";
        } else if (cause instanceof CredentialsExpiredException) {
            status = CREDENTIALS_EXPIRED;
            message = "Your password is expired. Please change your password.";
        } else {
            status = ACCESS_DENIED;
            message = "You are not authorized to access this resource";
        }

        return ApiResponse.builder().status(status).message(message).build();
    }

    private boolean isSuspiciousException(AuthenticationException e) {
        return e instanceof InvalidTokenInHeaderException
                || e instanceof InsufficientAuthenticationException
                || e instanceof AuthenticationCredentialsNotFoundException
                || e instanceof BadCredentialsException;
    }

    /**
     * Uses the request handed to {@link #commence}, not
     * {@code RequestContextHolder}. This entry point is invoked directly by
     * {@code AuthFilter}, and a thread-bound lookup only happens to work there
     * because Boot registers {@code RequestContextFilter} ahead of the security
     * chain. Depending on that ordering would make a logging call able to throw an
     * NPE out of {@code commence}, which would escape to
     * {@code GlobalExceptionFilter} and turn a clean 401 into a generic error.
     */
    private void logSuspiciousActivity(HttpServletRequest request, AuthenticationException e) {
        log.warn("Suspicious authentication attempt from IP: {}, User-Agent: {}, Error: {}",
                RequestContextUtil.getClientIp(request), request.getHeader("User-Agent"), e.getMessage());
    }
}
