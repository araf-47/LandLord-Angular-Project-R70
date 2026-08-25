package com.idb.auth.it;

import static com.idb.auth.common.constant.CommonConstants.ACCESS_TOKEN_HEADER;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_LOGOUT_ALL;
import static com.idb.auth.constant.AuthConstants.URL_TEST_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_USER_CONTROLLER;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

/**
 * Token expiry, silent refresh, and the two ways a token stops being valid
 * before its own expiry: an explicit revoke, and a password change.
 */
class TokenLifecycleIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Token@Pass123";
    private static final String PROBE = URL_TEST_CONTROLLER + "/any";

    @BeforeEach
    void setUp() {
        truncateAll();
    }

    @Test
    @DisplayName("an expired access token plus a valid refresh token is silently rotated")
    void expiredAccessTokenIsRotatedFromRefreshToken() {
        createUser("tok_refresh", PASSWORD, "ADMIN");
        JsonNode data = loginData("tok_refresh", PASSWORD);
        String refreshToken = data.get("refreshToken").asText();
        String expiredAccessToken = expiredTokenFor("tok_refresh");

        HttpResponse<String> res = getJson(PROBE, expiredAccessToken, refreshToken);

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        assertThat(dataOf(res).get("username").asText()).isEqualTo("tok_refresh");

        // The rotation is advertised through the response header, which is the only
        // way a client learns it should replace its stored access token.
        String rotated = header(res, ACCESS_TOKEN_HEADER)
                .orElseThrow(() -> new AssertionError("no " + ACCESS_TOKEN_HEADER + " header on rotation"));
        assertThat(rotated).isNotBlank().isNotEqualTo(expiredAccessToken);

        // And the rotated token has to actually work on its own.
        assertThat(statusOf(getJson(PROBE, rotated))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("a still-valid access token is not rotated - no x-access-token header is emitted")
    void validAccessTokenIsNotRotated() {
        createUser("tok_valid", PASSWORD, "ADMIN");
        String token = login("tok_valid", PASSWORD);

        HttpResponse<String> res = getJson(PROBE, token);

        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        assertThat(header(res, ACCESS_TOKEN_HEADER)).isEmpty();
    }

    @Test
    @DisplayName("an expired access token with no refresh token is SESSION_EXPIRED")
    void expiredAccessTokenWithoutRefreshIsRejected() {
        createUser("tok_expired", PASSWORD, "ADMIN");
        String expiredAccessToken = expiredTokenFor("tok_expired");

        HttpResponse<String> res = getJson(PROBE, expiredAccessToken);

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isEqualTo("SESSION_EXPIRED");
        assertThat(messageOf(res)).contains("Session expired");
    }

    @Test
    @DisplayName("an expired access token with an equally expired refresh token is SESSION_EXPIRED")
    void expiredRefreshTokenIsRejected() {
        createUser("tok_bothexpired", PASSWORD, "ADMIN");
        String expiredAccess = expiredTokenFor("tok_bothexpired");
        String expiredRefresh = expiredTokenFor("tok_bothexpired");

        HttpResponse<String> res = getJson(PROBE, expiredAccess, expiredRefresh);

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isEqualTo("SESSION_EXPIRED");
    }

    @Test
    @DisplayName("logout-all revokes the caller's existing tokens, including the refresh token")
    void logoutAllRevokesExistingTokens() {
        createUser("tok_logout", PASSWORD, "ADMIN");
        JsonNode data = loginData("tok_logout", PASSWORD);
        String accessToken = data.get("accessToken").asText();
        String refreshToken = data.get("refreshToken").asText();

        assertThat(statusOf(getJson(PROBE, accessToken))).isEqualTo("SUCCESS");

        awaitNextSecond();
        HttpResponse<String> logout = postJson(URL_USER_CONTROLLER + ENDPOINT_LOGOUT_ALL, Map.of(), accessToken);
        assertThat(statusOf(logout)).isEqualTo("SUCCESS");
        assertThat(messageOf(logout)).contains("All active sessions have been logged out");

        // The token's signature is still valid and it has not expired - it is the
        // tokens_valid_after watermark that refuses it.
        HttpResponse<String> afterLogout = getJson(PROBE, accessToken);
        assertThat(afterLogout.statusCode()).isEqualTo(401);
        assertThat(statusOf(afterLogout)).isEqualTo("SESSION_EXPIRED");
        assertThat(messageOf(afterLogout)).contains("revoked");

        // The refresh token was issued before the watermark too, so it cannot be
        // used to mint a replacement.
        assertThat(statusOf(getJson(PROBE, expiredTokenFor("tok_logout"), refreshToken)))
                .isEqualTo("SESSION_EXPIRED");

        // A fresh login still works: the watermark bounds the past, not the future.
        awaitNextSecond();
        assertThat(statusOf(getJson(PROBE, login("tok_logout", PASSWORD)))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("logout-all affects only the revoking user")
    void logoutAllIsScopedToOneUser() {
        createUser("tok_selfrevoke", PASSWORD, "ADMIN");
        createUser("tok_bystander", PASSWORD, "ADMIN");

        String revoking = login("tok_selfrevoke", PASSWORD);
        String bystander = login("tok_bystander", PASSWORD);

        awaitNextSecond();
        postJson(URL_USER_CONTROLLER + ENDPOINT_LOGOUT_ALL, Map.of(), revoking);

        assertThat(statusOf(getJson(PROBE, revoking))).isEqualTo("SESSION_EXPIRED");
        assertThat(statusOf(getJson(PROBE, bystander))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("changing the password invalidates every token signed with the old hash")
    void passwordChangeInvalidatesOldTokens() {
        createUser("tok_pwchange", PASSWORD, "ADMIN");
        String token = login("tok_pwchange", PASSWORD);
        assertThat(statusOf(getJson(PROBE, token))).isEqualTo("SUCCESS");

        // Rewrite the hash out of band - equivalent to any password change. Tokens
        // are HMAC-signed with the password hash, so the old signature no longer
        // verifies; no blacklist is involved.
        jdbc.update("UPDATE users SET password = ? WHERE username = ?",
                passwordEncoder.encode("Rotated@Pass456"), "tok_pwchange");
        cacheManager.getCache("user").evict("tok_pwchange");

        HttpResponse<String> res = getJson(PROBE, token);
        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isIn("ACCESS_DENIED", "INVALID_TOKEN_IN_HEADER");

        assertThat(statusOf(getJson(PROBE, login("tok_pwchange", "Rotated@Pass456")))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("a token issued for a since-deleted user is refused")
    void tokenForDeletedUserIsRefused() {
        createUser("tok_deleted", PASSWORD, "ADMIN");
        String token = login("tok_deleted", PASSWORD);

        jdbc.update("UPDATE users SET is_active = false WHERE username = ?", "tok_deleted");
        cacheManager.getCache("user").evict("tok_deleted");

        HttpResponse<String> res = getJson(PROBE, token);
        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isNotEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("a token whose issuedAt predates the revoke watermark by seconds is still refused")
    void watermarkIsInclusiveOfBorderlineTokens() {
        createUser("tok_watermark", PASSWORD, "ADMIN");
        String stale = signedTokenFor("tok_watermark",
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));

        jdbc.update("UPDATE users SET tokens_valid_after = now() WHERE username = ?", "tok_watermark");
        cacheManager.getCache("user").evict("tok_watermark");

        HttpResponse<String> res = getJson(PROBE, stale);
        assertThat(statusOf(res)).isEqualTo("SESSION_EXPIRED");
        assertThat(messageOf(res)).contains("revoked");
    }

    private JsonNode loginData(String username, String password) {
        HttpResponse<String> res = loginResponse(username, password);
        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        return dataOf(res);
    }
}
