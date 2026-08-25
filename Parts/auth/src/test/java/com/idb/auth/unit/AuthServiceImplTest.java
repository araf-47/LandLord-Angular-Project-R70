package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.idb.auth.common.constant.OperationStatus;
import com.idb.auth.constant.AttemptType;
import com.idb.auth.dto.request.ForgotPasswordRequest;
import com.idb.auth.dto.request.LoginRequest;
import com.idb.auth.exception.InvalidOtpException;
import com.idb.auth.exception.IpBlockedException;
import com.idb.auth.model.BlockedIp;
import com.idb.auth.model.Role;
import com.idb.auth.model.User;
import com.idb.auth.service.IpBlockingService;
import com.idb.auth.service.OtpService;
import com.idb.auth.service.UserService;
import com.idb.auth.service.impl.AuthServiceImpl;
import com.idb.auth.util.JwtUtil;

/** Login orchestration: password, then second factor, then token issuance. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserService userService;
    @Mock private OtpService otpService;
    @Mock private JwtUtil jwtUtil;
    @Mock private IpBlockingService ipBlockingService;

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 900_000L);
        ReflectionTestUtils.setField(authService, "blockDurationHours", 24);
        ReflectionTestUtils.setField(authService, "maxFailedAttempts", 4);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v3/auth/login");
        request.setRemoteAddr("198.51.100.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private User user(boolean twoFactor) {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("$2a$10$hash");
        user.setActive(true);
        user.setTwoFactorEnabled(twoFactor);
        Role role = new Role();
        role.setName("ADMIN");
        user.setRoles(List.of(role));
        return user;
    }

    private LoginRequest request(String otp) {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("Right@Pass1");
        request.setOtp(otp);
        return request;
    }

    private void authenticatesAs(User user, String accessToken) {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, accessToken, user.getAuthorities()));
    }

    @Test
    @DisplayName("a non-2FA login returns the minted access token plus a fresh refresh token")
    void nonTwoFactorLoginIssuesBothTokens() throws Exception {
        User user = user(false);
        authenticatesAs(user, "minted.access");
        when(jwtUtil.generateRefreshToken("alice", "$2a$10$hash")).thenReturn("minted.refresh");

        var response = authService.login(request(null));

        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(response.getData().getAccessToken()).isEqualTo("minted.access");
        assertThat(response.getData().getRefreshToken()).isEqualTo("minted.refresh");
        assertThat(response.getData().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getData().getExpiresInSeconds()).isEqualTo(900_000L);
    }

    @Test
    @DisplayName("a 2FA account with no OTP gets a challenge and NO token, and an OTP is generated")
    void twoFactorWithoutOtpChallenges() throws Exception {
        authenticatesAs(user(true), "minted.access");

        var response = authService.login(request(null));

        assertThat(response.getStatus()).isEqualTo(OperationStatus.OTP_REQUIRED);
        assertThat(response.getData()).isNull();
        verify(otpService).generateOtp("alice");
        // Critically: no refresh token is minted for a half-completed login.
        verify(jwtUtil, never()).generateRefreshToken(any(), any());
    }

    @Test
    @DisplayName("an empty-string OTP is treated as absent, not as a wrong code")
    void emptyOtpIsTreatedAsAbsent() throws Exception {
        authenticatesAs(user(true), "minted.access");

        assertThat(authService.login(request("")).getStatus()).isEqualTo(OperationStatus.OTP_REQUIRED);
        verify(otpService, never()).validateOtp(any(), any());
    }

    @Test
    @DisplayName("a valid OTP completes the login")
    void validOtpCompletesLogin() throws Exception {
        User user = user(true);
        authenticatesAs(user, "minted.access");
        when(otpService.validateOtp("alice", "123456")).thenReturn(true);
        when(jwtUtil.generateRefreshToken(any(), any())).thenReturn("minted.refresh");

        var response = authService.login(request("123456"));

        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getData().getAccessToken()).isEqualTo("minted.access");
    }

    @Test
    @DisplayName("a wrong OTP throws InvalidOtp and records an INVALID_OTP attempt")
    void wrongOtpIsRecorded() throws Exception {
        authenticatesAs(user(true), "minted.access");
        when(otpService.validateOtp("alice", "000000")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request("000000")))
                .isInstanceOf(InvalidOtpException.class);

        verify(ipBlockingService).recordFailedAttempt(eq("198.51.100.7"), any(), eq("alice"), any(),
                eq(AttemptType.INVALID_OTP));
    }

    @Test
    @DisplayName("a blocked IP is refused before any authentication work")
    void blockedIpShortCircuitsLogin() throws Exception {
        when(ipBlockingService.isIpBlocked("198.51.100.7")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request(null))).isInstanceOf(IpBlockedException.class);

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("a lockout propagates unchanged rather than being wrapped as a generic failure")
    void lockedExceptionPropagates() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));

        assertThatThrownBy(() -> authService.login(request(null))).isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("bad credentials are annotated with the remaining IP budget while one remains")
    void badCredentialsReportsRemainingAttempts() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        when(ipBlockingService.getBlockedIp("198.51.100.7")).thenReturn(BlockedIp.builder()
                .ipAddress("198.51.100.7").endpoint("/e").blockedAt(LocalDateTime.now())
                .failedLoginAttempts(1).active(false).build());

        assertThatThrownBy(() -> authService.login(request(null)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("3 attempts remaining");
    }

    @Test
    @DisplayName("the budget hint is suppressed once the IP is already blocked")
    void noHintOnceBlocked() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        when(ipBlockingService.getBlockedIp("198.51.100.7")).thenReturn(BlockedIp.builder()
                .ipAddress("198.51.100.7").endpoint("/e").blockedAt(LocalDateTime.now())
                .failedLoginAttempts(9).active(true).build());

        assertThatThrownBy(() -> authService.login(request(null)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageNotContaining("attempts remaining");
    }

    @Test
    @DisplayName("with no IP record at all, the original message is preserved")
    void noIpRecordKeepsOriginalMessage() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));
        when(ipBlockingService.getBlockedIp(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login(request(null)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid");
    }

    @Test
    @DisplayName("a valid OTP resets the password and clears an existing lockout")
    void forgotPasswordResetsAndUnlocks() throws Exception {
        User user = user(false);
        user.lockAccount(30);
        when(otpService.validateOtp("alice", "123456")).thenReturn(true);
        when(userService.findByUsername("alice")).thenReturn(user);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("alice");
        request.setPassword("Reset@Pass1");
        request.setOtp("123456");

        var response = authService.forgotPassword(request);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getMessage()).isEqualTo("Password reset successful");
        verify(userService).changePasswordUnchecked("alice", "Reset@Pass1");
        assertThat(user.isAccountLocked()).isFalse();
        verify(userService).saveUser(user);
    }

    @Test
    @DisplayName("a wrong OTP neither resets the password nor reveals whether the user exists")
    void forgotPasswordWithWrongOtp() throws Exception {
        when(otpService.validateOtp("alice", "000000")).thenReturn(false);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("alice");
        request.setPassword("Reset@Pass1");
        request.setOtp("000000");

        var response = authService.forgotPassword(request);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.ERROR);
        assertThat(response.getMessage()).isEqualTo("Invalid otp");
        verify(userService, never()).changePasswordUnchecked(any(), any());
        verify(ipBlockingService).recordFailedAttempt(any(), any(), eq("alice"), any(),
                eq(AttemptType.INVALID_OTP));
    }

    @Test
    @DisplayName("a blocked IP cannot use the password-reset path either")
    void forgotPasswordRespectsIpBlock() throws Exception {
        when(ipBlockingService.isIpBlocked("198.51.100.7")).thenReturn(true);
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("alice");

        assertThatThrownBy(() -> authService.forgotPassword(request)).isInstanceOf(IpBlockedException.class);
        verify(otpService, never()).validateOtp(any(), any());
    }

    @Test
    @DisplayName("resetting a password for an unknown user does not disclose that fact")
    void forgotPasswordForUnknownUser() throws Exception {
        when(otpService.validateOtp("ghost", "123456")).thenReturn(true);
        when(userService.findByUsername("ghost"))
                .thenThrow(com.idb.auth.common.exception.LogOnlyException.of(null, "User not found"));

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("ghost");
        request.setPassword("Reset@Pass1");
        request.setOtp("123456");

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(com.idb.auth.common.exception.TraceableException.class)
                .satisfies(e -> assertThat(
                        ((com.idb.auth.common.exception.TraceableException) e).getResponse().getMessage())
                        .isEqualTo("Invalid username"));
    }
}
