package com.idb.auth.common.util;

import static com.idb.auth.common.constant.CommonConstants.EMAIL_PATTERN;
import static com.idb.auth.common.constant.CommonConstants.PHONE_PATTERN;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static boolean isValidEmail(String value) {
        return StringUtil.isNotEmpty(value) && value.matches(EMAIL_PATTERN);
    }

    public static boolean isValidPhone(String value) {
        return StringUtil.isNotEmpty(value) && value.matches(PHONE_PATTERN);
    }
}
