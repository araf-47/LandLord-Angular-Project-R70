package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.idb.auth.config.AuthProvider;
import com.idb.auth.constant.AttemptType;
import com.idb.auth.dto.request.BearerAuthenticationToken;
import com.idb.auth.model.Role;
import com.idb.auth.model.User;
import com.idb.auth.service.IpBlockingService;
import com.idb.auth.service.UserService;
import com.idb.auth.util.JwtUtil;

/** Credential verification and the per-account lockout counter. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthProviderTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private IpBlockingService ipBlockingService;

    @InjectMocks private AuthProvider authProvider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authProvider, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(authProvider, "lockoutDurationMinutes", 30L);
        // handleFailedLogin reads the current request for the audit trail.
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest("POST", "/api/v3/auth/login")));
    }

    private User activeUser() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("$2a$10$storedhash");
        user.setActive(true);
        Role role = new Role();
        role.setName("ADMIN");
        user.setRoles(List.of(role));
        return user;
    }

    private Authentication loginToken() {
        return new UsernamePasswordAuthenticationToken("alice", "Right@Pass1");
    }

    @Test
    @DisplayName("a correct password yields an authentication whose credentials are a fresh access token")
    void successfulLoginMintsAccessToken() {
        User user = activeUser();
        when(userService.loadUserByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("Right@Pass1", "$2a$10$storedhash")).thenReturn(true);
        when(jwtUtil.generateAccessToken("alice", "$2a$10$storedhash")).thenReturn("minted.jwt.token");

        Authentication result = authProvider.authenticate(loginToken());

        assertThat(result.getPrincipal()).isSameAs(user);
        assertThat(result.getCredentials()).isEqualTo("minted.jwt.token");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ADMIN");
        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("an unknown user is reported exactly like a wrong password")
    void unknownUserLooksLikeBadCredentials() {
        when(userService.loadUserByUsername("alice")).thenThrow(new UsernameNotFoundException("nope"));

        assertThatThrownBy(() -> authProvider.authenticate(loginToken()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    @DisplayName("a null UserDetails is treated as bad credentials rather than NPEing")
    void nullUserDetailsIsBadCredentials() {
        when(userService.loadUserByUsername("alice")).thenReturn(null);

        assertThatThrownBy(() -> authProvider.authenticate(loginToken()))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("an inactive account is refused before the password is even checked")
    void inactiveAccountIsRefusedFirst() {
        User user = activeUser();
        user.setActive(false);
        when(userService.loadUserByUsername("alice")).thenReturn(user);

        assertThatThrownBy(() -> authProvider.authenticate(loginToken()))
                .isInstanceOf(InsufficientAuthenticationException.class)
                .hasMessageContaining("disabled");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("a locked account is refused before the password is checked, so a lock cannot be probed")
    void lockedAccountIsRefusedBeforePasswordCheck() {
        User user = activeUser();
        user.lockAccount(30);
        when(userService.loadUserByUsername("alice")).thenReturn(user);

        assertThatThrownBy(() -> authProvider.authenticate(loginToken()))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("temporarily locked");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("a wrong password increments the counter and records a LOGIN attempt")
    void wrongPasswordIncrementsAndRecords() throws Exception {
        User user = activeUser();
        when(userService.loadUserByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("Right@Pass1", "$2a$10$storedhash")).thenReturn(false);

        assertThatThrownBy(() -> authProvider.authenticate(loginToken()))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.isAccountLocked()).isFalse();
        verify(ipBlockingService).recordFailedAttempt(any(), eq("/api/v3/auth/login"), eq("alice"), any(),
                eq(AttemptType.LOGIN));
        verify(userService).saveUser(user);
    }

    @Test
    @DisplayName("reaching the attempt limit locks the account with a deadline")
    void reachingTheLimitLocksTheAccount() throws Exception {
        User user = activeUser();
        user.setFailedLoginAttempts(4);
        when(userService.loadUserByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("Right@Pass1", "$2a$10$storedhash")).thenReturn(false);

        assertThatThrownBy(() -> authProvider.authenticate(loginToken()))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.isTemporarilyLocked()).isTrue();
    }

    @Test
    @DisplayName("a bookkeeping failure must not turn a wrong password into a server error")
    void bookkeepingFailureStillReportsBadCredentials() throws Exception {
        User user = activeUser();
        when(userService.loadUserByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("Right@Pass1", "$2a$10$storedhash")).thenReturn(false);
        when(userService.saveUser(any())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> authProvider.authenticate(loginToken()))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("a successful login clears the accumulated failure state and the user's IP records")
    void successResetsFailureState() throws Exception {
        User user = activeUser();
        user.setFailedLoginAttempts(3);
        when(userService.loadUserByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("Right@Pass1", "$2a$10$storedhash")).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("t");

        authProvider.authenticate(loginToken());

        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(ipBlockingService).unblockAllForUser("alice");
        verify(userService).saveUser(user);
    }

    @Test
    @DisplayName("a clean successful login does not write to the database")
    void cleanSuccessDoesNotWrite() throws Exception {
        User user = activeUser();
        when(userService.loadUserByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("Right@Pass1", "$2a$10$storedhash")).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("t");

        authProvider.authenticate(loginToken());

        // No counter to reset, so no pointless UPDATE on the hot login path.
        verify(userService, never()).saveUser(any());
    }

    @Test
    @DisplayName("a bearer authentication passes its credentials through without a password check")
    void bearerTokenSkipsPasswordCheck() {
        User user = activeUser();
        when(userService.loadUserByUsername("alice")).thenReturn(user);

        Authentication result = authProvider.authenticate(
                new BearerAuthenticationToken("alice", "rotated.jwt", true));

        // The already-validated JWT flows through, and no new token is minted.
        assertThat(result.getCredentials()).isEqualTo("rotated.jwt");
        assertThat(result.getPrincipal()).isSameAs(user);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateAccessToken(anyString(), anyString());
    }

    @Test
    @DisplayName("a bearer authentication for a locked account is still refused")
    void bearerTokenForLockedAccountIsRefused() {
        User user = activeUser();
        user.lockAccount(30);
        when(userService.loadUserByUsername("alice")).thenReturn(user);

        assertThatThrownBy(() -> authProvider.authenticate(
                new BearerAuthenticationToken("alice", "jwt", true)))
                .isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("only the two expected authentication types are supported")
    void supportsOnlyTheExpectedTokenTypes() {
        assertThat(authProvider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
        assertThat(authProvider.supports(BearerAuthenticationToken.class)).isTrue();
        assertThat(authProvider.supports(Authentication.class)).isFalse();
    }

    @Test
    @DisplayName("the recorded audit trail names the failing account")
    void auditTrailNamesTheAccount() throws Exception {
        User user = activeUser();
        when(userService.loadUserByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authProvider.authenticate(loginToken()));

        ArgumentCaptor<String> username = ArgumentCaptor.forClass(String.class);
        verify(ipBlockingService).recordFailedAttempt(any(), any(), username.capture(), any(), any());
        assertThat(username.getValue()).isEqualTo("alice");
    }
}
