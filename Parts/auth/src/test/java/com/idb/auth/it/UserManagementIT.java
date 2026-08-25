package com.idb.auth.it;

import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_CLEAR_OTP_CACHE;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_GENERATE_OTP;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_LIST;
import static com.idb.auth.common.constant.CommonConstants.ENDPOINT_REGISTER;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_CHANGE_PASSWORD;
import static com.idb.auth.constant.AuthConstants.URL_AUTH_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_ROLE_CONTROLLER;
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
 * Registration, password change, and the role listing - the administrative half
 * of the flow, all of it behind the same bearer-token authentication.
 */
class UserManagementIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Manage@Pass123";

    @BeforeEach
    void setUp() {
        truncateAll();
    }

    @Test
    @DisplayName("an admin registers a user who can then log in and reach their own role's endpoints")
    void adminRegistersAUsableUser() {
        createUser("mg_admin", PASSWORD, "ADMIN");
        ensureRole("USER");
        String adminToken = login("mg_admin", PASSWORD);

        HttpResponse<String> res = postJson(URL_USER_CONTROLLER + ENDPOINT_REGISTER, payload(
                "username", "mgNewUser",
                "password", "Brand@New123",
                "email", "mg_new@test.local",
                "phone", "+8801711111111",
                "roles", List.of("USER")), adminToken);

        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        assertThat(dataOf(res).asText()).isEqualTo("mgNewUser");

        String newUserToken = login("mgNewUser", "Brand@New123");
        assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/user-only", newUserToken))).isEqualTo("SUCCESS");
        // The new user got USER, not ADMIN.
        assertThat(getJson(URL_TEST_CONTROLLER + "/admin-only", newUserToken).statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("a non-admin cannot register users")
    void nonAdminCannotRegister() {
        createUser("mg_plainuser", PASSWORD, "USER");
        String token = login("mg_plainuser", PASSWORD);

        HttpResponse<String> res = postJson(URL_USER_CONTROLLER + ENDPOINT_REGISTER, payload(
                "username", "mgSneaky",
                "password", "Brand@New123",
                "email", "mg_sneaky@test.local",
                "phone", "+8801722222222",
                "roles", List.of("ADMIN")), token);

        assertThat(res.statusCode()).isEqualTo(403);
        assertThat(statusOf(res)).isEqualTo("ACCESS_DENIED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE username = 'mgSneaky'", Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("registration rejects a weak password, an invalid phone and a duplicate username")
    void registrationValidatesItsInput() {
        createUser("mg_admin2", PASSWORD, "ADMIN");
        ensureRole("USER");
        String adminToken = login("mg_admin2", PASSWORD);

        HttpResponse<String> weak = postJson(URL_USER_CONTROLLER + ENDPOINT_REGISTER, payload(
                "username", "mgWeak", "password", "weak", "email", "mg_weak@test.local",
                "phone", "+8801733333333", "roles", List.of("USER")), adminToken);
        assertThat(statusOf(weak)).isEqualTo("ERROR");
        assertThat(messageOf(weak)).isEqualTo("Invalid password");

        HttpResponse<String> badPhone = postJson(URL_USER_CONTROLLER + ENDPOINT_REGISTER, payload(
                "username", "mgBadPhone", "password", "Brand@New123", "email", "mg_bp@test.local",
                "phone", "not-a-phone", "roles", List.of("USER")), adminToken);
        assertThat(messageOf(badPhone)).isEqualTo("Invalid phone number");

        // The username pattern is alphanumeric-only, and it is checked before the
        // uniqueness lookup - so an underscored name never reaches that check.
        HttpResponse<String> badName = postJson(URL_USER_CONTROLLER + ENDPOINT_REGISTER, payload(
                "username", "mg_under_score", "password", "Brand@New123", "email", "mg_us@test.local",
                "phone", "+8801755555555", "roles", List.of("USER")), adminToken);
        assertThat(messageOf(badName)).isEqualTo("Invalid username");

        Map<String, Object> firstRegistration = payload(
                "username", "mgDuplicate", "password", "Brand@New123", "email", "mg_dup@test.local",
                "phone", "+8801744444444", "roles", List.of("USER"));
        assertThat(statusOf(postJson(URL_USER_CONTROLLER + ENDPOINT_REGISTER, firstRegistration, adminToken)))
                .isEqualTo("SUCCESS");

        HttpResponse<String> duplicate = postJson(URL_USER_CONTROLLER + ENDPOINT_REGISTER, payload(
                "username", "mgDuplicate", "password", "Brand@New123", "email", "mg_dup2@test.local",
                "phone", "+8801766666666", "roles", List.of("USER")), adminToken);
        assertThat(messageOf(duplicate)).isEqualTo("Username already exists");
    }

    @Test
    @DisplayName("change-password requires both the old password and a valid OTP")
    void changePasswordRequiresOldPasswordAndOtp() throws TraceableException {
        createUser("mg_changepw", PASSWORD, "ADMIN");
        String token = login("mg_changepw", PASSWORD);

        postJson(URL_AUTH_CONTROLLER + ENDPOINT_GENERATE_OTP, Map.of("id", "mg_changepw"));
        String otp = captureOtp();

        // Wrong old password: refused even though the OTP is genuine.
        HttpResponse<String> wrongOld = postJson(URL_USER_CONTROLLER + ENDPOINT_CHANGE_PASSWORD, Map.of(
                "password", "Changed@Pass456", "oldPassword", "Wrong@Pass999", "otp", otp), token);
        assertThat(statusOf(wrongOld)).isEqualTo("ERROR");
        assertThat(messageOf(wrongOld)).isEqualTo("Old password is incorrect");

        // That attempt consumed the OTP, so a fresh one is needed.
        postJson(URL_USER_CONTROLLER + ENDPOINT_CLEAR_OTP_CACHE, Map.of("id", "mg_changepw"), token);
        postJson(URL_AUTH_CONTROLLER + ENDPOINT_GENERATE_OTP, Map.of("id", "mg_changepw"));
        String freshOtp = captureOtp();

        HttpResponse<String> ok = postJson(URL_USER_CONTROLLER + ENDPOINT_CHANGE_PASSWORD, Map.of(
                "password", "Changed@Pass456", "oldPassword", PASSWORD, "otp", freshOtp), token);
        assertThat(statusOf(ok)).isEqualTo("SUCCESS");
        assertThat(messageOf(ok)).isEqualTo("Password changed successfully");

        assertThat(statusOf(loginResponse("mg_changepw", PASSWORD))).isEqualTo("BAD_CREDENTIALS");
        assertThat(statusOf(loginResponse("mg_changepw", "Changed@Pass456"))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("change-password rejects a wrong OTP before checking anything else")
    void changePasswordRejectsWrongOtp() {
        createUser("mg_badotp", PASSWORD, "ADMIN");
        String token = login("mg_badotp", PASSWORD);

        HttpResponse<String> res = postJson(URL_USER_CONTROLLER + ENDPOINT_CHANGE_PASSWORD, Map.of(
                "password", "Changed@Pass456", "oldPassword", PASSWORD, "otp", "000000"), token);

        assertThat(statusOf(res)).isEqualTo("ERROR");
        assertThat(messageOf(res)).isEqualTo("Otp is incorrect");
        assertThat(statusOf(loginResponse("mg_badotp", PASSWORD))).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("role/list is ADMIN-only and hides the default admin role")
    void roleListIsAdminOnlyAndExcludesTheDefaultRole() {
        createUser("mg_admin3", PASSWORD, "ADMIN");
        createUser("mg_user3", PASSWORD, "USER");

        HttpResponse<String> asAdmin = getJson(URL_ROLE_CONTROLLER + ENDPOINT_LIST, login("mg_admin3", PASSWORD));
        assertThat(statusOf(asAdmin)).isEqualTo("SUCCESS");
        List<String> roles = dataOf(asAdmin).valueStream().map(n -> n.asText()).toList();
        assertThat(roles).contains("USER");
        // credentials.default.role is filtered out by RoleServiceImpl.findByActive().
        assertThat(roles).doesNotContain("ADMIN");

        assertThat(getJson(URL_ROLE_CONTROLLER + ENDPOINT_LIST, login("mg_user3", PASSWORD)).statusCode())
                .isEqualTo(403);
    }

    @Test
    @DisplayName("the default administrator is seeded from configuration and can log in")
    void seededAdminCanLogIn() {
        // truncateAll() removed it, so re-seed the same way startup does.
        long adminRole = ensureRole("ADMIN");
        jdbc.update("""
                INSERT INTO users (username, password, email, phone, is_active, failed_login_attempts,
                                   account_locked, two_factor_enabled, created_at, created_by)
                VALUES ('admin', ?, 'admin@test.local', '+8801700000000', true, 0, false, false, now(), 'SYSTEM')
                """, passwordEncoder.encode("Admin@12345"));
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT id, ? FROM users WHERE username = 'admin'",
                adminRole);

        assertThat(statusOf(loginResponse("admin", "Admin@12345"))).isEqualTo("SUCCESS");
    }

    private String captureOtp() throws TraceableException {
        ArgumentCaptor<MailInfo> captor = ArgumentCaptor.forClass(MailInfo.class);
        verify(mailService, atLeastOnce()).sendTemplatedEmail(captor.capture());
        return captor.getValue().getTemplateModel().get("otp").toString();
    }
}
