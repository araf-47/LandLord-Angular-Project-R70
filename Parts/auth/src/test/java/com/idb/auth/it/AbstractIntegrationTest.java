package com.idb.auth.it;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.idb.auth.AuthApplication;
import com.idb.auth.common.service.MailService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base class for every {@code *IT} test. Load-bearing decisions:
 *
 * <ul>
 * <li><b>Real PostgreSQL, not H2.</b> The permission lookups use PostgreSQL
 * syntax ({@code STRING_AGG}, {@code EXISTS}); H2 would diverge silently and the
 * tests would prove nothing about production behaviour.
 *
 * <li><b>One container for the whole suite.</b> Started once in a static
 * initialiser and never stopped - Testcontainers' reaper removes it at JVM exit.
 *
 * <li><b>{@code AuthApplication.initPublicUrls()} runs in a static
 * initialiser.</b> Production populates {@code PUBLIC_URLS} from {@code main()},
 * which {@code @SpringBootTest} never calls. Without this, {@code AuthFilter}
 * treats {@code /auth/login} as protected and the whole suite gets 401s.
 *
 * <li><b>Plain {@link HttpClient}, not a Spring test client.</b> Requests go over
 * real HTTP to a real port, nothing is stubbed, and both the status line and the
 * response headers stay observable - the {@code x-access-token} rotation header
 * is part of the contract under test.
 *
 * <li><b>Assert on the body, not only on the HTTP status.</b> Application errors
 * come back as HTTP 200 with the failure in {@code ApiResponse.status}, while
 * authentication failures come from the security entry point as 401/403 carrying
 * the same body shape. Tests assert {@link #statusOf} and pin the HTTP code where
 * it is part of the contract.
 *
 * <li><b>Mail is mocked.</b> OTPs exist in plaintext only inside the mail call,
 * so capturing it is the only way a test can learn one.
 * </ul>
 */
@SpringBootTest(classes = AuthApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("auth_test")
                    .withUsername("auth_test")
                    .withPassword("auth_test")
                    // Durability is irrelevant for a throwaway database.
                    .withCommand("postgres", "-c", "fsync=off", "-c", "full_page_writes=off",
                            "-c", "synchronous_commit=off");

    static {
        POSTGRES.start();
        AuthApplication.initPublicUrls();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    /** Mocked so no test run can send mail, and so OTPs can be captured. */
    @MockitoBean
    protected MailService mailService;

    @LocalServerPort
    protected int port;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    protected CacheManager cacheManager;

    protected JdbcTemplate jdbc;

    protected final JsonMapper mapper = JsonMapper.builder().build();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @BeforeEach
    void initJdbc() {
        jdbc = new JdbcTemplate(dataSource);
    }

    // ------------------------------------------------------------------
    // HTTP helpers
    // ------------------------------------------------------------------

    protected String url(String path) {
        return "http://localhost:" + port + (path.startsWith("/") ? path : "/" + path);
    }

    protected HttpResponse<String> postJson(String path, Object body) {
        return send(request(path).POST(bodyOf(body)).header("Content-Type", "application/json").build());
    }

    protected HttpResponse<String> postJson(String path, Object body, String token) {
        return send(request(path).POST(bodyOf(body))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .build());
    }

    protected HttpResponse<String> getJson(String path) {
        return send(request(path).GET().build());
    }

    protected HttpResponse<String> getJson(String path, String token) {
        return send(request(path).GET().header("Authorization", "Bearer " + token).build());
    }

    protected HttpResponse<String> getJson(String path, String accessToken, String refreshToken) {
        return send(request(path).GET()
                .header("Authorization", "Bearer " + accessToken)
                .header("x-refresh-token", refreshToken)
                .build());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(url(path)))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");
    }

    private HttpRequest.BodyPublisher bodyOf(Object body) {
        return HttpRequest.BodyPublishers.ofString(toJson(body), StandardCharsets.UTF_8);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new AssertionError("HTTP call failed: " + request.method() + " " + request.uri(), e);
        }
    }

    // ------------------------------------------------------------------
    // Response helpers
    // ------------------------------------------------------------------

    protected JsonNode body(HttpResponse<String> response) {
        try {
            return mapper.readTree(response.body());
        } catch (Exception e) {
            throw new AssertionError("Response body was not valid JSON: " + response.body(), e);
        }
    }

    protected String statusOf(HttpResponse<String> response) {
        JsonNode node = body(response).get("status");
        return node == null || node.isNull() ? null : node.asText();
    }

    protected String messageOf(HttpResponse<String> response) {
        JsonNode node = body(response).get("message");
        return node == null || node.isNull() ? null : node.asText();
    }

    protected JsonNode dataOf(HttpResponse<String> response) {
        return body(response).get("data");
    }

    protected Optional<String> header(HttpResponse<String> response, String name) {
        return response.headers().firstValue(name);
    }

    protected String toJson(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof String s) {
            return s;
        }
        return mapper.writeValueAsString(o);
    }

    /** Ordered map helper - {@code Map.of} cannot hold null values. */
    protected Map<String, Object> payload(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    /**
     * Clears per-test state and evicts every cache.
     *
     * <p>Only the mutable tables are truncated. {@code roles},
     * {@code permissions}, {@code role_permissions} and {@code configurations}
     * hold the authorization matrix, which is imported from permissions.json
     * <em>once at startup</em> - wiping them would silently strip every role of
     * its permissions for the rest of the suite, since nothing reloads the file
     * mid-run.
     *
     * <p>The cache eviction is not optional: all IT classes sharing a context
     * share the Caffeine caches, {@code findByUsername} is {@code @Cacheable},
     * and {@code RESTART IDENTITY} recycles ids from 1. Without it a cached
     * {@code User} outlives its row and is served under a recycled id - e.g. a
     * user cached with 2FA on makes a later test's login return
     * {@code OTP_REQUIRED} for what should be a fresh user.
     */
    protected void truncateAll() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(name -> Optional.ofNullable(cacheManager.getCache(name))
                    .ifPresent(org.springframework.cache.Cache::clear));
        }
        jdbc.execute("TRUNCATE TABLE \"user_roles\", \"users\", \"blocked_ip\" RESTART IDENTITY CASCADE");
    }

    protected long ensureRole(String name) {
        List<Long> existing = jdbc.queryForList("SELECT id FROM roles WHERE name = ?", Long.class, name);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        jdbc.update("INSERT INTO roles (name, is_active, created_at, created_by) VALUES (?, true, now(), 'test')",
                name);
        return jdbc.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, name);
    }

    protected long createUser(String username, String rawPassword, String role) {
        return createUser(username, rawPassword, role, false);
    }

    /**
     * Inserts an active user with a BCrypt password and the given role. Written
     * with raw JDBC so the fixture does not depend on the service layer under
     * test; JPA auditing does not apply to raw inserts, hence the explicit audit
     * columns.
     */
    protected long createUser(String username, String rawPassword, String role, boolean twoFactorEnabled) {
        long roleId = ensureRole(role);
        jdbc.update("""
                INSERT INTO users (username, password, email, phone, is_active,
                                   failed_login_attempts, account_locked, two_factor_enabled,
                                   created_at, created_by)
                VALUES (?, ?, ?, ?, true, 0, false, ?, now(), 'test')
                """, username, passwordEncoder.encode(rawPassword),
                username + "@test.local", uniquePhone(username), twoFactorEnabled);
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        return userId;
    }

    /** {@code users.phone} is unique, so it has to be derived from the username. */
    private String uniquePhone(String username) {
        return "+8801" + String.format("%09d", Math.abs(username.hashCode()) % 1_000_000_000);
    }

    protected String storedPasswordHash(String username) {
        return jdbc.queryForObject("SELECT password FROM users WHERE username = ?", String.class, username);
    }

    /** Logs in over real HTTP and returns the access token. */
    protected String login(String username, String password) {
        HttpResponse<String> res = loginResponse(username, password);
        if (!"SUCCESS".equals(statusOf(res))) {
            throw new AssertionError("Fixture login failed for '" + username + "': status=" + statusOf(res)
                    + " body=" + res.body());
        }
        return dataOf(res).get("accessToken").asText();
    }

    protected HttpResponse<String> loginResponse(String username, String password) {
        return postJson("/api/v3/auth/login", Map.of("username", username, "password", password));
    }

    /**
     * Blocks until the wall clock crosses into the next second.
     *
     * <p>Needed around token revocation: a JWT {@code iat} has whole-second
     * resolution, so {@code JwtUtil} must refuse tokens issued in the same second
     * as the revoke (see its {@code rejectIfRevoked} javadoc). Crossing the
     * boundary makes "a token minted after the revoke" unambiguous instead of
     * dependent on sub-second timing.
     */
    protected void awaitNextSecond() {
        long startSecond = Instant.now().getEpochSecond();
        while (Instant.now().getEpochSecond() == startSecond) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for the next second", e);
            }
        }
    }

    /**
     * Mints an already-expired token signed with the user's current password hash -
     * the same key the application derives. Building it here rather than waiting out
     * a short TTL keeps the test fast and deterministic.
     */
    protected String expiredTokenFor(String username) {
        return signedTokenFor(username, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
    }

    protected String signedTokenFor(String username, Instant issuedAt, Instant expiresAt) {
        SecretKey key = Keys.hmacShaKeyFor(storedPasswordHash(username).getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }
}
