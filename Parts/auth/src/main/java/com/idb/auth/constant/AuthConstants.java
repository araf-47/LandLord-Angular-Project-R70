package com.idb.auth.constant;

import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_PATTERN_ALL;
import static com.idb.auth.common.constant.CommonConstants.URL_BASE_CONTROLLER;

import java.util.List;

public final class AuthConstants {

    private AuthConstants() {
    }

    // Endpoints
    public static final String ENDPOINT_GET_USER_PERMISSIONS = "/get-user-permissions";
    public static final String ENDPOINT_ROLE_PERMISSIONS = "/role-permissions";
    public static final String ENDPOINT_CHANGE_PASSWORD = "/change-password";
    public static final String ENDPOINT_LOGOUT_ALL = "/logout-all";
    public static final String ENDPOINT_IP_BLOCK_LIST = "/list";
    public static final String ENDPOINT_IP_BLOCK_UNBLOCK = "/unblock";
    public static final String ENDPOINT_IP_BLOCK_UNBLOCK_USER = "/unblock-user";

    // Controllers
    public static final String URL_AUTH_CONTROLLER = URL_BASE_CONTROLLER + "auth";
    public static final String URL_PERMISSION_CONTROLLER = URL_BASE_CONTROLLER + "permission";
    public static final String URL_ROLE_CONTROLLER = URL_BASE_CONTROLLER + "role";
    public static final String URL_USER_CONTROLLER = URL_BASE_CONTROLLER + "user";
    public static final String URL_IP_BLOCK_CONTROLLER = URL_BASE_CONTROLLER + "ip-block";
    public static final String URL_USER_BLOCK_CONTROLLER = URL_BASE_CONTROLLER + "user-block";
    public static final String URL_TEST_CONTROLLER = URL_BASE_CONTROLLER + "test";

    // Roles
    public static final String ROLE_ADMIN = "ADMIN";

    // Patterns
    public static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9])(?=\\S+$).{8,16}$";
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9]{3,100}$";

    /**
     * Everything under the auth controller is public - login, OTP generation and
     * password reset all have to be reachable without a token.
     */
    public static final List<String> AUTH_PUBLIC_URLS = List.of(URL_AUTH_CONTROLLER.concat(ENDPOINT_PATTERN_ALL));

    /**
     * Endpoints that stay reachable from a blocked IP. Without this an
     * administrator whose own IP tripped the threshold would have no way to lift
     * the block - the only route back would be direct database access.
     *
     * <p>Every IP-block check must consult this: the check is duplicated in
     * {@code IpBlockingFilter} (which runs first) and {@code AuthManager} (which
     * runs inside the security chain), and an exemption honoured by only one of
     * them is no exemption at all.
     *
     * <p>These endpoints are still authenticated and still ADMIN-only; exempting
     * them from IP blocking does not make them public.
     */
    public static final List<String> IP_BLOCK_EXEMPT_ENDPOINTS = List.of(
            URL_IP_BLOCK_CONTROLLER + ENDPOINT_IP_BLOCK_UNBLOCK,
            URL_IP_BLOCK_CONTROLLER + ENDPOINT_IP_BLOCK_UNBLOCK_USER);

    public static boolean isIpBlockExempt(String requestUri) {
        if (requestUri == null) {
            return false;
        }
        return IP_BLOCK_EXEMPT_ENDPOINTS.stream().anyMatch(requestUri::startsWith);
    }

    // Configuration keys
    public static final String CONFIG_KEY_PERMISSION_FILE_VERSION = "permission.file.version";

    // Cache names
    public static final String CACHE_OTP = "otp";
    public static final String CACHE_OTP_ATTEMPTS = "otp-attempts";
    public static final String CACHE_PERMISSIONS = "permissions";

    // Messages
    public static final String MESSAGE_PASSWORD_INVALID = "Password must be between 8 and 16 characters long and "
            + "contain at least one uppercase letter, one lowercase letter, one number, and one special character.";

    // IP blocking property keys
    public static final String IP_BLOCKING_ENABLED = "auth.ip.block.enabled";
    public static final String IP_BLOCKING_MAX_FAILED_ATTEMPTS = "auth.ip.block.max.failed.attempts";
    public static final String IP_BLOCKING_MAX_UNAUTHENTICATED_ATTEMPTS = "auth.ip.block.max.unauthenticated.attempts";
    public static final String IP_BLOCKING_MAX_INVALID_JWT_ATTEMPTS = "auth.ip.block.max.invalid.jwt.attempts";
    public static final String IP_BLOCKING_MAX_INVALID_OTP_ATTEMPTS = "auth.ip.block.max.invalid.otp.attempts";
    public static final String IP_BLOCKING_BLOCK_DURATION_HOURS = "auth.ip.block.block.duration.hours";
    public static final String CORS_ORIGINS = "cors.allowed-origins";
}
