package com.idb.auth.config;

import static com.idb.auth.common.constant.CommonConstants.BEARER_TOKEN_PREFIX;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_LOGIN;
import static com.idb.auth.common.constant.CommonConstants.REFRESH_TOKEN_HEADER;
import static com.idb.auth.constant.AuthConstants.URL_AUTH_CONTROLLER;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.common.util.RequestContextUtil;
import com.idb.auth.common.util.StringUtil;
import com.idb.auth.constant.AttemptType;
import com.idb.auth.constant.AuthConstants;
import com.idb.auth.dto.request.BearerAuthenticationToken;
import com.idb.auth.exception.IpBlockedException;
import com.idb.auth.exception.SessionExpiredException;
import com.idb.auth.model.User;
import com.idb.auth.service.IpBlockingService;
import com.idb.auth.service.UserService;
import com.idb.auth.util.AuthUtil;
import com.idb.auth.util.JwtUtil;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The single entry point for every authentication decision, replacing Spring's
 * {@code ProviderManager}. It routes by request URI:
 *
 * <ul>
 * <li>{@code /auth/login} - username/password, delegated to {@code AuthProvider}.
 * Wrong credentials here do <b>not</b> count towards IP blocking directly;
 * {@code AuthProvider} records them as {@code LOGIN} attempts.
 * <li>anything else - bearer token, validated and possibly refreshed, then
 * delegated to {@code AuthProvider} to attach authorities.
 * </ul>
 *
 * Requests that fall through either branch are recorded as
 * {@code UNAUTHENTICATED} attempts and rejected.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthManager implements AuthenticationManager {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final AuthProvider authProvider;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final IpBlockingService ipBlockingService;

    @Value("${auth.ip.block.block.duration.hours:1440}")
    private int blockDurationHours;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String clientIp = RequestContextUtil.getClientIp();
        HttpServletRequest request = RequestContextUtil.getRequest();
        String requestURI = request.getRequestURI();

        // isIpBlockExempt, not an unconditional check: IpBlockingFilter lets the
        // unblock endpoints through so a locked-out administrator can recover, and
        // re-checking here without the same exemption would silently cancel that.
        if (!AuthConstants.isIpBlockExempt(requestURI) && ipBlockingService.isIpBlocked(clientIp)) {
            throw new IpBlockedException(clientIp, blockDurationHours);
        }

        try {
            if (requestURI.equals(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN)) {
                String username = String.valueOf(authentication.getPrincipal());
                String password = String.valueOf(authentication.getCredentials());
                if (StringUtil.isNotEmpty(username) && StringUtil.isNotEmpty(password)
                        && authProvider.supports(authentication.getClass())) {
                    return authProvider.authenticate(authentication);
                }
            } else {
                Authentication bearerAuthentication = authenticateBearer(request, clientIp, requestURI);
                if (bearerAuthentication != null) {
                    return bearerAuthentication;
                }
            }

            recordUnauthenticatedAttempt(clientIp, requestURI, getRequestData(request));
            throw AuthUtil.getAuthenticationException(getInvalidAuthenticationMessage(request, clientIp), null);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw AuthUtil.getAuthenticationException(getInvalidAuthenticationMessage(request, clientIp), e);
        }
    }

    private Authentication authenticateBearer(HttpServletRequest request, String clientIp, String requestURI) {
        String bearerToken = RequestContextUtil.getAuthorization();
        if (StringUtil.isEmpty(bearerToken) || !bearerToken.startsWith(BEARER_TOKEN_PREFIX)) {
            return null;
        }

        String accessToken = bearerToken.substring(BEARER_TOKEN_PREFIX.length());
        String refreshToken = RequestContextUtil.getHeaders().get(REFRESH_TOKEN_HEADER);

        try {
            String username;
            try {
                username = AuthUtil.getUsernameFromAccessToken(accessToken);
            } catch (InsufficientAuthenticationException e) {
                recordInvalidJwtAttempt(clientIp, requestURI, getRequestData(request));
                throw e;
            }

            UserDetails userDetails = userService.loadUserByUsername(username);
            LocalDateTime tokensValidAfter = (userDetails instanceof User user) ? user.getTokensValidAfter() : null;

            BearerAuthenticationToken bearerAuth = jwtUtil.getBearerToken(accessToken, refreshToken,
                    userDetails.getPassword(), tokensValidAfter);
            if (bearerAuth != null && authProvider.supports(bearerAuth.getClass())) {
                return authProvider.authenticate(bearerAuth);
            }
            return null;
        } catch (SessionExpiredException | ExpiredJwtException e) {
            // An expired session is a normal end-of-life event, not an attack.
            throw AuthUtil.getAuthenticationException(e.getMessage(), e);
        } catch (InsufficientAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            recordInvalidJwtAttempt(clientIp, requestURI, getRequestData(request));
            throw e;
        }
    }

    private void recordUnauthenticatedAttempt(String clientIp, String endpoint, String requestData) {
        try {
            ipBlockingService.recordFailedAttempt(clientIp, endpoint, extractUsername(requestData), requestData,
                    AttemptType.UNAUTHENTICATED);
            log.warn("Recorded unauthenticated access attempt from IP: {} to endpoint: {}", clientIp, endpoint);
        } catch (TraceableException e) {
            log.error("Failed to record unauthenticated access attempt: {}", e.getMessage());
        }
    }

    private void recordInvalidJwtAttempt(String clientIp, String endpoint, String requestData) {
        try {
            ipBlockingService.recordFailedAttempt(clientIp, endpoint, null, requestData, AttemptType.INVALID_JWT);
            log.warn("Recorded invalid JWT token attempt from IP: {} to endpoint: {}", clientIp, endpoint);
        } catch (TraceableException e) {
            log.error("Failed to record invalid JWT attempt: {}", e.getMessage());
        }
    }

    /**
     * Best-effort username extraction for the audit trail. Parsed as JSON rather
     * than by index arithmetic on the raw body, so a body containing "username"
     * inside another value cannot corrupt the recorded value.
     */
    private String extractUsername(String requestData) {
        if (StringUtil.isEmpty(requestData)) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(requestData);
            JsonNode username = node.get("username");
            return username == null || username.isNull() ? null : username.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String getInvalidAuthenticationMessage(HttpServletRequest request, String clientIp) {
        return "Invalid authentication attempt from: %s. Request URI: %s. Data: %s"
                .formatted(clientIp, request.getRequestURI(), getRequestData(request));
    }

    private String getRequestData(HttpServletRequest request) {
        try {
            return request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException | IllegalStateException e) {
            return "";
        }
    }
}
