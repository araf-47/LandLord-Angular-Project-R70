package com.idb.auth.it;

import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_LOGIN;
import static com.idb.auth.constant.AuthConstants.URL_AUTH_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_TEST_CONTROLLER;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * The core login-then-use-token journey, plus the rejection paths around it.
 */
class AuthFlowIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Auth@Pass123";

    @BeforeEach
    void setUp() {
        truncateAll();
    }

    @Test
    @DisplayName("login with valid credentials returns an access token, a refresh token and the expiry")
    void loginReturnsTokenPair() {
        createUser("flow_admin", PASSWORD, "ADMIN");

        HttpResponse<String> res = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", "flow_admin", "password", PASSWORD, "otp", ""));

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        assertThat(messageOf(res)).isEqualTo("Login successful");
        assertThat(dataOf(res).get("accessToken").asText()).isNotBlank();
        assertThat(dataOf(res).get("refreshToken").asText()).isNotBlank();
        assertThat(dataOf(res).get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(dataOf(res).get("expiresInSeconds").asLong()).isPositive();
    }

    @Test
    @DisplayName("the access token authenticates a protected endpoint and carries the user's authorities")
    void accessTokenAuthenticatesProtectedEndpoint() {
        createUser("flow_user", PASSWORD, "ADMIN");
        String token = login("flow_user", PASSWORD);

        HttpResponse<String> res = getJson(URL_TEST_CONTROLLER + "/any", token);

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        assertThat(dataOf(res).get("username").asText()).isEqualTo("flow_user");
        assertThat(dataOf(res).get("authorities").get(0).asText()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("a protected endpoint with no Authorization header is rejected as ACCESS_DENIED")
    void protectedEndpointWithoutTokenIsRejected() {
        HttpResponse<String> res = getJson(URL_TEST_CONTROLLER + "/any");

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("a structurally invalid bearer token is rejected and the cached token headers are blanked")
    void garbageTokenIsRejected() {
        HttpResponse<String> res = getJson(URL_TEST_CONTROLLER + "/any", "not-a-jwt");

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isEqualTo("ACCESS_DENIED");
        // The entry point blanks these so a client stops replaying a bad token.
        assertThat(header(res, "x-access-token")).contains("");
        assertThat(header(res, "x-refresh-token")).contains("");
    }

    @Test
    @DisplayName("a well-formed token signed with the wrong key is rejected")
    void tokenSignedWithWrongKeyIsRejected() {
        createUser("flow_victim", PASSWORD, "ADMIN");
        createUser("flow_attacker", PASSWORD, "ADMIN");

        // Correct subject, but signed with a different user's password hash. Tokens
        // are keyed per-user, so this must not authenticate.
        String forged = signedTokenForKeyOf("flow_victim", "flow_attacker");

        HttpResponse<String> res = getJson(URL_TEST_CONTROLLER + "/any", forged);

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isIn("ACCESS_DENIED", "INVALID_TOKEN_IN_HEADER", "SESSION_EXPIRED");
    }

    @Test
    @DisplayName("a wrong password is reported as BAD_CREDENTIALS")
    void wrongPasswordIsRejected() {
        createUser("flow_wrongpass", PASSWORD, "ADMIN");

        HttpResponse<String> res = loginResponse("flow_wrongpass", "Wrong@Pass999");

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isEqualTo("BAD_CREDENTIALS");
        assertThat(messageOf(res)).contains("Invalid username or password");
    }

    @Test
    @DisplayName("an unknown username is indistinguishable from a wrong password")
    void unknownUsernameDoesNotLeakExistence() {
        createUser("flow_known", PASSWORD, "ADMIN");

        HttpResponse<String> unknown = loginResponse("flow_nosuchuser", PASSWORD);
        HttpResponse<String> wrongPassword = loginResponse("flow_known", "Wrong@Pass999");

        // Same status and same message: no username enumeration oracle.
        assertThat(statusOf(unknown)).isEqualTo(statusOf(wrongPassword)).isEqualTo("BAD_CREDENTIALS");
        assertThat(messageOf(unknown)).isEqualTo(messageOf(wrongPassword));
    }

    @Test
    @DisplayName("a login body missing required fields fails bean validation, not authentication")
    void missingFieldsAreAValidationError() {
        HttpResponse<String> res = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN, Map.of("username", "flow_admin"));

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(statusOf(res)).isEqualTo("VALIDATION_ERROR");
        assertThat(dataOf(res).get("password").asText()).isEqualTo("Password is required");
    }

    @Test
    @DisplayName("an inactive (soft-deleted) user cannot log in")
    void inactiveUserCannotLogIn() {
        createUser("flow_inactive", PASSWORD, "ADMIN");
        jdbc.update("UPDATE users SET is_active = false WHERE username = ?", "flow_inactive");

        HttpResponse<String> res = loginResponse("flow_inactive", PASSWORD);

        assertThat(res.statusCode()).isEqualTo(401);
        // findByUsername filters on is_active, so the user simply does not exist as
        // far as authentication is concerned.
        assertThat(statusOf(res)).isEqualTo("BAD_CREDENTIALS");
    }

    /** Signs a token whose subject is {@code subject} but whose key belongs to {@code keyOwner}. */
    private String signedTokenForKeyOf(String subject, String keyOwner) {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                storedPasswordHash(keyOwner).getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(wrongKey)
                .compact();
    }
}
