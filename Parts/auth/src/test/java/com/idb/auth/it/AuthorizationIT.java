package com.idb.auth.it;

import static com.idb.auth.constant.AuthConstants.ENDPOINT_GET_USER_PERMISSIONS;
import static com.idb.auth.constant.AuthConstants.URL_PERMISSION_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_ROLE_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_TEST_CONTROLLER;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

/**
 * Authorization, as opposed to authentication: the URL-to-roles matrix loaded
 * from permissions.json, and the independent method-security layer.
 */
class AuthorizationIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Authz@Pass123";

    @BeforeEach
    void setUp() {
        truncateAll();
    }

    @Test
    @DisplayName("an ADMIN reaches an ADMIN-only URL and a USER is refused with 403")
    void adminOnlyUrlIsRestrictedToAdmin() {
        createUser("authz_admin", PASSWORD, "ADMIN");
        createUser("authz_user", PASSWORD, "USER");

        HttpResponse<String> asAdmin = getJson(URL_TEST_CONTROLLER + "/admin-only", login("authz_admin", PASSWORD));
        assertThat(asAdmin.statusCode()).isEqualTo(200);
        assertThat(statusOf(asAdmin)).isEqualTo("SUCCESS");

        HttpResponse<String> asUser = getJson(URL_TEST_CONTROLLER + "/admin-only", login("authz_user", PASSWORD));
        assertThat(asUser.statusCode()).isEqualTo(403);
        assertThat(statusOf(asUser)).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("a USER-only URL refuses an ADMIN - the matrix is not hierarchical")
    void userOnlyUrlRefusesAdmin() {
        createUser("authz_admin2", PASSWORD, "ADMIN");
        createUser("authz_user2", PASSWORD, "USER");

        HttpResponse<String> asUser = getJson(URL_TEST_CONTROLLER + "/user-only", login("authz_user2", PASSWORD));
        assertThat(asUser.statusCode()).isEqualTo(200);
        assertThat(statusOf(asUser)).isEqualTo("SUCCESS");

        // ADMIN is not a superset of USER: authorities are matched literally.
        HttpResponse<String> asAdmin = getJson(URL_TEST_CONTROLLER + "/user-only", login("authz_admin2", PASSWORD));
        assertThat(asAdmin.statusCode()).isEqualTo(403);
        assertThat(statusOf(asAdmin)).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("a URL granted to both roles admits both")
    void sharedUrlAdmitsBothRoles() {
        createUser("authz_admin3", PASSWORD, "ADMIN");
        createUser("authz_user3", PASSWORD, "USER");

        assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/any", login("authz_admin3", PASSWORD))))
                .isEqualTo("SUCCESS");
        assertThat(statusOf(getJson(URL_TEST_CONTROLLER + "/any", login("authz_user3", PASSWORD))))
                .isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("@PreAuthorize enforces on top of the URL rule, not instead of it")
    void methodSecurityIsASeparateLayer() {
        createUser("authz_admin4", PASSWORD, "ADMIN");
        createUser("authz_user4", PASSWORD, "USER");

        // permissions.json grants /method-secured to ADMIN and USER, so the URL
        // rule lets both through. The method's @PreAuthorize("hasAuthority('ADMIN')")
        // is what stops the USER - proving the two layers are independent.
        HttpResponse<String> asAdmin = getJson(URL_TEST_CONTROLLER + "/method-secured",
                login("authz_admin4", PASSWORD));
        assertThat(asAdmin.statusCode()).isEqualTo(200);
        assertThat(statusOf(asAdmin)).isEqualTo("SUCCESS");

        HttpResponse<String> asUser = getJson(URL_TEST_CONTROLLER + "/method-secured",
                login("authz_user4", PASSWORD));
        assertThat(statusOf(asUser)).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("get-user-permissions returns exactly the permissions granted to the caller's role")
    void userPermissionsReflectTheRoleMatrix() {
        createUser("authz_perm_user", PASSWORD, "USER");

        HttpResponse<String> res = getJson(URL_PERMISSION_CONTROLLER + ENDPOINT_GET_USER_PERMISSIONS,
                login("authz_perm_user", PASSWORD));

        assertThat(statusOf(res)).isEqualTo("SUCCESS");
        List<String> names = dataOf(res).valueStream().map(node -> node.get("name").asText()).sorted().toList();

        // Every USER grant from permissions.json, and nothing that is ADMIN-only.
        assertThat(names).containsExactly(
                "LOGOUT_ALL", "MANAGE_PASSWORD", "TEST_ANY", "TEST_METHOD_SECURED", "TEST_USER_ONLY",
                "TOGGLE_2FA", "VIEW_PERMISSIONS");
        assertThat(names).doesNotContain("LIST_ROLES", "MANAGE_IP_BLOCKS", "REGISTER_USER");
    }

    @Test
    @DisplayName("each returned permission carries its url and route from permissions.json")
    void permissionResponseCarriesUrlAndRoute() {
        createUser("authz_perm_admin", PASSWORD, "ADMIN");

        HttpResponse<String> res = getJson(URL_PERMISSION_CONTROLLER + ENDPOINT_GET_USER_PERMISSIONS,
                login("authz_perm_admin", PASSWORD));

        JsonNode listRoles = dataOf(res).valueStream()
                .filter(node -> "LIST_ROLES".equals(node.get("name").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("LIST_ROLES not granted to ADMIN: " + res.body()));

        assertThat(listRoles.get("url").asText()).isEqualTo(URL_ROLE_CONTROLLER + "/list");
        assertThat(listRoles.get("route").asText()).isEqualTo("/dashboard/roles");
        assertThat(listRoles.get("id").asText()).isNotBlank();
    }

    @Test
    @DisplayName("a wildcard URL rule covers every path beneath it")
    void wildcardRuleCoversSubPaths() {
        createUser("authz_admin5", PASSWORD, "ADMIN");
        createUser("authz_user5", PASSWORD, "USER");

        // permissions.json maps /api/v3/user-block/** to ADMIN only.
        assertThat(getJson("/api/v3/user-block/list", login("authz_user5", PASSWORD)).statusCode()).isEqualTo(403);
        assertThat(statusOf(getJson("/api/v3/user-block/list", login("authz_admin5", PASSWORD))))
                .isEqualTo("SUCCESS");
    }
}
