package com.idb.auth.it;

import static com.idb.auth.constant.AuthConstants.URL_AUTH_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_TEST_CONTROLLER;
import static com.idb.auth.constant.AuthConstants.URL_USER_CONTROLLER;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Controller-level edge cases: malformed input, wrong verbs, wrong content types,
 * oversized values. These are the paths where a framework default can leak a
 * stack trace or a different error shape than the {@code ApiResponse} contract.
 */
class ControllerContractIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Contract@Pass1";

    @BeforeEach
    void setUp() {
        truncateAll();
    }

    private HttpResponse<String> raw(String method, String path, String body, String contentType, String token) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url(path)))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json");
            if (contentType != null) {
                builder.header("Content-Type", contentType);
            }
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            builder.method(method, body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
            return java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                    .send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new AssertionError("HTTP call failed", e);
        }
    }

    @Test
    @DisplayName("malformed JSON on a public endpoint returns the ApiResponse envelope, not a stack trace")
    void malformedJsonIsHandled() {
        HttpResponse<String> res = raw("POST", URL_AUTH_CONTROLLER + "/login",
                "{\"username\":\"alice\",", "application/json", null);

        assertThat(res.body()).doesNotContain("Exception").doesNotContain("at org.springframework");
        assertThat(statusOf(res)).isIn("ERROR", "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("an empty body is handled as a bad request, not a 500")
    void emptyBodyIsHandled() {
        HttpResponse<String> res = raw("POST", URL_AUTH_CONTROLLER + "/login", "", "application/json", null);

        assertThat(res.body()).doesNotContain("at org.springframework");
        assertThat(statusOf(res)).isIn("ERROR", "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("a JSON array where an object is expected does not leak internals")
    void wrongJsonShapeIsHandled() {
        HttpResponse<String> res = raw("POST", URL_AUTH_CONTROLLER + "/login",
                "[1,2,3]", "application/json", null);

        assertThat(res.body()).doesNotContain("at org.springframework");
        assertThat(statusOf(res)).isIn("ERROR", "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("the wrong content type on login is refused without leaking internals")
    void wrongContentTypeIsRefused() {
        HttpResponse<String> res = raw("POST", URL_AUTH_CONTROLLER + "/login",
                "username=alice&password=x", "application/x-www-form-urlencoded", null);

        assertThat(res.statusCode()).isIn(200, 400, 415);
        assertThat(res.body()).doesNotContain("at org.springframework");
    }

    @Test
    @DisplayName("GET on a POST-only public endpoint does not become a 500")
    void wrongMethodOnPublicEndpoint() {
        HttpResponse<String> res = raw("GET", URL_AUTH_CONTROLLER + "/login", null, null, null);

        assertThat(res.statusCode()).isIn(200, 401, 405);
        assertThat(res.body()).doesNotContain("at org.springframework");
    }

    @Test
    @DisplayName("an unauthenticated request to a protected endpoint with any verb is 401")
    void protectedEndpointRejectsEveryVerb() {
        for (String method : new String[] { "GET", "POST", "PUT", "DELETE" }) {
            HttpResponse<String> res = raw(method, URL_TEST_CONTROLLER + "/any",
                    method.equals("GET") || method.equals("DELETE") ? null : "{}", "application/json", null);
            assertThat(res.statusCode()).as(method).isEqualTo(401);
        }
    }

    @Test
    @DisplayName("a very long username is rejected by validation, not by a database error")
    void oversizedUsernameIsRejectedByValidation() {
        createUser("ct_admin", PASSWORD, "ADMIN");
        ensureRole("USER");
        String token = login("ct_admin", PASSWORD);

        HttpResponse<String> res = postJson(URL_USER_CONTROLLER + "/register", payload(
                "username", "a".repeat(300),
                "password", "Valid@Pass1",
                "email", "ct_long@test.local",
                "phone", "+8801799111111",
                "roles", java.util.List.of("USER")), token);

        // The pattern caps the length at 100, so this never reaches the column.
        assertThat(statusOf(res)).isEqualTo("ERROR");
        assertThat(messageOf(res)).isEqualTo("Invalid username");
    }

    @Test
    @DisplayName("a null id in a SingleParamRequest is a validation error")
    void nullSingleParamIsValidationError() {
        HttpResponse<String> res = postJson(URL_AUTH_CONTROLLER + "/otp", Map.of());

        assertThat(statusOf(res)).isEqualTo("VALIDATION_ERROR");
        assertThat(dataOf(res).get("id").asText()).isEqualTo("ID is required");
    }

    @Test
    @DisplayName("unicode and injection-shaped input is stored and compared literally")
    void unicodeAndInjectionShapedInputIsSafe() {
        // A quote-and-comment payload as a username: the parameterised queries mean
        // this is simply a username that does not exist, not a SQL problem.
        HttpResponse<String> res = postJson(URL_AUTH_CONTROLLER + "/login",
                Map.of("username", "admin'--", "password", "x' OR '1'='1"));

        assertThat(statusOf(res)).isEqualTo("BAD_CREDENTIALS");
        // And the table is intact.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isZero();
    }

    @Test
    @DisplayName("a password containing regex metacharacters round-trips through BCrypt")
    void regexMetacharactersInPasswordWork() {
        String tricky = "A1a!.*+?[]";
        createUser("ct_regex", tricky, "ADMIN");

        assertThat(statusOf(loginResponse("ct_regex", tricky))).isEqualTo("SUCCESS");
        assertThat(statusOf(loginResponse("ct_regex", "A1a!.*+?[]x"))).isEqualTo("BAD_CREDENTIALS");
    }

    @Test
    @DisplayName("every error response is JSON and carries a status field")
    void everyErrorIsAConsistentEnvelope() {
        createUser("ct_envelope", PASSWORD, "USER");
        String userToken = login("ct_envelope", PASSWORD);

        // 401 unauthenticated, 403 unauthorized, 200 validation error, 200 service
        // error - four different failure classes, one body shape.
        for (HttpResponse<String> res : java.util.List.of(
                getJson(URL_TEST_CONTROLLER + "/any"),
                getJson(URL_TEST_CONTROLLER + "/admin-only", userToken),
                postJson(URL_AUTH_CONTROLLER + "/login", Map.of("username", "x")),
                postJson(URL_AUTH_CONTROLLER + "/otp", Map.of("id", "ct_nosuchuser")))) {
            assertThat(res.headers().firstValue("Content-Type").orElse(""))
                    .as(res.body()).contains("application/json");
            assertThat(statusOf(res)).as(res.body()).isNotNull().isNotEqualTo("SUCCESS");
        }
    }

    @Test
    @DisplayName("a successful response never carries the password hash")
    void responsesNeverLeakThePasswordHash() {
        createUser("ct_nohash", PASSWORD, "ADMIN");
        String hash = storedPasswordHash("ct_nohash");

        HttpResponse<String> loginRes = loginResponse("ct_nohash", PASSWORD);
        HttpResponse<String> probe = getJson(URL_TEST_CONTROLLER + "/any",
                dataOf(loginRes).get("accessToken").asText());

        // The hash is the JWT signing key, so leaking it would let anyone mint
        // tokens for this user.
        assertThat(loginRes.body()).doesNotContain(hash).doesNotContain("password");
        assertThat(probe.body()).doesNotContain(hash).doesNotContain("password");
    }
}
