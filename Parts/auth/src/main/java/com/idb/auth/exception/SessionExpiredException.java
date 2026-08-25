package com.idb.auth.exception;

import org.springframework.security.core.AuthenticationException;

/** Access token expired and no usable refresh token was presented. */
public class SessionExpiredException extends AuthenticationException {

    public SessionExpiredException(String message) {
        super(message);
    }
}
