package com.idb.auth.common.util;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public final class StringUtil {

    private StringUtil() {
    }

    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Dotted-version comparison used to decide whether permissions.json has to be
     * re-imported. Returns true when newVersion is strictly greater.
     */
    public static boolean isUpdatedVersion(String currentVersion, String newVersion) {
        if (isEmpty(newVersion)) {
            return false;
        }
        if (isEmpty(currentVersion)) {
            return true;
        }

        String[] currentParts = currentVersion.split("\\.");
        String[] newParts = newVersion.split("\\.");

        int length = Math.min(currentParts.length, newParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = Integer.parseInt(currentParts[i]);
            int newPart = Integer.parseInt(newParts[i]);

            if (newPart > currentPart) {
                return true;
            } else if (newPart < currentPart) {
                return false;
            }
        }

        return newParts.length > currentParts.length;
    }

    public static boolean isEmpty(String value) {
        return StringUtils.isEmpty(value);
    }

    public static boolean isNotEmpty(String value) {
        return StringUtils.isNotEmpty(value);
    }

    public static boolean isBlank(String value) {
        return StringUtils.isBlank(value);
    }

    public static boolean isNotBlank(String value) {
        return StringUtils.isNotBlank(value);
    }

    public static String format(String message, Object... args) {
        if (isEmpty(message)) {
            return message;
        }
        try {
            return String.format(message, args);
        } catch (Exception e) {
            return message;
        }
    }
}
