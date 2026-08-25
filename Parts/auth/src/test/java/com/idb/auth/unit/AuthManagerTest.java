package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.idb.auth.config.AuthManager;
import com.idb.auth.config.AuthProvider;
import com.idb.auth.constant.AttemptType;
import com.idb.auth.dto.request.BearerAuthenticationToken;
import com.idb.auth.exception.IpBlockedException;
import com.idb.auth.exception.SessionExpiredException;
import com.idb.auth.model.User;
import com.idb.auth.service.IpBlockingService;
import com.idb.auth.service.UserService;
import com.idb.auth.util.JwtUtil;

/**
 * The routing layer: which credential shape is expected for which URI, and which
 * failures count as suspicious.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthManagerTest {

    private static final String LOGIN_URI = "/api/v3/auth/login";
    private static final String PROTECTED_URI = "/api/v3/test/any";

    @Mock private AuthProvider authProvider;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserService userService;
    @Mock private IpBlockingService ipBlockingService;

    @InjectMocks private AuthManager authManager;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authManager, "blockDurationHours", 24);
        when(authProvider.supports(any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void onRequest(String uri, String authorization, String refreshToken) {
        request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr("198.51.100.4");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        if (refreshToken != null) {
            request.addHeader("x-refresh-token", refreshToken);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private User user() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("$2a$10$hash");
        user.setActive(true);
        return user;
    }

    @Test
    @DisplayName("on the login URI, credentials are delegated to the provider")
    void loginUriDelegatesCredentials() {
        onRequest(LOGIN_URI, null, null);
        Authentication token = new UsernamePasswordAuthenticationToken("alice", "pw");
        Authentication expected = new UsernamePasswordAuthenticationToken("alice", "jwt");
        when(authProvider.authenticate(token)).thenReturn(expected);

        assertThat(authManager.authenticate(token)).isSameAs(expected);
    }

    @Test
    @DisplayName("on the login URI with empty credentials, the attempt is recorded as unauthenticated")
    void loginUriWithoutCredentialsIsRecorded() throws Exception {
        onRequest(LOGIN_URI, null, null);

        assertThatThrownBy(() -> authManager.authenticate(
                new UsernamePasswordAuthenticationToken("", "")))
                .hasMessageContaining("Invalid authentication attempt");

        verify(ipBlockingService).recordFailedAttempt(eq("198.51.100.4"), eq(LOGIN_URI), any(), any(),
                eq(AttemptType.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("on a protected URI, a valid bearer token is delegated to the provider")
    void bearerTokenIsDelegated() {
        // A real three-segment JWT shape, so the unverified subject read succeeds.
        onRequest(PROTECTED_URI, "Bearer " + JwtFixtures.jwtWithSubject("alice"), null);
        User user = user();
        when(userService.loadUserByUsername("alice")).thenReturn(user);
        BearerAuthenticationToken bearer = new BearerAuthenticationToken("alice", null, true);
        when(jwtUtil.getBearerToken(any(), any(), eq("$2a$10$hash"), any())).thenReturn(bearer);
        Authentication expected = new UsernamePasswordAuthenticationToken(user, null);
        when(authProvider.authenticate(bearer)).thenReturn(expected);

        // The request parameters are empty - on a protected URI the bearer header is
        // the only credential considered.
        assertThat(authManager.authenticate(new UsernamePasswordAuthenticationToken("", ""))).isSameAs(expected);
    }

    @Test
    @DisplayName("the revocation watermark is read off the user and handed to the validator")
    void watermarkIsPassedThrough() {
        onRequest(PROTECTED_URI, "Bearer " + JwtFixtures.jwtWithSubject("alice"), "refresh.jwt.here");
        User user = user();
        LocalDateTime watermark = LocalDateTime.now().minusMinutes(1);
        user.setTokensValidAfter(watermark);
        when(userService.loadUserByUsername("alice")).thenReturn(user);
        BearerAuthenticationToken bearer = new BearerAuthenticationToken("alice", null, true);
        when(jwtUtil.getBearerToken(any(), any(), any(), any())).thenReturn(bearer);
        when(authProvider.authenticate(bearer)).thenReturn(bearer);

        authManager.authenticate(new UsernamePasswordAuthenticationToken("", ""));

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jwtUtil).getBearerToken(any(), eq("refresh.jwt.here"), eq("$2a$10$hash"), captor.capture());
        assertThat(captor.getValue()).isEqualTo(watermark);
    }

    @Test
    @DisplayName("a structurally invalid bearer token is recorded as an INVALID_JWT attempt")
    void malformedBearerIsRecordedAsInvalidJwt() throws Exception {
        onRequest(PROTECTED_URI, "Bearer not-a-jwt", null);

        assertThatThrownBy(() -> authManager.authenticate(new UsernamePasswordAuthenticationToken("", "")))
                .isInstanceOf(InsufficientAuthenticationException.class);

        verify(ipBlockingService).recordFailedAttempt(eq("198.51.100.4"), eq(PROTECTED_URI), eq(null), any(),
                eq(AttemptType.INVALID_JWT));
    }

    @Test
    @DisplayName("an expired session is NOT recorded as an attack - it is a normal end of life")
    void sessionExpiryIsNotSuspicious() throws Exception {
        onRequest(PROTECTED_URI, "Bearer " + JwtFixtures.jwtWithSubject("alice"), null);
        when(userService.loadUserByUsername("alice")).thenReturn(user());
        when(jwtUtil.getBearerToken(any(), any(), any(), any()))
                .thenThrow(new SessionExpiredException("Session expired. Please login again."));

        assertThatThrownBy(() -> authManager.authenticate(new UsernamePasswordAuthenticationToken("", "")))
                .hasMessageContaining("Session expired");

        // Recording this would let an idle user's own browser eventually block their
        // office IP.
        verify(ipBlockingService, never()).recordFailedAttempt(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("a request with no Authorization header at all is recorded as unauthenticated")
    void noAuthorizationHeaderIsUnauthenticated() throws Exception {
        onRequest(PROTECTED_URI, null, null);

        assertThatThrownBy(() -> authManager.authenticate(new UsernamePasswordAuthenticationToken("", "")))
                .hasMessageContaining("Invalid authentication attempt");

        verify(ipBlockingService).recordFailedAttempt(any(), eq(PROTECTED_URI), any(), any(),
                eq(AttemptType.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("a non-Bearer Authorization scheme is treated as no credentials")
    void nonBearerSchemeIsUnauthenticated() throws Exception {
        onRequest(PROTECTED_URI, "Basic dXNlcjpwYXNz", null);

        assertThatThrownBy(() -> authManager.authenticate(new UsernamePasswordAuthenticationToken("", "")));

        verify(ipBlockingService).recordFailedAttempt(any(), any(), any(), any(),
                eq(AttemptType.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("a blocked IP is refused before any credential work happens")
    void blockedIpShortCircuits() {
        onRequest(PROTECTED_URI, "Bearer " + JwtFixtures.jwtWithSubject("alice"), null);
        when(ipBlockingService.isIpBlocked("198.51.100.4")).thenReturn(true);

        assertThatThrownBy(() -> authManager.authenticate(new UsernamePasswordAuthenticationToken("", "")))
                .isInstanceOf(IpBlockedException.class);

        verify(userService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("the unblock endpoints stay reachable from a blocked IP")
    void unblockEndpointsAreExemptFromTheIpCheck() {
        // Without this exemption an administrator whose own IP tripped the threshold
        // could never lift the block. IpBlockingFilter exempts these too; an
        // exemption honoured by only one of the two checks is no exemption at all.
        for (String uri : new String[] { "/api/v3/ip-block/unblock", "/api/v3/ip-block/unblock-user" }) {
            onRequest(uri, "Bearer " + JwtFixtures.jwtWithSubject("alice"), null);
            when(ipBlockingService.isIpBlocked("198.51.100.4")).thenReturn(true);
            User user = user();
            when(userService.loadUserByUsername("alice")).thenReturn(user);
            BearerAuthenticationToken bearer = new BearerAuthenticationToken("alice", null, true);
            when(jwtUtil.getBearerToken(any(), any(), any(), any())).thenReturn(bearer);
            when(authProvider.authenticate(bearer)).thenReturn(bearer);

            assertThat(authManager.authenticate(new UsernamePasswordAuthenticationToken("", ""))).isNotNull();
        }
    }

    @Test
    @DisplayName("the list endpoint is NOT exempt - only unblocking is")
    void listEndpointIsNotExempt() {
        onRequest("/api/v3/ip-block/list", "Bearer x", null);
        when(ipBlockingService.isIpBlocked("198.51.100.4")).thenReturn(true);

        assertThatThrownBy(() -> authManager.authenticate(new UsernamePasswordAuthenticationToken("", "")))
                .isInstanceOf(IpBlockedException.class);
    }

    @Test
    @DisplayName("a failure while recording the attempt does not mask the authentication failure")
    void recordingFailureIsSwallowed() throws Exception {
        onRequest(PROTECTED_URI, null, null);
        when(ipBlockingService.recordFailedAttempt(any(), any(), any(), any(), any()))
                .thenThrow(com.idb.auth.common.exception.TraceableException.of(
                        "boom", new RuntimeException(), "boom"));

        assertThatThrownBy(() -> authManager.authenticate(new UsernamePasswordAuthenticationToken("", "")))
                .hasMessageContaining("Invalid authentication attempt");
    }

    /** Minimal three-segment token so the unverified subject read succeeds. */
    static final class JwtFixtures {
        static String jwtWithSubject(String subject) {
            java.util.Base64.Encoder enc = java.util.Base64.getUrlEncoder().withoutPadding();
            return enc.encodeToString("{\"alg\":\"HS384\"}".getBytes())
                    + "." + enc.encodeToString(("{\"sub\":\"" + subject + "\"}").getBytes())
                    + ".sig";
        }
    }
}
