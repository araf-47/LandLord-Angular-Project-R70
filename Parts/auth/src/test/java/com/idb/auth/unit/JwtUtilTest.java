package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.idb.auth.dto.request.BearerAuthenticationToken;
import com.idb.auth.exception.InvalidTokenInHeaderException;
import com.idb.auth.exception.SessionExpiredException;
import com.idb.auth.util.JwtUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * The heart of the scheme: per-user signing keys, expiry, silent refresh, and the
 * revocation watermark.
 */
class JwtUtilTest {

    /** A BCrypt hash - the real shape of a signing secret here (60 chars / 480 bits). */
    private static final String SECRET = "$2a$10$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVWXYZ012345";
    private static final String OTHER_SECRET = "$2a$10$zyxwvutsrqponmlkjihgfZYXWVUTSRQPONMLKJIHGFEDCBA987654";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900_000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 31_536_000_000L);
    }

    @Test
    @DisplayName("an access token round-trips and carries the username as its subject")
    void accessTokenRoundTrips() {
        String token = jwtUtil.generateAccessToken("alice", SECRET);

        BearerAuthenticationToken auth = jwtUtil.getBearerToken(token, null, SECRET, null);

        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice");
        assertThat(auth.isAuthenticated()).isTrue();
        // Null credentials mean "the presented token was fine, nothing was rotated".
        assertThat(auth.getCredentials()).isNull();
    }

    @Test
    @DisplayName("a token signed with one user's secret does not verify against another's")
    void keysAreScopedToTheUser() {
        String token = jwtUtil.generateAccessToken("alice", SECRET);

        assertThatThrownBy(() -> jwtUtil.getBearerToken(token, null, OTHER_SECRET, null))
                .satisfies(e -> assertThat(e.getMessage()).contains("Bearer token validation failed"));
    }

    @Test
    @DisplayName("an expired access token with a valid refresh token is silently rotated")
    void expiredAccessIsRotatedFromRefresh() {
        String expiredAccess = expired("alice");
        String refresh = jwtUtil.generateRefreshToken("alice", SECRET);

        BearerAuthenticationToken auth = jwtUtil.getBearerToken(expiredAccess, refresh, SECRET, null);

        assertThat(auth.getName()).isEqualTo("alice");
        // Non-null credentials are the signal AuthFilter uses to emit x-access-token.
        assertThat(auth.getCredentials()).asString().isNotBlank().isNotEqualTo(expiredAccess);

        // And the replacement is itself a valid token.
        assertThat(jwtUtil.getBearerToken((String) auth.getCredentials(), null, SECRET, null).getName())
                .isEqualTo("alice");
    }

    @Test
    @DisplayName("an expired access token with no refresh token is a session expiry, not an invalid token")
    void expiredAccessWithoutRefreshIsSessionExpired() {
        assertThatThrownBy(() -> jwtUtil.getBearerToken(expired("alice"), null, SECRET, null))
                .isInstanceOf(SessionExpiredException.class)
                .hasMessageContaining("Session expired");
    }

    @Test
    @DisplayName("an expired access token with a blank refresh token is a session expiry")
    void expiredAccessWithBlankRefreshIsSessionExpired() {
        assertThatThrownBy(() -> jwtUtil.getBearerToken(expired("alice"), "", SECRET, null))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    @DisplayName("an expired refresh token cannot rescue an expired access token")
    void expiredRefreshCannotRotate() {
        assertThatThrownBy(() -> jwtUtil.getBearerToken(expired("alice"), expired("alice"), SECRET, null))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    @DisplayName("a malformed token is reported as an invalid header token")
    void malformedTokenIsInvalidHeader() {
        assertThatThrownBy(() -> jwtUtil.getBearerToken("not.a.jwt", null, SECRET, null))
                .satisfies(e -> assertThat(e.getCause()).isInstanceOf(InvalidTokenInHeaderException.class));
    }

    @Test
    @DisplayName("an absent token yields no authentication rather than an error")
    void emptyTokenReturnsNull() {
        assertThat(jwtUtil.getBearerToken(null, null, SECRET, null)).isNull();
        assertThat(jwtUtil.getBearerToken("", null, SECRET, null)).isNull();
    }

    @Test
    @DisplayName("a token issued strictly before the watermark is revoked")
    void tokenIssuedBeforeWatermarkIsRevoked() {
        String token = signed("alice", LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> jwtUtil.getBearerToken(token, null, SECRET, LocalDateTime.now().minusMinutes(5)))
                .isInstanceOf(SessionExpiredException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("a token issued in the SAME SECOND as the watermark is revoked - the comparison fails closed")
    void sameSecondAsWatermarkIsRevoked() {
        // This is the regression the reference had: JWT iat is whole-second while
        // the watermark has sub-second precision, so a strict `<` let a token minted
        // milliseconds before a revoke survive it. Every "log out everywhere" left a
        // usable token behind for up to a second.
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String token = signed("alice", now, now.plusHours(1));
        LocalDateTime watermarkSameSecond = now.withNano(700_000_000);

        assertThatThrownBy(() -> jwtUtil.getBearerToken(token, null, SECRET, watermarkSameSecond))
                .isInstanceOf(SessionExpiredException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("a token issued after the watermark survives, so a fresh login still works")
    void tokenIssuedAfterWatermarkSurvives() {
        LocalDateTime watermark = LocalDateTime.now().minusMinutes(5);
        String token = signed("alice", LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        assertThat(jwtUtil.getBearerToken(token, null, SECRET, watermark).getName()).isEqualTo("alice");
    }

    @Test
    @DisplayName("no watermark means no revocation check")
    void nullWatermarkSkipsTheCheck() {
        String token = signed("alice", LocalDateTime.now().minusYears(1), LocalDateTime.now().plusHours(1));
        assertThat(jwtUtil.getBearerToken(token, null, SECRET, null).getName()).isEqualTo("alice");
    }

    @Test
    @DisplayName("the revocation watermark also applies to a token minted by refresh rotation")
    void rotationStillHonoursTheWatermark() {
        // The refresh token predates the revoke, so rotating off it must not mint a
        // token that outlives the revocation.
        String expiredAccess = expired("alice");
        String oldRefresh = signed("alice", LocalDateTime.now().minusHours(2), LocalDateTime.now().plusYears(1));

        assertThatThrownBy(() -> jwtUtil.getBearerToken(expiredAccess, oldRefresh, SECRET,
                LocalDateTime.now().minusMinutes(1)))
                .isInstanceOf(SessionExpiredException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("key derivation is deterministic per secret and differs across secrets")
    void keyDerivation() {
        assertThat(jwtUtil.generateKey(SECRET).getEncoded())
                .isEqualTo(jwtUtil.generateKey(SECRET).getEncoded())
                .isNotEqualTo(jwtUtil.generateKey(OTHER_SECRET).getEncoded());
    }

    @Test
    @DisplayName("a refresh token outlives an access token")
    void refreshOutlivesAccess() {
        var access = Jwts.parser().verifyWith(jwtUtil.generateKey(SECRET)).build()
                .parseSignedClaims(jwtUtil.generateAccessToken("alice", SECRET)).getPayload();
        var refresh = Jwts.parser().verifyWith(jwtUtil.generateKey(SECRET)).build()
                .parseSignedClaims(jwtUtil.generateRefreshToken("alice", SECRET)).getPayload();

        assertThat(refresh.getExpiration()).isAfter(access.getExpiration());
        assertThat(jwtUtil.getAccessTokenExpiration()).isEqualTo(900_000L);
    }

    private String expired(String subject) {
        return signed(subject, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
    }

    private String signed(String subject, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(toDate(issuedAt))
                .expiration(toDate(expiresAt))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private Date toDate(LocalDateTime value) {
        return Date.from(value.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }
}
