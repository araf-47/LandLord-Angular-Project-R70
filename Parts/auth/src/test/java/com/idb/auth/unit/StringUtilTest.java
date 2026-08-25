package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.idb.auth.common.util.StringUtil;

class StringUtilTest {

    @ParameterizedTest
    @CsvSource({
            // current,   new,       expected
            "1.0.0.0,     1.0.0.1,   true",   // patch bump
            "1.0.0.1,     1.0.1.0,   true",   // minor bump
            "1.0.0.1,     2.0.0.0,   true",   // major bump
            "1.0.0.1,     1.0.0.1,   false",  // identical
            "1.0.0.2,     1.0.0.1,   false",  // downgrade
            "2.0.0.0,     1.9.9.9,   false",  // downgrade on the leading segment
            "1.0.0.1,     1.0,       false",  // shorter new version is not an upgrade
            "1.0,         1.0.0.1,   true",   // longer new version with equal prefix IS an upgrade
            ",            1.0.0.1,   true",   // no recorded version: initial import
            "1.0.0.1,     ,          false",  // no new version: nothing to compare
    })
    @DisplayName("isUpdatedVersion drives whether permissions.json gets re-imported")
    void isUpdatedVersion(String current, String next, boolean expected) {
        assertThat(StringUtil.isUpdatedVersion(current, next)).isEqualTo(expected);
    }

    @Test
    @DisplayName("a longer new version with an equal prefix counts as an upgrade")
    void trailingSegmentsCount() {
        // Guards a regression: comparing only min(len) segments and then returning
        // false would treat 1.0 -> 1.0.0.1 as "up to date" and never re-import.
        assertThat(StringUtil.isUpdatedVersion("1.0", "1.0.0.1")).isTrue();
        assertThat(StringUtil.isUpdatedVersion("1.0.0.1", "1.0")).isFalse();
    }

    @Test
    @DisplayName("format never throws on a bad format string, it returns the template")
    void formatIsFailSafe() {
        assertThat(StringUtil.format("user %s locked", "bob")).isEqualTo("user bob locked");
        // A mismatched specifier must not blow up an error path that is itself
        // already handling a failure.
        assertThat(StringUtil.format("user %d locked", "bob")).isEqualTo("user %d locked");
        assertThat(StringUtil.format(null, "bob")).isNull();
        assertThat(StringUtil.format("", "bob")).isEmpty();
    }

    @Test
    @DisplayName("blank/empty predicates distinguish whitespace from empty")
    void blankAndEmpty() {
        assertThat(StringUtil.isEmpty("")).isTrue();
        assertThat(StringUtil.isEmpty(null)).isTrue();
        assertThat(StringUtil.isEmpty("   ")).isFalse();
        assertThat(StringUtil.isBlank("   ")).isTrue();
        assertThat(StringUtil.isNotBlank(" x ")).isTrue();
        assertThat(StringUtil.isNotEmpty("   ")).isTrue();
    }

    @Test
    @DisplayName("generateUuid returns distinct parseable ids")
    void generateUuid() {
        String a = StringUtil.generateUuid();
        String b = StringUtil.generateUuid();
        assertThat(a).isNotEqualTo(b);
        assertThat(java.util.UUID.fromString(a)).isNotNull();
    }
}
