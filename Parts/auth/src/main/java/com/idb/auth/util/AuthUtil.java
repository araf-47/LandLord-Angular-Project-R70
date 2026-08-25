package com.idb.auth.util;

import static com.idb.auth.constant.AuthConstants.PASSWORD_PATTERN;
import static com.idb.auth.constant.AuthConstants.USERNAME_PATTERN;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

import com.idb.auth.common.util.StringUtil;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class AuthUtil {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private AuthUtil() {
    }

    public static boolean isValidUsername(String username) {
        return StringUtil.isNotBlank(username) && username.matches(USERNAME_PATTERN);
    }

    public static boolean isValidStrongPassword(String value) {
        return StringUtil.isNotBlank(value) && value.matches(PASSWORD_PATTERN);
    }

    /**
     * Reads {@code sub} out of the JWT payload <b>without verifying the
     * signature</b>. This is deliberate and safe only because the value is used
     * solely to look up the user, whose stored password hash is then the key the
     * signature is actually verified against ({@code JwtUtil.getBearerToken}). An
     * unverified subject never grants access on its own.
     *
     * <p>Parsed as JSON rather than by string-splitting on {@code "sub":"} so a
     * crafted payload cannot smuggle a different subject past the check.
     */
    public static String getUsernameFromAccessToken(String accessToken) throws InsufficientAuthenticationException {
        String[] parts = accessToken.split("\\.");
        if (parts.length != 3) {
            throw new InsufficientAuthenticationException("Invalid JWT token format");
        }
        String username;
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = MAPPER.readTree(new String(payload, StandardCharsets.UTF_8));
            JsonNode sub = node.get("sub");
            username = sub == null || sub.isNull() ? null : sub.asText();
        } catch (Exception e) {
            throw new InsufficientAuthenticationException("Invalid JWT token format", e);
        }
        if (StringUtil.isEmpty(username)) {
            throw new InsufficientAuthenticationException("Username not found in token");
        }
        return username;
    }

    public static AuthenticationException getAuthenticationException(String message, Throwable cause) {
        return new AuthenticationException(message, cause) {
            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public synchronized Throwable getCause() {
                return cause;
            }
        };
    }
}
