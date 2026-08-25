package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.idb.auth.common.util.ValidationUtil;

class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = { "a@b.co", "first.last+tag@sub.example.com", "x_y-z@example.io" })
    void acceptsValidEmails(String email) {
        assertThat(ValidationUtil.isValidEmail(email)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "no-at-sign", "a@b", "a@b.c", "@example.com", "a b@example.com", "a@@example.com" })
    void rejectsInvalidEmails(String email) {
        assertThat(ValidationUtil.isValidEmail(email)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "+8801717171717", "8801717171717", "+15551234567" })
    void acceptsE164Phones(String phone) {
        assertThat(ValidationUtil.isValidPhone(phone)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "not-a-phone", "+0123456789", "0123456789", "+880 1717 171717", "+123456789012345678" })
    void rejectsNonE164Phones(String phone) {
        assertThat(ValidationUtil.isValidPhone(phone)).isFalse();
    }
}
