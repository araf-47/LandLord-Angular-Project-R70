package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import com.idb.auth.util.AuthUtil;

class AuthUtilTest {

    private static String jwtWithPayload(String json) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString("{\"alg\":\"HS384\"}".getBytes(StandardCharsets.UTF_8))
                + "." + enc.encodeToString(json.getBytes(StandardCharsets.UTF_8))
                + ".signature-not-checked-here";
    }

    @Test
    @DisplayName("the subject is read out of the payload")
    void extractsSubject() {
        assertThat(AuthUtil.getUsernameFromAccessToken(jwtWithPayload("{\"sub\":\"alice\",\"iat\":1}")))
                .isEqualTo("alice");
    }

    @Test
    @DisplayName("a payload where another value contains the sub marker cannot smuggle a subject")
    void isNotFooledByAValueContainingTheSubMarker() {
        // The reference implementation did payload.split("\"sub\":\"")[1], so a
        // crafted claim ordered before the real one hijacked the lookup. Parsing as
        // JSON makes the real `sub` authoritative regardless of ordering.
        String payload = "{\"note\":\"\\\"sub\\\":\\\"attacker\\\"\",\"sub\":\"victim\"}";
        assertThat(AuthUtil.getUsernameFromAccessToken(jwtWithPayload(payload))).isEqualTo("victim");
    }

    @Test
    @DisplayName("a nested object carrying its own sub does not win over the top-level one")
    void nestedSubIsIgnored() {
        String payload = "{\"data\":{\"sub\":\"attacker\"},\"sub\":\"victim\"}";
        assertThat(AuthUtil.getUsernameFromAccessToken(jwtWithPayload(payload))).isEqualTo("victim");
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-a-jwt", "only.two", "a.b.c.d", "" })
    @DisplayName("anything that is not three dot-separated segments is refused")
    void rejectsMalformedStructure(String token) {
        assertThatThrownBy(() -> AuthUtil.getUsernameFromAccessToken(token))
                .isInstanceOf(InsufficientAuthenticationException.class)
                .hasMessageContaining("Invalid JWT token format");
    }

    @Test
    @DisplayName("a payload that is not JSON is refused rather than yielding a garbage subject")
    void rejectsNonJsonPayload() {
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString("h".getBytes(StandardCharsets.UTF_8))
                + "." + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString("not json at all".getBytes(StandardCharsets.UTF_8))
                + ".sig";
        assertThatThrownBy(() -> AuthUtil.getUsernameFromAccessToken(token))
                .isInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    @DisplayName("a payload with no sub, or a null sub, is refused")
    void rejectsMissingSubject() {
        assertThatThrownBy(() -> AuthUtil.getUsernameFromAccessToken(jwtWithPayload("{\"iat\":1}")))
                .isInstanceOf(InsufficientAuthenticationException.class)
                .hasMessageContaining("Username not found in token");
        assertThatThrownBy(() -> AuthUtil.getUsernameFromAccessToken(jwtWithPayload("{\"sub\":null}")))
                .isInstanceOf(InsufficientAuthenticationException.class);
        assertThatThrownBy(() -> AuthUtil.getUsernameFromAccessToken(jwtWithPayload("{\"sub\":\"\"}")))
                .isInstanceOf(InsufficientAuthenticationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "user1", "AliceBob99", "aaa" })
    void acceptsAlphanumericUsernames(String username) {
        assertThat(AuthUtil.isValidUsername(username)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "ab", "has_underscore", "has-dash", "has space", "has.dot", "e@mail" })
    void rejectsNonAlphanumericOrTooShortUsernames(String username) {
        assertThat(AuthUtil.isValidUsername(username)).isFalse();
    }

    @Test
    void rejectsBlankUsername() {
        assertThat(AuthUtil.isValidUsername(null)).isFalse();
        assertThat(AuthUtil.isValidUsername("   ")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "Aa1!aaaa", "Str0ng@Pass", "Xx9#xxxxxxxxxxx" })
    void acceptsStrongPasswords(String password) {
        assertThat(AuthUtil.isValidStrongPassword(password)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Aa1!aaa",              // 7 chars, one short
            "Aa1!aaaaaaaaaaaaa",    // 17 chars, one long
            "aa1!aaaa",             // no uppercase
            "AA1!AAAA",             // no lowercase
            "Aa!!aaaa",             // no digit
            "Aa11aaaa",             // no special character
            "Aa1! aaaa"             // contains whitespace
    })
    void rejectsWeakPasswords(String password) {
        assertThat(AuthUtil.isValidStrongPassword(password)).isFalse();
    }

    @Test
    @DisplayName("the ad-hoc AuthenticationException preserves both message and cause")
    void authenticationExceptionCarriesMessageAndCause() {
        RuntimeException cause = new RuntimeException("root");
        var ex = AuthUtil.getAuthenticationException("outer", cause);
        assertThat(ex.getMessage()).isEqualTo("outer");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
