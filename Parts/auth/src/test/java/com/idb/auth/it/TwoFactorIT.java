package com.idb.auth.it;

import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_GENERATE_OTP;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_LOGIN;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_TOGGLE_2FA;
import static com.idb.auth.constant.AuthConstants.URL_AUTH_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_TEST_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_USER_CONTROLLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.idb.auth.common.dto.MailInfo;
import com.idb.auth.common.exception.TraceableException;

/**
 * Two-factor login. The OTP is only ever stored BCrypt-hashed in the cache, so
 * the mocked {@code MailService} is the only place a test can observe the
 * plaintext - which is also what proves delivery actually happened.
 */
class TwoFactorIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "TwoFa@Pass123";

    @BeforeEach
    void setUp() {
        truncateAll();
    }

    @Test
    @DisplayName("a 2FA account with correct credentials but no OTP gets a challenge, not a token")
    void loginWithoutOtpReturnsChallenge() throws TraceableException {
        createUser("mfa_challenge", PASSWORD, "ADMIN", true);

        HttpResponse<String> res = loginResponse("mfa_challenge", PASSWORD);

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(statusOf(res)).isEqualTo("OTP_REQUIRED");
        assertThat(messageOf(res)).isEqualTo("OTP required");
        // No token is handed out at the challenge stage.
        assertThat(body(res).get("data")).isNull();

        // The challenge is only useful if the code was actually delivered.
        MailInfo mail = captureMail();
        assertThat(mail.getTo()).containsExactly("mfa_challenge@test.local");
        assertThat(mail.getTemplateName()).isEqualTo("otp");
        assertThat(mail.getTemplateModel().get("otp").toString()).matches("\\d{6}");
        assertThat(mail.getTemplateModel()).containsEntry("expiryMinutes", 5);
    }

    @Test
    @DisplayName("the challenge OTP completes the login and yields a working token")
    void loginWithValidOtpSucceeds() throws TraceableException {
        createUser("mfa_success", PASSWORD, "ADMIN", true);

        loginResponse("mfa_success", PASSWORD);
        String otp = captureMail().getTemplateModel().get("otp").toString();

        HttpResponse<String> res = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", "mfa_success", "password", PASSWORD, "otp", otp));

        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        String accessToken = dataOf(res).get("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/any", accessToken))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("a wrong OTP is rejected as INVALID_OTP even with a correct password")
    void loginWithWrongOtpIsRejected() throws TraceableException {
        createUser("mfa_wrongotp", PASSWORD, "ADMIN", true);

        loginResponse("mfa_wrongotp", PASSWORD);
        String realOtp = captureMail().getTemplateModel().get("otp").toString();
        String wrongOtp = realOtp.equals("000000") ? "111111" : "000000";

        HttpResponse<String> res = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", "mfa_wrongotp", "password", PASSWORD, "otp", wrongOtp));

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isEqualTo("INVALID_OTP");
        assertThat(body(res).get("data")).isNull();
    }

    @Test
    @DisplayName("an OTP is single-use - replaying it fails")
    void otpIsSingleUse() throws TraceableException {
        createUser("mfa_replay", PASSWORD, "ADMIN", true);

        loginResponse("mfa_replay", PASSWORD);
        String otp = captureMail().getTemplateModel().get("otp").toString();

        Map<String, Object> loginWithOtp = Map.of("username", "mfa_replay", "password", PASSWORD, "otp", otp);
        assertThat(statusOf(postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN, loginWithOtp))).isEqualTo("SUCCESS");

        // A successful validation evicts the cached OTP, so the second use finds
        // nothing to match against.
        HttpResponse<String> replay = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN, loginWithOtp);
        assertThat(statusOf(replay)).isEqualTo("INVALID_OTP");
    }

    @Test
    @DisplayName("an OTP issued for one account cannot complete another account's login")
    void otpIsBoundToItsAccount() throws TraceableException {
        createUser("mfa_owner", PASSWORD, "ADMIN", true);
        createUser("mfa_other", PASSWORD, "ADMIN", true);

        loginResponse("mfa_owner", PASSWORD);
        String ownerOtp = captureMail().getTemplateModel().get("otp").toString();

        HttpResponse<String> res = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", "mfa_other", "password", PASSWORD, "otp", ownerOtp));

        assertThat(statusOf(res)).isEqualTo("INVALID_OTP");
    }

    @Test
    @DisplayName("OTP validation is rate limited - a fourth wrong attempt cannot be rescued by the right code")
    void otpValidationIsRateLimited() throws TraceableException {
        createUser("mfa_ratelimit", PASSWORD, "ADMIN", true);

        loginResponse("mfa_ratelimit", PASSWORD);
        String realOtp = captureMail().getTemplateModel().get("otp").toString();
        String wrongOtp = realOtp.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 3; i++) {
            assertThat(statusOf(postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                    Map.of("username", "mfa_ratelimit", "password", PASSWORD, "otp", wrongOtp))))
                    .isEqualTo("INVALID_OTP");
        }

        // The attempt counter is now exhausted, so even the genuine code is refused.
        assertThat(statusOf(postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", "mfa_ratelimit", "password", PASSWORD, "otp", realOtp))))
                .isEqualTo("INVALID_OTP");
    }

    @Test
    @DisplayName("OTP generation is rate limited to three requests")
    void otpGenerationIsRateLimited() {
        createUser("mfa_genlimit", PASSWORD, "ADMIN");

        for (int i = 0; i < 3; i++) {
            assertThat(statusOf(postJson(URL_AUTH_CONTROLLER + ENDPOINT_GENERATE_OTP,
                    Map.of("id", "mfa_genlimit")))).isEqualTo("SUCCESS");
        }

        HttpResponse<String> fourth = postJson(URL_AUTH_CONTROLLER + ENDPOINT_GENERATE_OTP,
                Map.of("id", "mfa_genlimit"));
        assertThat(statusOf(fourth)).isEqualTo("ERROR");
        assertThat(messageOf(fourth)).contains("Too many OTP requests");
    }

    @Test
    @DisplayName("OTP generation for an unknown user reports a generic error and sends no mail")
    void otpForUnknownUserSendsNoMail() {
        HttpResponse<String> res = postJson(URL_AUTH_CONTROLLER + ENDPOINT_GENERATE_OTP,
                Map.of("id", "mfa_nosuchuser"));

        assertThat(statusOf(res)).isEqualTo("ERROR");
        assertThat(messageOf(res)).isEqualTo("User not found");
        org.mockito.Mockito.verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("toggling 2FA on makes the next login require an OTP")
    void togglingTwoFactorOnTakesEffect() {
        createUser("mfa_toggle", PASSWORD, "ADMIN");

        // 2FA off: a plain login succeeds.
        String token = login("mfa_toggle", PASSWORD);

        assertThat(statusOf(postJson(URL_USER_CONTROLLER + ENDPOINT_TOGGLE_2FA, Map.of("id", true), token)))
                .isEqualTo("SUCCESS");

        assertThat(statusOf(loginResponse("mfa_toggle", PASSWORD))).isEqualTo("OTP_REQUIRED");
    }

    /** Reads the plaintext OTP out of the most recent (mocked) mail send. */
    private MailInfo captureMail() throws TraceableException {
        ArgumentCaptor<MailInfo> captor = ArgumentCaptor.forClass(MailInfo.class);
        verify(mailService, atLeastOnce()).sendTemplatedEmail(captor.capture());
        return captor.getValue();
    }
}
