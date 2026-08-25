package com.idb.auth.it;

import static com.idb.auth.constant.AuthConstants.ENDPOINT_IP_BLOCK_LIST;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_IP_BLOCK_UNBLOCK;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_IP_BLOCK_UNBLOCK_USER;
import static com.idb.auth.constant.AuthConstants.URL_IP_BLOCK_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_TEST_CONTROLLER;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Per-IP blocking, which is off in the default test profile because it is
 * stateful across requests. Enabling it here swaps
 * {@code NoOpIpBlockingServiceImpl} for the persistent implementation and
 * activates {@code IpBlockingFilter} - a different bean graph, so this class gets
 * its own Spring context.
 *
 * <p>Thresholds are set low so the boundary is reachable in a handful of calls,
 * and the block duration is long enough that lazy expiry cannot fire mid-test.
 */
@TestPropertySource(properties = {
        "auth.ip.block.enabled=true",
        "auth.ip.block.max.unauthenticated.attempts=3",
        "auth.ip.block.max.invalid.jwt.attempts=3",
        "auth.ip.block.max.failed.attempts=4",
        "auth.ip.block.max.invalid.otp.attempts=2",
        "auth.ip.block.block.duration.hours=24"
})
class IpBlockingIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "IpBlock@Pass123";
    private static final String PROBE = URL_TEST_CONTROLLER + "/any";

    @BeforeEach
    void setUp() {
        truncateAll();
    }

    @Test
    @DisplayName("unauthenticated attempts accumulate and the IP is blocked at the threshold")
    void unauthenticatedAttemptsBlockTheIp() {
        // Below the threshold: refused, but not blocked.
        for (int i = 0; i < 2; i++) {
            assertThat(getJson(PROBE).statusCode()).isEqualTo(401);
        }
        assertThat(blockedIpCount()).isEqualTo(1);
        assertThat(isIpActive()).isFalse();

        // The third attempt reaches the threshold and flips the row to blocked.
        assertThat(getJson(PROBE).statusCode()).isEqualTo(401);
        assertThat(isIpActive()).isTrue();

        // From now on IpBlockingFilter answers before anything else runs.
        HttpResponse<String> blocked = getJson(PROBE);
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(statusOf(blocked)).isEqualTo("IP_BLOCKED");
        assertThat(messageOf(blocked)).contains("blocked").contains("24 hours");
    }

    @Test
    @DisplayName("a blocked IP cannot even reach the public login endpoint")
    void blockedIpCannotLogIn() {
        createUser("ipb_user", PASSWORD, "ADMIN");
        blockThisIp();

        HttpResponse<String> res = loginResponse("ipb_user", PASSWORD);

        // Login is a public URL, but the IP filter runs ahead of the whole security
        // chain, so being public does not help.
        assertThat(res.statusCode()).isEqualTo(429);
        assertThat(statusOf(res)).isEqualTo("IP_BLOCKED");
    }

    @Test
    @DisplayName("repeated malformed JWTs block the IP under the INVALID_JWT threshold")
    void invalidJwtAttemptsBlockTheIp() {
        for (int i = 0; i < 3; i++) {
            assertThat(getJson(PROBE, "not-a-jwt").statusCode()).isEqualTo(401);
        }

        assertThat(isIpActive()).isTrue();
        assertThat(jdbc.queryForObject("SELECT last_failure_type FROM blocked_ip", String.class))
                .isEqualTo("INVALID_JWT");
        assertThat(jdbc.queryForObject("SELECT reason FROM blocked_ip", String.class))
                .isEqualTo("Invalid JWT token");
    }

    @Test
    @DisplayName("wrong passwords are recorded as LOGIN attempts and warn about the remaining budget")
    void failedLoginsCountTowardsTheIpBudget() {
        createUser("ipb_wrongpass", PASSWORD, "ADMIN");

        HttpResponse<String> first = loginResponse("ipb_wrongpass", "Wrong@Pass999");
        assertThat(statusOf(first)).isEqualTo("BAD_CREDENTIALS");
        // max.failed.attempts=4, one consumed, so three remain.
        assertThat(messageOf(first)).contains("3 attempts remaining");

        assertThat(jdbc.queryForObject("SELECT username FROM blocked_ip", String.class)).isEqualTo("ipb_wrongpass");
        assertThat(jdbc.queryForObject("SELECT failed_login_attempts FROM blocked_ip", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("an expired block is lifted lazily on the next request, with no scheduled sweep")
    void expiredBlockIsLiftedOnNextRequest() {
        createUser("ipb_expiring", PASSWORD, "ADMIN");
        blockThisIp();
        assertThat(loginResponse("ipb_expiring", PASSWORD).statusCode()).isEqualTo(429);

        jdbc.update("UPDATE blocked_ip SET unblock_at = now() - interval '1 hour'");

        assertThat(statusOf(loginResponse("ipb_expiring", PASSWORD))).isEqualTo("SUCCESS");
        assertThat(isIpActive()).isFalse();
    }

    @Test
    @DisplayName("an admin can list blocked IPs and unblock one, and the unblock endpoint stays reachable")
    void adminCanListAndUnblockIps() {
        createUser("ipb_admin", PASSWORD, "ADMIN");
        String adminToken = login("ipb_admin", PASSWORD);

        // Block a different IP than the caller's, so the admin can still get in.
        jdbc.update("""
                INSERT INTO blocked_ip (ip_address, blocked_at, unblock_at, endpoint, username,
                                        is_active, failed_attempts, failed_login_attempts,
                                        failed_unauthenticated_attempts, last_failure_type, last_attempt_at, reason)
                VALUES ('10.9.9.9', now(), now() + interval '24 hours', '/api/v3/test/any', 'someone',
                        true, 9, 9, 0, 'LOGIN', now(), 'Invalid credentials')
                """);

        HttpResponse<String> list = postJson(URL_IP_BLOCK_CONTROLLER + ENDPOINT_IP_BLOCK_LIST,
                Map.of("pageNumber", 0, "pageSize", 10, "filter", Map.of()), adminToken);
        assertThat(statusOf(list)).isEqualTo("SUCCESS");
        assertThat(dataOf(list).valueStream().map(n -> n.get("ipAddress").asText()).toList())
                .contains("10.9.9.9");
        assertThat(body(list).get("totalElements").asLong()).isPositive();

        HttpResponse<String> unblock = postJson(URL_IP_BLOCK_CONTROLLER + ENDPOINT_IP_BLOCK_UNBLOCK,
                Map.of("id", "10.9.9.9"), adminToken);
        assertThat(statusOf(unblock)).isEqualTo("SUCCESS");
        assertThat(messageOf(unblock)).contains("unblocked successfully");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM blocked_ip WHERE ip_address = '10.9.9.9'",
                Integer.class)).isZero();
    }

    @Test
    @DisplayName("the unblock endpoint is exempt from the filter, so an admin cannot lock themselves out")
    void unblockEndpointIsExemptFromBlocking() {
        createUser("ipb_selfrescue", PASSWORD, "ADMIN");
        String adminToken = login("ipb_selfrescue", PASSWORD);
        blockThisIp();
        String ownIp = jdbc.queryForObject("SELECT ip_address FROM blocked_ip", String.class);

        // Every other path is 429 by now...
        assertThat(getJson(PROBE, adminToken).statusCode()).isEqualTo(429);

        // ...but the unblock endpoint still answers, which is the only way back.
        HttpResponse<String> unblock = postJson(URL_IP_BLOCK_CONTROLLER + ENDPOINT_IP_BLOCK_UNBLOCK,
                Map.of("id", ownIp), adminToken);
        assertThat(statusOf(unblock)).isEqualTo("SUCCESS");

        assertThat(statusOf(getJson(PROBE, adminToken))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("a successful login clears that user's accumulated IP records")
    void successfulLoginClearsTheUsersIpRecords() {
        createUser("ipb_forgiven", PASSWORD, "ADMIN");

        loginResponse("ipb_forgiven", "Wrong@Pass999");
        assertThat(blockedIpCount()).isEqualTo(1);

        assertThat(statusOf(loginResponse("ipb_forgiven", PASSWORD))).isEqualTo("SUCCESS");
        assertThat(blockedIpCount()).isZero();
    }

    @Test
    @DisplayName("unblock-user clears every IP record attributed to that username")
    void unblockUserClearsAllIpsForThatUsername() {
        createUser("ipb_multiip", PASSWORD, "ADMIN");
        String adminToken = login("ipb_multiip", PASSWORD);

        // The same account seen from three different sources - the case the
        // per-username unblock exists for, which a per-IP unblock cannot handle.
        for (String ip : new String[] { "10.1.1.1", "10.1.1.2", "10.1.1.3" }) {
            insertBlockedIp(ip, "ipb_blockeduser");
        }
        // Plus one belonging to somebody else, which must survive.
        insertBlockedIp("10.2.2.2", "ipb_someoneelse");

        HttpResponse<String> res = postJson(URL_IP_BLOCK_CONTROLLER + ENDPOINT_IP_BLOCK_UNBLOCK_USER,
                Map.of("id", "ipb_blockeduser"), adminToken);

        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        assertThat(messageOf(res)).isEqualTo("Unblocked 3 IP entries for user ipb_blockeduser");
        assertThat(countByUsername("ipb_blockeduser")).isZero();
        assertThat(countByUsername("ipb_someoneelse")).isEqualTo(1);
    }

    @Test
    @DisplayName("unblock-user on a username with no records reports nothing found rather than failing")
    void unblockUserWithNoRecordsIsNotAnError() {
        createUser("ipb_nothing", PASSWORD, "ADMIN");

        HttpResponse<String> res = postJson(URL_IP_BLOCK_CONTROLLER + ENDPOINT_IP_BLOCK_UNBLOCK_USER,
                Map.of("id", "ipb_nosuchuser"), login("ipb_nothing", PASSWORD));

        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        assertThat(messageOf(res)).isEqualTo("No blocked IP entries found for user ipb_nosuchuser");
    }

    private void insertBlockedIp(String ipAddress, String username) {
        jdbc.update("""
                INSERT INTO blocked_ip (ip_address, blocked_at, unblock_at, endpoint, username,
                                        is_active, failed_attempts, failed_login_attempts,
                                        failed_unauthenticated_attempts, last_failure_type,
                                        last_attempt_at, reason)
                VALUES (?, now(), now() + interval '24 hours', '/api/v3/auth/login', ?,
                        true, 5, 5, 0, 'LOGIN', now(), 'Invalid credentials')
                """, ipAddress, username);
    }

    private int countByUsername(String username) {
        return jdbc.queryForObject("SELECT count(*) FROM blocked_ip WHERE username = ?", Integer.class, username);
    }

    /** Drives the loopback IP over its unauthenticated-attempt threshold. */
    private void blockThisIp() {
        for (int i = 0; i < 3; i++) {
            getJson(PROBE);
        }
        assertThat(isIpActive()).isTrue();
    }

    private int blockedIpCount() {
        return jdbc.queryForObject("SELECT count(*) FROM blocked_ip", Integer.class);
    }

    private boolean isIpActive() {
        Boolean active = jdbc.queryForObject(
                "SELECT bool_or(is_active) FROM blocked_ip", Boolean.class);
        return Boolean.TRUE.equals(active);
    }
}
