package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.config.AuthEntryPoint;
import com.idb.auth.exception.InvalidTokenInHeaderException;
import com.idb.auth.exception.IpBlockedException;
import com.idb.auth.exception.SessionExpiredException;
import com.idb.auth.util.AuthUtil;

import tools.jackson.databind.json.JsonMapper;

/**
 * The entry point turns filter-chain authentication failures into the same
 * {@code ApiResponse} envelope the controllers emit, so a client never has to
 * parse two error shapes.
 */
class AuthEntryPointTest {

    private AuthEntryPoint entryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        entryPoint = new AuthEntryPoint(JsonMapper.builder().build());
        request = new MockHttpServletRequest("GET", "/api/v3/test/any");
        request.setRemoteAddr("198.51.100.9");
        request.addHeader("User-Agent", "junit");
        response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private String commence(AuthenticationException ex) throws Exception {
        entryPoint.commence(request, response, ex);
        return response.getContentAsString();
    }

    @Test
    @DisplayName("bad credentials map to BAD_CREDENTIALS with 401 and the original message")
    void badCredentials() throws Exception {
        String body = commence(AuthUtil.getAuthenticationException("Invalid username or password",
                new BadCredentialsException("Invalid username or password")));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(body).contains("\"status\":\"BAD_CREDENTIALS\"").contains("Invalid username or password");
    }

    @Test
    @DisplayName("a session expiry maps to SESSION_EXPIRED, not a generic denial")
    void sessionExpired() throws Exception {
        String body = commence(new SessionExpiredException("Session expired. Please login again."));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body).contains("\"status\":\"SESSION_EXPIRED\"").contains("Session expired");
    }

    @Test
    @DisplayName("a wrapped session expiry is still SESSION_EXPIRED")
    void wrappedSessionExpired() throws Exception {
        String body = commence(AuthUtil.getAuthenticationException("Session has been revoked. Please login again.",
                new SessionExpiredException("Session has been revoked. Please login again.")));

        assertThat(body).contains("\"status\":\"SESSION_EXPIRED\"").contains("revoked");
    }

    @Test
    @DisplayName("each account-state failure gets its own status so a client can react correctly")
    void accountStateFailuresAreDistinct() throws Exception {
        assertThat(commence(AuthUtil.getAuthenticationException("x", new DisabledException("x"))))
                .contains("\"status\":\"USER_INACTIVE\"");

        response = new MockHttpServletResponse();
        assertThat(commence(AuthUtil.getAuthenticationException("x", new LockedException("x"))))
                .contains("\"status\":\"USER_LOCKED\"");

        response = new MockHttpServletResponse();
        assertThat(commence(AuthUtil.getAuthenticationException("x", new AccountExpiredException("x"))))
                .contains("\"status\":\"USER_EXPIRED\"");

        response = new MockHttpServletResponse();
        assertThat(commence(AuthUtil.getAuthenticationException("x", new CredentialsExpiredException("x"))))
                .contains("\"status\":\"CREDENTIALS_EXPIRED\"");
    }

    @Test
    @DisplayName("an unclassified failure falls back to ACCESS_DENIED")
    void unclassifiedFailureFallsBack() throws Exception {
        String body = commence(new InsufficientAuthenticationException("no credentials"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body).contains("\"status\":\"ACCESS_DENIED\"")
                .contains("not authorized to access this resource");
    }

    @Test
    @DisplayName("a blocked IP gets 403 IP_BLOCKED with the duration, not a generic 401")
    void ipBlockedIsForbiddenWithDuration() throws Exception {
        String body = commence(new IpBlockedException("198.51.100.9", 24));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body).contains("\"status\":\"IP_BLOCKED\"").contains("24 hours");
    }

    @Test
    @DisplayName("a service exception in the cause chain wins, and is reported as HTTP 200")
    void serviceExceptionTakesPrecedence() throws Exception {
        String body = commence(AuthUtil.getAuthenticationException("wrapped",
                LogOnlyException.of("internal detail", "Your account needs attention")));

        // The service layer already decided what the client should see.
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).contains("Your account needs attention").doesNotContain("internal detail");
    }

    @Test
    @DisplayName("suspicious failures blank the token headers so a client stops replaying a bad token")
    void suspiciousFailuresBlankTokenHeaders() throws Exception {
        for (AuthenticationException ex : new AuthenticationException[] {
                new InvalidTokenInHeaderException("bad"),
                new InsufficientAuthenticationException("bad"),
                new AuthenticationCredentialsNotFoundException("bad"),
                new BadCredentialsException("bad") }) {
            response = new MockHttpServletResponse();
            commence(ex);
            assertThat(response.getHeader("x-access-token")).isEmpty();
            assertThat(response.getHeader("x-refresh-token")).isEmpty();
        }
    }

    @Test
    @DisplayName("a plain session expiry is not suspicious, so the headers are left alone")
    void sessionExpiryDoesNotBlankHeaders() throws Exception {
        commence(new SessionExpiredException("Session expired. Please login again."));

        // Blanking here would make an ordinary idle timeout look like an attack and
        // would throw away a refresh token the client could still legitimately use.
        assertThat(response.getHeader("x-access-token")).isNull();
        assertThat(response.getHeader("x-refresh-token")).isNull();
    }
}
