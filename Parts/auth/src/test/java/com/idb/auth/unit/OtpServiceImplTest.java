package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.idb.auth.common.dto.MailInfo;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.common.service.MailService;
import com.idb.auth.dao.UserRepository;
import com.idb.auth.model.User;
import com.idb.auth.service.impl.OtpServiceImpl;

/**
 * OTP generation, validation and the two rate limits. A real Caffeine cache
 * manager is used rather than a mock, because the counters are ordinary
 * read-modify-write cache operations and mocking them would test nothing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpServiceImplTest {

    @Mock private MailService mailService;
    @Mock private UserRepository userRepository;

    private CacheManager cacheManager;
    private PasswordEncoder passwordEncoder;
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        CaffeineCacheManager manager = new CaffeineCacheManager("otp", "otp-attempts", "otp-resend-cooldown");
        cacheManager = manager;
        passwordEncoder = new BCryptPasswordEncoder();
        otpService = new OtpServiceImpl(cacheManager, passwordEncoder, mailService, userRepository,
                new MockEnvironment());

        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    }

    private String generateAndCapture() throws Exception {
        otpService.generateOtp("alice");
        return capturedOtp();
    }

    private String capturedOtp() throws TraceableException {
        ArgumentCaptor<MailInfo> captor = ArgumentCaptor.forClass(MailInfo.class);
        verify(mailService, org.mockito.Mockito.atLeastOnce()).sendTemplatedEmail(captor.capture());
        return captor.getValue().getTemplateModel().get("otp").toString();
    }

    /**
     * Tests that fire several {@code generateOtp} calls back-to-back are
     * exercising the attempt-cap, not the resend cooldown - evict the
     * cooldown entry between calls so it doesn't shadow what's under test.
     */
    private void evictResendCooldown(String username) {
        cacheManager.getCache("otp-resend-cooldown").evict(username);
    }

    @Test
    @DisplayName("generation mails a six-digit code and stores it hashed, never in plaintext")
    void generationStoresOnlyAHash() throws Exception {
        String otp = generateAndCapture();

        assertThat(otp).matches("\\d{6}");
        String stored = cacheManager.getCache("otp").get("alice", String.class);
        assertThat(stored).isNotNull().isNotEqualTo(otp).startsWith("$2");
        assertThat(passwordEncoder.matches(otp, stored)).isTrue();
    }

    @Test
    @DisplayName("the mail carries the recipient, template and expiry the notification needs")
    void mailPayloadShape() throws Exception {
        otpService.generateOtp("alice");

        ArgumentCaptor<MailInfo> captor = ArgumentCaptor.forClass(MailInfo.class);
        verify(mailService).sendTemplatedEmail(captor.capture());
        MailInfo mail = captor.getValue();

        assertThat(mail.getTo()).containsExactly("alice@example.com");
        assertThat(mail.getTemplateName()).isEqualTo("otp");
        assertThat(mail.getSubject()).contains("alice");
        assertThat(mail.getTemplateModel()).containsEntry("expiryMinutes", 5).containsKey("otp");
    }

    @Test
    @DisplayName("the correct code validates once, and the second use finds nothing")
    void otpIsSingleUse() throws Exception {
        String otp = generateAndCapture();

        assertThat(otpService.validateOtp("alice", otp)).isTrue();
        assertThat(cacheManager.getCache("otp").get("alice")).isNull();
        assertThat(otpService.validateOtp("alice", otp)).isFalse();
    }

    @Test
    @DisplayName("a wrong code does not consume the stored OTP")
    void wrongCodeDoesNotConsume() throws Exception {
        String otp = generateAndCapture();

        assertThat(otpService.validateOtp("alice", "000000")).isFalse();
        assertThat(cacheManager.getCache("otp").get("alice")).isNotNull();
        // The genuine code still works afterwards, within the attempt budget.
        assertThat(otpService.validateOtp("alice", otp)).isTrue();
    }

    @Test
    @DisplayName("validation with no OTP on file fails instead of throwing")
    void validateWithNothingStored() throws Exception {
        assertThat(otpService.validateOtp("alice", "123456")).isFalse();
    }

    @Test
    @DisplayName("a blank code is rejected without consuming an attempt")
    void blankCodeIsRejectedCheaply() throws Exception {
        String otp = generateAndCapture();

        assertThat(otpService.validateOtp("alice", null)).isFalse();
        assertThat(otpService.validateOtp("alice", "")).isFalse();
        assertThat(otpService.validateOtp("alice", "   ")).isFalse();
        // Budget untouched, so the real code still validates.
        assertThat(otpService.validateOtp("alice", otp)).isTrue();
    }

    @Test
    @DisplayName("validation is capped at three attempts, after which even the right code fails")
    void validationAttemptsAreCapped() throws Exception {
        String otp = generateAndCapture();

        for (int i = 0; i < 3; i++) {
            assertThat(otpService.validateOtp("alice", "000000")).isFalse();
        }
        // The counter is exhausted. This is the limit the reference never actually
        // enforced, because its @CachePut helpers were self-invoked and so bypassed
        // the Spring proxy entirely.
        assertThat(otpService.validateOtp("alice", otp)).isFalse();
    }

    @Test
    @DisplayName("a second generation request right after the first is rejected by the resend cooldown")
    void resendCooldownRejectsAnImmediateRetry() throws Exception {
        assertThat(otpService.generateOtp("alice").getMessage()).contains("OTP generated successfully");

        assertThatThrownBy(() -> otpService.generateOtp("alice"))
                .isInstanceOf(TraceableException.class)
                .satisfies(e -> assertThat(((TraceableException) e).getResponse().getMessage())
                        .contains("Please wait before requesting another OTP"));

        // Only the first call's mail actually went out.
        verify(mailService, org.mockito.Mockito.times(1)).sendTemplatedEmail(any());
    }

    @Test
    @DisplayName("once the cooldown is cleared, generation works again")
    void generationWorksAgainAfterCooldownClears() throws Exception {
        otpService.generateOtp("alice");
        evictResendCooldown("alice");

        assertThat(otpService.generateOtp("alice").getMessage()).contains("OTP generated successfully");
    }

    @Test
    @DisplayName("generation is capped at three requests per window")
    void generationAttemptsAreCapped() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(otpService.generateOtp("alice").getMessage()).contains("OTP generated successfully");
            evictResendCooldown("alice");
        }

        assertThatThrownBy(() -> otpService.generateOtp("alice"))
                .isInstanceOf(TraceableException.class)
                .satisfies(e -> assertThat(((TraceableException) e).getResponse().getMessage())
                        .contains("Too many OTP requests"));
    }

    @Test
    @DisplayName("clearing the cache resets the OTP and BOTH prefixed attempt counters")
    void clearCacheResetsEverything() throws Exception {
        generateAndCapture();
        otpService.validateOtp("alice", "000000");

        assertThat(cacheManager.getCache("otp-attempts").get("otp_gen_alice")).isNotNull();
        assertThat(cacheManager.getCache("otp-attempts").get("otp_val_alice")).isNotNull();

        otpService.clearCache("alice");

        // Evicting by bare username - as the reference did - would leave both of
        // these behind, permanently wedging the user at their attempt limit.
        assertThat(cacheManager.getCache("otp").get("alice")).isNull();
        assertThat(cacheManager.getCache("otp-attempts").get("otp_gen_alice")).isNull();
        assertThat(cacheManager.getCache("otp-attempts").get("otp_val_alice")).isNull();
    }

    @Test
    @DisplayName("after a clear, generation works again")
    void clearRestoresTheGenerationBudget() throws Exception {
        for (int i = 0; i < 3; i++) {
            otpService.generateOtp("alice");
            evictResendCooldown("alice");
        }
        assertThatThrownBy(() -> otpService.generateOtp("alice")).isInstanceOf(TraceableException.class);

        otpService.clearCache("alice");

        assertThat(otpService.generateOtp("alice").getMessage()).contains("OTP generated successfully");
    }

    @Test
    @DisplayName("counters are per-user, so one account cannot exhaust another's budget")
    void countersAreScopedPerUser() throws Exception {
        User bob = new User();
        bob.setUsername("bob");
        bob.setEmail("bob@example.com");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        for (int i = 0; i < 3; i++) {
            otpService.generateOtp("alice");
            evictResendCooldown("alice");
        }
        assertThatThrownBy(() -> otpService.generateOtp("alice")).isInstanceOf(TraceableException.class);

        assertThat(otpService.generateOtp("bob").getMessage()).contains("OTP generated successfully");
    }

    @Test
    @DisplayName("an unknown user gets no mail and a generic not-found error")
    void unknownUserSendsNoMail() throws Exception {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.generateOtp("ghost"))
                .isInstanceOf(LogOnlyException.class)
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("User not found"));

        verify(mailService, never()).sendTemplatedEmail(any());
    }

    @Test
    @DisplayName("a user with no email address cannot be sent an OTP")
    void userWithoutEmailIsRejected() throws Exception {
        User noEmail = new User();
        noEmail.setUsername("noemail");
        when(userRepository.findByUsername("noemail")).thenReturn(Optional.of(noEmail));

        assertThatThrownBy(() -> otpService.generateOtp("noemail"))
                .isInstanceOf(LogOnlyException.class)
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("User email not found"));

        verify(mailService, never()).sendTemplatedEmail(any());
    }

    @Test
    @DisplayName("a missing cache fails loudly instead of silently disabling OTP")
    void missingCacheFailsLoudly() throws Exception {
        OtpServiceImpl noCache = new OtpServiceImpl(new CaffeineCacheManager() {
            @Override
            public org.springframework.cache.Cache getCache(String name) {
                return null;
            }
        }, passwordEncoder, mailService, userRepository, new MockEnvironment());

        assertThatThrownBy(() -> noCache.generateOtp("alice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    @DisplayName("generated codes are drawn from the full six-digit space")
    void generatedCodesVary() {
        assertThat(java.util.stream.Stream.generate(() -> otpService.generateSecureRandomOtp())
                .limit(50)
                .peek(otp -> assertThat(otp).matches("\\d{6}"))
                .distinct()
                .count()).isGreaterThan(40);
    }
}
