package com.idb.auth.common.constant;

import java.util.LinkedList;
import java.util.List;

public final class CommonConstants {
    public static final String EMPTY_STRING = "";

    // Auth Headers
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_TOKEN_PREFIX = "Bearer ";
    public static final String ACCESS_TOKEN_HEADER = "x-access-token";
    public static final String REFRESH_TOKEN_HEADER = "x-refresh-token";

    // Endpoints
    public static final String ENDPOINT_LOGIN = "/login";
    public static final String ENDPOINT_FORGOT_PASSWORD = "/forgot-password";
    public static final String ENDPOINT_LIST = "/list";
    public static final String ENDPOINT_REGISTER = "/register";
    public static final String ENDPOINT_UPDATE = "/update";
    public static final String ENDPOINT_GENERATE_OTP = "/otp";
    public static final String ENDPOINT_CLEAR_OTP_CACHE = "/clear-otp-cache";
    public static final String ENDPOINT_TOGGLE_2FA = "/toggle-2fa";
    public static final String ENDPOINT_PATTERN_ALL = "/**";

    // Controllers
    public static final String URL_BASE_CONTROLLER = "/api/v3/";

    // Patterns
    public static final String PHONE_PATTERN = "^\\+?[1-9]\\d{1,14}$";
    public static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    // Error Messages
    public static final String VALIDATION_ERROR_MSG = "Validation failed";

    // Cache Container
    public static final String CACHE_USER = "user";
    public static final String CACHE_CONFIGURATION = "configuration";

    // Alphanumeric Constants
    public static final int DEFAULT_ID_LENGTH = 10;

    /**
     * Mutable, module-aggregated list of endpoints that bypass authentication.
     *
     * <p>Populated by {@code AuthApplication.initPublicUrls()} before the Spring
     * context starts, exactly as the reference implementation does it from
     * {@code main()}.
     *
     * <p><b>Patterns must be valid Spring {@code PathPattern}s.</b> Spring
     * Security 7 removed {@code AntPathRequestMatcher}; the reference project's
     * suffix globs ({@code /**.html}, {@code /**.css}) are not expressible as
     * PathPatterns and have been dropped - this is an API-only service with no
     * static resources to exempt.
     */
    public static final List<String> PUBLIC_URLS = new LinkedList<>(List.of(
            "/v3/api-docs/**", "/swagger-ui/**", "/actuator/health"));

    /**
     * Like {@link #PUBLIC_URLS}, but scoped to {@code GET} only - for endpoints
     * that must stay anonymously readable (e.g. a public marketplace's guest
     * browse/search) while other verbs on the same path stay role-gated via
     * permissions.json. A plain {@code PUBLIC_URLS} entry would permit every verb
     * on that path, including writes.
     */
    public static final List<String> PUBLIC_GET_URLS = new LinkedList<>();

    private CommonConstants() {
    }
}
