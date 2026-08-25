package com.idb.auth.exception;

import static com.idb.auth.common.constant.OperationStatus.OTP_REQUIRED;

import com.idb.auth.common.dto.response.ApiResponse;

import lombok.Getter;

/** Credentials were correct but the account requires a second factor. */
@Getter
public class OtpRequiredException extends RuntimeException {

    private final ApiResponse<?> response;

    public OtpRequiredException(String message) {
        super(message);
        this.response = ApiResponse.builder().status(OTP_REQUIRED).message(message).build();
    }
}
