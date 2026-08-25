package com.idb.auth.util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.idb.auth.common.util.StringUtil;
import com.idb.auth.dto.request.BearerAuthenticationToken;
import com.idb.auth.exception.InvalidTokenInHeaderException;
import com.idb.auth.exception.SessionExpiredException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Tokens are signed <b>per user, with that user's BCrypt password hash as the
 * HMAC secret</b>. Two consequences that the rest of the flow depends on:
 *
 * <ul>
 * <li>Changing a password invalidates every token that user holds, with no
 * blacklist required.
 * <li>Verifying a token requires loading the user first, which is why
 * {@code AuthUtil.getUsernameFromAccessToken} reads the subject unverified.
 * </ul>
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.token.expiration.access}")
    private long accessTokenExpiration;

    @Value("${jwt.token.expiration.refresh}")
    private long refreshTokenExpiration;

    /**
     * Derives the HMAC key from the user's stored password hash.
     *
     * <p>Deliberately <b>not</b> {@code @Cacheable}. The reference annotates this
     * method and then calls it only from {@link #createToken} and
     * {@link #extractAllClaims} on {@code this} - self-invocation never reaches the
     * Spring proxy, so the cache it declared was dead weight that merely looked
     * like an optimisation. It is dropped rather than fixed because
     * {@code hmacShaKeyFor} only length-checks and wraps the bytes; caching it
     * would buy nothing and would keep derived key material in a second place.
     *
     * <p>If key derivation ever becomes expensive, caching it needs a separate bean
     * - not an annotation on a self-invoked method.
     */
    public SecretKey generateKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, String secretKey) {
        return createToken(new HashMap<>(), username, accessTokenExpiration, secretKey);
    }

    public String generateRefreshToken(String username, String secretKey) {
        return createToken(new HashMap<>(), username, refreshTokenExpiration, secretKey);
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration, String secretKey) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(generateKey(secretKey))
                .compact();
    }

    /**
     * Validates the presented access token and, when it has expired, silently
     * rotates it using the refresh token.
     *
     * @return a token whose credentials are the new access token if one was
     *         minted, or {@code null} credentials if the presented access token
     *         was still valid
     */
    public BearerAuthenticationToken getBearerToken(String accessToken, String refreshToken, String secretKey,
            LocalDateTime tokensValidAfter) throws AuthenticationException {
        try {
            if (StringUtil.isEmpty(accessToken)) {
                return null;
            }
            Claims claims;
            String issuedToken = accessToken;
            try {
                claims = extractAllClaims(accessToken, secretKey);
                issuedToken = null;
            } catch (SessionExpiredException e) {
                if (StringUtil.isEmpty(refreshToken)) {
                    throw e;
                }
                claims = extractAllClaims(refreshToken, secretKey);
                issuedToken = generateAccessToken(claims.getSubject(), secretKey);
            }
            rejectIfRevoked(claims, tokensValidAfter);
            return new BearerAuthenticationToken(claims.getSubject(), issuedToken, true);
        } catch (SessionExpiredException e) {
            throw e;
        } catch (Exception e) {
            throw AuthUtil.getAuthenticationException(
                    "Bearer token validation failed: %s".formatted(e.getMessage()), e);
        }
    }

    /**
     * Enforces the "logout everywhere" watermark: a token whose {@code iat} is at
     * or before {@code tokensValidAfter} is refused even though its signature is
     * valid and it has not expired.
     *
     * <p>The comparison is {@code <=}, not {@code <}, and that is load-bearing. A
     * JWT {@code iat} is a NumericDate with <b>whole-second</b> resolution, while
     * the watermark is stored with sub-second precision. With a strict {@code <},
     * a token minted at 12:00:00.100 and revoked at 12:00:00.700 compares as
     * {@code 12:00:00 < 12:00:00} - false - and survives its own revocation. Every
     * "log out everywhere" would silently leave a usable token behind for up to a
     * second.
     *
     * <p>The cost of failing closed is that a token minted later in the same second
     * as the revoke is also refused, since it is indistinguishable at this
     * resolution. The caller simply logs in again.
     */
    private void rejectIfRevoked(Claims claims, LocalDateTime tokensValidAfter) {
        if (tokensValidAfter == null || claims == null) {
            return;
        }
        long validAfterEpochSecond = tokensValidAfter.atZone(ZoneId.systemDefault()).toEpochSecond();
        Date issuedAt = claims.getIssuedAt();
        if (issuedAt == null || issuedAt.toInstant().getEpochSecond() <= validAfterEpochSecond) {
            throw new SessionExpiredException("Session has been revoked. Please login again.");
        }
    }

    private Claims extractAllClaims(String token, String secretKey) {
        try {
            return Jwts.parser()
                    .verifyWith(generateKey(secretKey))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new SessionExpiredException("Session expired. Please login again.");
        } catch (Exception e) {
            throw new InvalidTokenInHeaderException("Invalid token in header", e);
        }
    }
}
