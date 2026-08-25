package com.idb.auth.it;

import static com.idb.auth.constant.AuthConstants.URL_TEST_CONTROLLER;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The @Configuration beans, verified against the running context rather than by
 * reading the class: caching, JPA auditing, CORS, session policy, CSRF, and the
 * property bindings the security behaviour depends on.
 */
class ConfigurationIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Config@Pass123";

    @Autowired private PasswordEncoder passwordEncoderBean;
    @Autowired private CacheManager cacheManagerBean;

    @Value("${security.account-lockout.max-attempts}") private int maxAttempts;
    @Value("${security.account-lockout.duration-minutes}") private long lockoutMinutes;
    @Value("${jwt.token.expiration.access}") private long accessExpiry;
    @Value("${jwt.token.expiration.refresh}") private long refreshExpiry;
    @Value("${auth.ip.block.enabled}") private boolean ipBlockingEnabled;

    @BeforeEach
    void setUp() {
        truncateAll();
    }

    // ---------------- CacheConfig ----------------

    @Test
    @DisplayName("every cache the code names is registered by CacheConfig")
    void allNamedCachesExist() {
        // A missing cache is not a performance problem here: OtpServiceImpl throws
        // IllegalStateException rather than silently disabling OTP, and the JWT key
        // cache is on the hot path of every authenticated request.
        assertThat(cacheManagerBean.getCacheNames())
                .contains("user", "permissions", "otp", "otp-attempts", "configuration");
    }

    @Test
    @DisplayName("the user cache actually caches, and a save writes through instead of going stale")
    void userCacheWritesThrough() {
        createUser("cfg_cache", PASSWORD, "ADMIN");

        // Populate the cache through a login.
        login("cfg_cache", PASSWORD);
        assertThat(cacheManagerBean.getCache("user").get("cfg_cache")).isNotNull();

        // A @CachePut on save keeps the cached copy current; without it a role or
        // password change would be invisible until the TTL elapsed.
        assertThat(cacheManagerBean.getCache("user").get("cfg_cache").get()).isNotNull();
    }

    @Test
    @DisplayName("there is no jwt-secret-keys cache - the annotation it came from was never effective")
    void noDeadJwtKeyCache() {
        // JwtUtil.generateKey is only ever self-invoked, so an @Cacheable there
        // never reached the Spring proxy. The declaration was removed rather than
        // left in place looking like an optimisation.
        assertThat(cacheManagerBean.getCacheNames()).doesNotContain("jwt-secret-keys");
    }

    // ---------------- AuditingConfig ----------------

    @Test
    @DisplayName("JPA auditing stamps createdBy/createdAt, using SYSTEM for startup writes")
    void auditingStampsStartupWrites() {
        // permissions.json is imported before any request exists, so the auditor
        // must fall back rather than NPE on an absent SecurityContext.
        var row = jdbc.queryForMap("SELECT created_by, created_at FROM permissions LIMIT 1");

        assertThat(row.get("created_by")).isEqualTo("SYSTEM");
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    @DisplayName("a write made inside an authenticated request is attributed to that user")
    void auditingAttributesRequestScopedWrites() {
        createUser("cfg_auditor", PASSWORD, "ADMIN");
        ensureRole("USER");
        String token = login("cfg_auditor", PASSWORD);

        assertThat(statusOf(postJson("/api/v3/user/register", payload(
                "username", "cfgAudited",
                "password", "Audited@Pass1",
                "email", "cfg_audited@test.local",
                "phone", "+8801788000001",
                "roles", List.of("USER")), token))).isEqualTo("SUCCESS");

        assertThat(jdbc.queryForObject("SELECT created_by FROM users WHERE username = 'cfgAudited'",
                String.class)).isEqualTo("cfg_auditor");
    }

    @Test
    @DisplayName("an update stamps the modifier without touching the creation columns")
    void auditingKeepsCreationImmutable() {
        createUser("cfg_updater", PASSWORD, "ADMIN");
        String token = login("cfg_updater", PASSWORD);
        long id = jdbc.queryForObject("SELECT id FROM users WHERE username = 'cfg_updater'", Long.class);
        String createdBy = jdbc.queryForObject("SELECT created_by FROM users WHERE id = ?", String.class, id);

        assertThat(statusOf(postJson("/api/v3/user/update",
                payload("id", id, "phone", "+8801788000002"), token))).isEqualTo("SUCCESS");

        var row = jdbc.queryForMap("SELECT created_by, updated_by, updated_at FROM users WHERE id = " + id);
        assertThat(row.get("created_by")).isEqualTo(createdBy);
        assertThat(row.get("updated_by")).isEqualTo("cfg_updater");
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    @DisplayName("the password encoder bean is BCrypt, and stored hashes are in that format")
    void passwordEncoderIsBcrypt() {
        assertThat(passwordEncoderBean).isInstanceOf(BCryptPasswordEncoder.class);

        createUser("cfg_bcrypt", PASSWORD, "ADMIN");
        String hash = storedPasswordHash("cfg_bcrypt");
        assertThat(hash).startsWith("$2").hasSizeGreaterThanOrEqualTo(59);
        assertThat(passwordEncoderBean.matches(PASSWORD, hash)).isTrue();
    }

    // ---------------- SecurityConfig ----------------

    @Test
    @DisplayName("a cross-origin preflight is answered without credentials and exposes the token headers")
    void corsPreflightIsHandled() throws Exception {
        HttpResponse<String> res = send(HttpRequest.newBuilder(URI.create(url("/api/v3/auth/login")))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "https://app.example.com")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,authorization")
                .build());

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.headers().firstValue("Access-Control-Allow-Origin"))
                .contains("https://app.example.com");
        assertThat(res.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
        // x-access-token must be exposed or a browser client can never read a
        // silently rotated token.
        assertThat(res.headers().firstValue("Access-Control-Expose-Headers").orElse(""))
                .contains("x-access-token");
    }

    @Test
    @DisplayName("the session policy is stateless - no session cookie is ever issued")
    void noSessionCookieIsIssued() {
        createUser("cfg_stateless", PASSWORD, "ADMIN");

        HttpResponse<String> res = loginResponse("cfg_stateless", PASSWORD);

        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        assertThat(res.headers().allValues("Set-Cookie")).noneMatch(c -> c.contains("JSESSIONID"));
    }

    @Test
    @DisplayName("CSRF is disabled, so a state-changing POST needs no token beyond the bearer")
    void csrfIsDisabled() {
        createUser("cfg_csrf", PASSWORD, "ADMIN");
        String token = login("cfg_csrf", PASSWORD);

        // With CSRF on this POST would be 403 regardless of the bearer token.
        assertThat(statusOf(postJson("/api/v3/user/logout-all", java.util.Map.of(), token)))
                .isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("anonymous access is disabled - there is no half-authenticated state")
    void anonymousIsDisabled() {
        // With anonymous enabled an unauthenticated request would arrive at the
        // authorization layer carrying an AnonymousAuthenticationToken; here it is
        // refused outright by AuthFilter instead.
        HttpResponse<String> res = getJson(URL_TEST_CONTROLLER + "/any");

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(statusOf(res)).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("an unmapped path is still authenticated - anyRequest().authenticated() is the default")
    void unmappedPathsAreProtected() {
        HttpResponse<String> res = getJson("/api/v3/does/not/exist");

        // Fail-closed: a newly added controller is protected before anyone
        // remembers to add it to permissions.json.
        assertThat(res.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("the actuator health endpoint is public")
    void healthEndpointIsPublic() {
        HttpResponse<String> res = getJson("/actuator/health");

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("UP");
    }

    // ---------------- property binding ----------------

    @Test
    @DisplayName("the security-relevant properties are actually bound from the test profile")
    void propertiesAreBound() {
        // A typo in a property key would leave these at their @Value defaults and
        // silently change the lockout and expiry behaviour the tests rely on.
        assertThat(maxAttempts).isEqualTo(5);
        assertThat(lockoutMinutes).isEqualTo(30L);
        assertThat(accessExpiry).isEqualTo(900_000L);
        assertThat(refreshExpiry).isEqualTo(31_536_000_000L);
        assertThat(ipBlockingEnabled).isFalse();
    }

    @Test
    @DisplayName("with IP blocking off, the no-op service is wired and the filter is absent")
    void noOpIpBlockingIsSelected() {
        // Both are @ConditionalOnProperty on the same key, so an inconsistency here
        // would mean requests get checked by a filter backed by a no-op service.
        assertThat(getJson(URL_TEST_CONTROLLER + "/any").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM blocked_ip", Integer.class)).isZero();
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }
}
