package com.idb.auth.exception;

import org.springframework.security.core.AuthenticationException;

/** Token present but malformed, or signed with the wrong key. */
public class InvalidTokenInHeaderException extends AuthenticationException {

    public InvalidTokenInHeaderException(String message) {
        super(message);
    }

    public InvalidTokenInHeaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
