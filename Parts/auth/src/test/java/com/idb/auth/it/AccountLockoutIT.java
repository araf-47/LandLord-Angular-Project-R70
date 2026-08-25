package com.idb.auth.it;

import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_GENERATE_OTP;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_LIST;
import static com.idb.auth.constant.AuthConstants.URL_AUTH_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_USER_BLOCK_CONTROLLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.idb.auth.common.dto.MailInfo;
import com.idb.auth.common.exception.TraceableException;

/**
 * Per-account lockout after repeated wrong passwords, and the two ways out of
 * it: an administrative unblock, and the self-service password reset.
 *
 * <p>{@code security.account-lockout.max-attempts=5} in the test profile.
 */
class AccountLockoutIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Lock@Pass123";
    private static final String WRONG_PASSWORD = "Wrong@Pass999";
    private static final int MAX_ATTEMPTS = 5;

    @BeforeEach
    void setUp() {
        truncateAll();
    }

    @Test
    @DisplayName("the failed-attempt counter increments per wrong password and the account survives below the limit")
    void failedAttemptsAccumulateBelowTheLimit() {
        createUser("lock_counting", PASSWORD, "ADMIN");

        for (int i = 1; i < MAX_ATTEMPTS; i++) {
            assertThat(statusOf(loginResponse("lock_counting", WRONG_PASSWORD))).isEqualTo("BAD_CREDENTIALS");
            assertThat(failedAttempts("lock_counting")).isEqualTo(i);
            assertThat(isLocked("lock_counting")).isFalse();
        }

        // Still usable: the limit was approached but not reached.
        assertThat(statusOf(loginResponse("lock_counting", PASSWORD))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("the fifth wrong password locks the account, and the correct password is then refused")
    void accountLocksAtTheLimit() {
        createUser("lock_locked", PASSWORD, "ADMIN");

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginResponse("lock_locked", WRONG_PASSWORD);
        }

        assertThat(isLocked("lock_locked")).isTrue();
        assertThat(lockedUntil("lock_locked")).isAfter(LocalDateTime.now());

        HttpResponse<String> res = loginResponse("lock_locked", PASSWORD);
        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isEqualTo("ACCOUNT_LOCKED");
        assertThat(messageOf(res)).contains("temporarily locked");
    }

    @Test
    @DisplayName("a successful login before the limit resets the counter")
    void successfulLoginResetsTheCounter() {
        createUser("lock_reset", PASSWORD, "ADMIN");

        loginResponse("lock_reset", WRONG_PASSWORD);
        loginResponse("lock_reset", WRONG_PASSWORD);
        assertThat(failedAttempts("lock_reset")).isEqualTo(2);

        assertThat(statusOf(loginResponse("lock_reset", PASSWORD))).isEqualTo("SUCCESS");
        assertThat(failedAttempts("lock_reset")).isZero();
    }

    @Test
    @DisplayName("an elapsed lock window lets the account back in without an explicit unlock")
    void lockExpiresOnItsOwn() {
        createUser("lock_expiring", PASSWORD, "ADMIN");

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginResponse("lock_expiring", WRONG_PASSWORD);
        }
        assertThat(statusOf(loginResponse("lock_expiring", PASSWORD))).isEqualTo("ACCOUNT_LOCKED");

        // Move the window into the past. isTemporarilyLocked() checks the deadline,
        // not just the flag, so no scheduled job is needed to clear a lock.
        jdbc.update("UPDATE users SET locked_until = ? WHERE username = ?",
                LocalDateTime.now().minusMinutes(1), "lock_expiring");
        cacheManager.getCache("user").evict("lock_expiring");

        assertThat(statusOf(loginResponse("lock_expiring", PASSWORD))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("a locked account is listed by user-block/list and can be unblocked by an admin")
    void adminCanListAndUnblockLockedAccounts() {
        createUser("lock_admin", PASSWORD, "ADMIN");
        createUser("lock_victim", PASSWORD, "USER");

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginResponse("lock_victim", WRONG_PASSWORD);
        }

        String adminToken = login("lock_admin", PASSWORD);

        HttpResponse<String> list = getJson(URL_USER_BLOCK_CONTROLLER + ENDPOINT_LIST, adminToken);
        assertThat(statusOf(list)).isEqualTo("SUCCESS");
        assertThat(dataOf(list).valueStream().map(n -> n.get("username").asText()).toList())
                .contains("lock_victim");

        HttpResponse<String> unblock = postJson(URL_USER_BLOCK_CONTROLLER + "/unblock",
                Map.of("id", "lock_victim"), adminToken);
        assertThat(statusOf(unblock)).isEqualTo("SUCCESS");
        assertThat(messageOf(unblock)).contains("unblocked successfully");

        assertThat(statusOf(loginResponse("lock_victim", PASSWORD))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("forgot-password with a valid OTP resets the password and clears the lock")
    void passwordResetUnlocksTheAccount() throws TraceableException {
        createUser("lock_selfserve", PASSWORD, "ADMIN");

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginResponse("lock_selfserve", WRONG_PASSWORD);
        }
        assertThat(isLocked("lock_selfserve")).isTrue();

        postJson(URL_AUTH_CONTROLLER + ENDPOINT_GENERATE_OTP, Map.of("id", "lock_selfserve"));
        String otp = captureOtp();

        String newPassword = "Fresh@Pass456";
        HttpResponse<String> reset = postJson(URL_AUTH_CONTROLLER + "/forgot-password",
                Map.of("username", "lock_selfserve", "password", newPassword, "otp", otp));

        assertThat(statusOf(reset)).isEqualTo("SUCCESS");
        assertThat(messageOf(reset)).isEqualTo("Password reset successful");
        assertThat(isLocked("lock_selfserve")).isFalse();

        // The old password is gone and the new one works on a no-longer-locked account.
        assertThat(statusOf(loginResponse("lock_selfserve", PASSWORD))).isEqualTo("BAD_CREDENTIALS");
        assertThat(statusOf(loginResponse("lock_selfserve", newPassword))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("forgot-password with a wrong OTP neither resets the password nor unlocks the account")
    void passwordResetWithWrongOtpIsRefused() throws TraceableException {
        createUser("lock_badotp", PASSWORD, "ADMIN");

        postJson(URL_AUTH_CONTROLLER + ENDPOINT_GENERATE_OTP, Map.of("id", "lock_badotp"));
        String realOtp = captureOtp();
        String wrongOtp = realOtp.equals("000000") ? "111111" : "000000";

        HttpResponse<String> reset = postJson(URL_AUTH_CONTROLLER + "/forgot-password",
                Map.of("username", "lock_badotp", "password", "Fresh@Pass456", "otp", wrongOtp));

        assertThat(statusOf(reset)).isEqualTo("ERROR");
        assertThat(messageOf(reset)).isEqualTo("Invalid otp");
        assertThat(statusOf(loginResponse("lock_badotp", PASSWORD))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("forgot-password rejects a password that fails the strength policy before any OTP check")
    void passwordResetEnforcesPasswordPolicy() {
        createUser("lock_weakpw", PASSWORD, "ADMIN");

        HttpResponse<String> res = postJson(URL_AUTH_CONTROLLER + "/forgot-password",
                Map.of("username", "lock_weakpw", "password", "weak", "otp", "123456"));

        assertThat(statusOf(res)).isEqualTo("VALIDATION_ERROR");
        assertThat(dataOf(res).get("password").asText()).contains("8 and 16 characters");
    }

    private String captureOtp() throws TraceableException {
        ArgumentCaptor<MailInfo> captor = ArgumentCaptor.forClass(MailInfo.class);
        verify(mailService, atLeastOnce()).sendTemplatedEmail(captor.capture());
        return captor.getValue().getTemplateModel().get("otp").toString();
    }

    private int failedAttempts(String username) {
        return jdbc.queryForObject("SELECT failed_login_attempts FROM users WHERE username = ?",
                Integer.class, username);
    }

    private boolean isLocked(String username) {
        return jdbc.queryForObject("SELECT account_locked FROM users WHERE username = ?", Boolean.class, username);
    }

    private LocalDateTime lockedUntil(String username) {
        return jdbc.queryForObject("SELECT locked_until FROM users WHERE username = ?",
                LocalDateTime.class, username);
    }
}
