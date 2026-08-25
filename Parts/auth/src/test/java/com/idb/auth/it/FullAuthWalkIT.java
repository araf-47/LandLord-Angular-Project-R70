package com.idb.auth.it;

import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_CLEAR_OTP_CACHE;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_GENERATE_OTP;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_LOGIN;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_REGISTER;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_TOGGLE_2FA;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_UPDATE;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_CHANGE_PASSWORD;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_GET_USER_PERMISSIONS;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_LOGOUT_ALL;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_ROLE_PERMISSIONS;
import static com.idb.auth.constant.AuthConstants.URL_AUTH_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_PERMISSION_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_TEST_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_USER_CONTROLLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.idb.auth.common.dto.MailInfo;
import com.idb.auth.common.exception.TraceableException;

/**
 * One continuous account lifecycle driven <b>entirely over HTTP</b>: seeded admin
 * → register → authenticate → authorize → promote → re-authorize → enable 2FA →
 * revoke → OTP login → change password → disable 2FA.
 *
 * <p>The other IT classes each isolate one mechanism and use JDBC fixtures to get
 * there quickly. This one deliberately does not: every state transition goes
 * through a real endpoint, so it catches breakage that only shows up when the
 * pieces are chained - a cache that goes stale between two calls, a role change
 * that does not reach an already-issued token, an OTP counter that survives a step
 * it should not.
 *
 * <p>JDBC is used for exactly two things, both reads or setup that has no endpoint:
 * seeding the default admin the way startup does (which {@code truncateAll} just
 * removed), and looking up ids the API does not expose.
 */
class FullAuthWalkIT extends AbstractIntegrationTest {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PW = "Admin@12345";
    private static final String USER = "walkUser";
    private static final String USER_PW = "Walk@Pass123";
    private static final String USER_PW2 = "Walk@Pass456";

    @BeforeEach
    void setUp() {
        truncateAll();
        seedDefaultAdmin();
    }

    @Test
    @DisplayName("full lifecycle: seeded admin -> register -> authorize -> promote -> 2FA -> revoke -> reset")
    void fullAuthWalk() throws TraceableException {
        // ---------- 1. the bootstrapped administrator can authenticate ----------
        HttpResponse<String> adminLogin = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", ADMIN, "password", ADMIN_PW));
        assertThat(statusOf(adminLogin)).isEqualTo("SUCCESS");
        String adminToken = dataOf(adminLogin).get("accessToken").asText();

        // ---------- 2. the admin creates a USER over the API ----------
        HttpResponse<String> register = postJson(URL_USER_CONTROLLER + ENDPOINT_REGISTER, payload(
                "username", USER,
                "password", USER_PW,
                "email", "walk_user@test.local",
                "phone", "+8801799000001",
                "roles", List.of("USER")), adminToken);
        assertThat(statusOf(register)).isEqualTo("SUCCESS");
        assertThat(dataOf(register).asText()).isEqualTo(USER);

        // ---------- 3. that user authenticates with the credentials just set ----------
        HttpResponse<String> userLogin = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", USER, "password", USER_PW));
        assertThat(statusOf(userLogin)).isEqualTo("SUCCESS");
        String userToken = dataOf(userLogin).get("accessToken").asText();
        String userRefresh = dataOf(userLogin).get("refreshToken").asText();

        // ---------- 4. authorization matches the granted role, both ways ----------
        assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/user-only", userToken))).isEqualTo("SUCCESS");
        assertThat(getJson(URL_TEST_CONTROLLER + "/admin-only", userToken).statusCode()).isEqualTo(403);
        assertThat(authoritiesOf(userToken)).containsExactly("USER");

        // ---------- 5. the user reads their own permission set ----------
        assertThat(permissionNames(userToken))
                .contains("TEST_USER_ONLY", "MANAGE_PASSWORD")
                .doesNotContain("TEST_ADMIN_ONLY", "REGISTER_USER");

        // ---------- 6. the admin promotes the user to ADMIN ----------
        HttpResponse<String> promote = postJson(URL_USER_CONTROLLER + ENDPOINT_UPDATE, payload(
                "id", userId(USER),
                "roles", List.of("ADMIN")), adminToken);
        assertThat(statusOf(promote)).isEqualTo("SUCCESS");
        assertThat(dataOf(promote).asText()).isEqualTo(USER);

        // ---------- 7. the promotion reaches the ALREADY-ISSUED token ----------
        // Authorities are re-read from the user on every request rather than being
        // baked into the JWT, so no re-login is needed. This is the assertion that
        // catches a stale `user` cache after an out-of-band role change.
        assertThat(authoritiesOf(userToken)).containsExactly("ADMIN");
        assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/admin-only", userToken))).isEqualTo("SUCCESS");
        assertThat(getJson(URL_TEST_CONTROLLER + "/user-only", userToken).statusCode()).isEqualTo(403);
        assertThat(permissionNames(userToken)).contains("TEST_ADMIN_ONLY", "REGISTER_USER");

        // ---------- 8. now an admin, the user edits a role's permission set ----------
        // A throwaway role, so this cannot disturb the ADMIN/USER matrix that the
        // rest of the suite shares (permissions are imported once, at startup).
        long spareRole = ensureRole("WALK_SPARE_ROLE");
        List<Long> twoPermissions = jdbc.queryForList(
                "SELECT id FROM permissions ORDER BY id LIMIT 2", Long.class);

        HttpResponse<String> grant = postJson(URL_PERMISSION_CONTROLLER + ENDPOINT_ROLE_PERMISSIONS,
                payload("roleId", spareRole, "permissionIds", twoPermissions), userToken);
        assertThat(statusOf(grant)).isEqualTo("SUCCESS");
        assertThat(messageOf(grant)).isEqualTo("Permissions updated successfully");
        assertThat(rolePermissionCount(spareRole)).isEqualTo(2);

        // Replacing, not appending: the previous set is cleared first.
        HttpResponse<String> replace = postJson(URL_PERMISSION_CONTROLLER + ENDPOINT_ROLE_PERMISSIONS,
                payload("roleId", spareRole, "permissionIds", twoPermissions.subList(0, 1)), userToken);
        assertThat(statusOf(replace)).isEqualTo("SUCCESS");
        assertThat(rolePermissionCount(spareRole)).isEqualTo(1);

        // ---------- 9. the user turns on their own second factor ----------
        assertThat(statusOf(postJson(URL_USER_CONTROLLER + ENDPOINT_TOGGLE_2FA, Map.of("id", true), userToken)))
                .isEqualTo("SUCCESS");

        // ---------- 10. logging out everywhere kills both tokens ----------
        awaitNextSecond();
        assertThat(statusOf(postJson(URL_USER_CONTROLLER + ENDPOINT_LOGOUT_ALL, Map.of(), userToken)))
                .isEqualTo("SUCCESS");
        assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/any", userToken))).isEqualTo("SESSION_EXPIRED");
        assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/any", expiredTokenFor(USER), userRefresh)))
                .isEqualTo("SESSION_EXPIRED");

        // ---------- 11. re-login now demands the second factor ----------
        awaitNextSecond();
        HttpResponse<String> challenge = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", USER, "password", USER_PW));
        assertThat(statusOf(challenge)).isEqualTo("OTP_REQUIRED");
        assertThat(body(challenge).get("data")).isNull();

        String otp = captureOtp();
        HttpResponse<String> otpLogin = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", USER, "password", USER_PW, "otp", otp));
        assertThat(statusOf(otpLogin)).isEqualTo("SUCCESS");
        userToken = dataOf(otpLogin).get("accessToken").asText();

        // ---------- 12. clearing the OTP cache resets the generation budget ----------
        // Three generations per window is the cap. Burn it, confirm the cap bites,
        // then clear and confirm generation works again - which is the only
        // observable proof this endpoint does anything.
        for (int i = 0; i < 3; i++) {
            postJson(URL_AUTH_CONTROLLER + ENDPOINT_GENERATE_OTP, Map.of("id", USER));
        }
        assertThat(messageOf(postJson(URL_AUTH_CONTROLLER + ENDPOINT_GENERATE_OTP, Map.of("id", USER))))
                .contains("Too many OTP requests");

        assertThat(statusOf(postJson(URL_USER_CONTROLLER + ENDPOINT_CLEAR_OTP_CACHE,
                Map.of("id", USER), userToken))).isEqualTo("SUCCESS");

        assertThat(statusOf(postJson(URL_AUTH_CONTROLLER + ENDPOINT_GENERATE_OTP, Map.of("id", USER))))
                .isEqualTo("SUCCESS");
        String changeOtp = captureOtp();

        // ---------- 13. the user changes their own password ----------
        HttpResponse<String> change = postJson(URL_USER_CONTROLLER + ENDPOINT_CHANGE_PASSWORD, Map.of(
                "password", USER_PW2, "oldPassword", USER_PW, "otp", changeOtp), userToken);
        assertThat(statusOf(change)).isEqualTo("SUCCESS");

        // The password hash is the JWT signing key, so the live token dies with it.
        assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/any", userToken))).isNotEqualTo("SUCCESS");
        assertThat(statusOf(postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", USER, "password", USER_PW)))).isEqualTo("BAD_CREDENTIALS");

        // ---------- 14. the new password still goes through 2FA ----------
        assertThat(statusOf(postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", USER, "password", USER_PW2)))).isEqualTo("OTP_REQUIRED");
        String finalOtp = captureOtp();
        HttpResponse<String> finalLogin = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", USER, "password", USER_PW2, "otp", finalOtp));
        assertThat(statusOf(finalLogin)).isEqualTo("SUCCESS");
        userToken = dataOf(finalLogin).get("accessToken").asText();

        // ---------- 15. turning 2FA back off restores single-factor login ----------
        assertThat(statusOf(postJson(URL_USER_CONTROLLER + ENDPOINT_TOGGLE_2FA, Map.of("id", false), userToken)))
                .isEqualTo("SUCCESS");
        HttpResponse<String> plainLogin = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", USER, "password", USER_PW2));
        assertThat(statusOf(plainLogin)).isEqualTo("SUCCESS");
        assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/any",
                dataOf(plainLogin).get("accessToken").asText()))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("editing role_permissions does not change URL enforcement until restart")
    void urlEnforcementIsFixedAtStartup() {
        String adminToken = postLoginToken(ADMIN, ADMIN_PW);

        // Strip every permission from the USER role at runtime...
        long userRole = ensureRole("USER");
        List<Long> before = jdbc.queryForList(
                "SELECT permission_id FROM role_permissions WHERE role_id = ?", Long.class, userRole);
        try {
            postJson(URL_USER_CONTROLLER + ENDPOINT_REGISTER, payload(
                    "username", "walkFixed", "password", USER_PW, "email", "walk_fixed@test.local",
                    "phone", "+8801799000002", "roles", List.of("USER")), adminToken);
            String userToken = postLoginToken("walkFixed", USER_PW);

            HttpResponse<String> stripped = postJson(URL_PERMISSION_CONTROLLER + ENDPOINT_ROLE_PERMISSIONS,
                    payload("roleId", userRole, "permissionIds", List.of(before.get(0))), adminToken);
            assertThat(statusOf(stripped)).isEqualTo("SUCCESS");

            // ...and the URL rules still hold, because SecurityConfig registered its
            // matchers from permissions.json once, at startup. Only the DB-driven
            // view changes. Worth pinning: it means a permission edit is not a live
            // security control, and an operator expecting otherwise would be wrong.
            assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/user-only", userToken))).isEqualTo("SUCCESS");
            assertThat(permissionNames(userToken)).hasSize(1);
        } finally {
            // Restore, or every later test in this shared context inherits the damage.
            jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", userRole);
            for (Long permissionId : before) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)",
                        userRole, permissionId);
            }
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** Recreates the row {@code UserServiceImpl.init()} writes at startup. */
    private void seedDefaultAdmin() {
        long adminRole = ensureRole("ADMIN");
        jdbc.update("""
                INSERT INTO users (username, password, email, phone, is_active, failed_login_attempts,
                                   account_locked, two_factor_enabled, created_at, created_by)
                VALUES (?, ?, 'admin@test.local', '+8801700000000', true, 0, false, false, now(), 'SYSTEM')
                """, ADMIN, passwordEncoder.encode(ADMIN_PW));
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT id, ? FROM users WHERE username = ?",
                adminRole, ADMIN);
    }

    private String postLoginToken(String username, String password) {
        HttpResponse<String> res = postJson(URL_AUTH_CONTROLLER + ENDPOINT_LOGIN,
                Map.of("username", username, "password", password));
        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        return dataOf(res).get("accessToken").asText();
    }

    private List<String> authoritiesOf(String token) {
        HttpResponse<String> res = getJson(URL_TEST_CONTROLLER + "/any", token);
        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        return dataOf(res).get("authorities").valueStream().map(n -> n.asText()).toList();
    }

    private List<String> permissionNames(String token) {
        HttpResponse<String> res = getJson(URL_PERMISSION_CONTROLLER + ENDPOINT_GET_USER_PERMISSIONS, token);
        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        return dataOf(res).valueStream().map(n -> n.get("name").asText()).toList();
    }

    private long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private int rolePermissionCount(long roleId) {
        return jdbc.queryForObject("SELECT count(*) FROM role_permissions WHERE role_id = ?",
                Integer.class, roleId);
    }

    private String captureOtp() throws TraceableException {
        ArgumentCaptor<MailInfo> captor = ArgumentCaptor.forClass(MailInfo.class);
        verify(mailService, atLeastOnce()).sendTemplatedEmail(captor.capture());
        return captor.getValue().getTemplateModel().get("otp").toString();
    }
}
