package com.idb.auth.exception;

import org.springframework.security.core.AuthenticationException;

import lombok.Getter;

@Getter
public class IpBlockedException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    private final String ipAddress;
    private final long blockDurationHours;

    public IpBlockedException(String ipAddress, long blockDurationHours) {
        super("IP address is blocked due to too many failed authentication attempts");
        this.ipAddress = ipAddress;
        this.blockDurationHours = blockDurationHours;
    }
}
