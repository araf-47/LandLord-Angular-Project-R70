package com.idb.auth.constant;

/**
 * Categories of failed authentication attempt, each with its own IP-block
 * threshold.
 */
public enum AttemptType {
    /** Username/password login with wrong credentials. */
    LOGIN,

    /** Request to a protected resource with no credentials at all. */
    UNAUTHENTICATED,

    /** Request carrying a malformed or unverifiable JWT. */
    INVALID_JWT,

    /** Wrong OTP supplied during 2FA login or password reset. */
    INVALID_OTP
}
