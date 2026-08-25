package com.idb.auth.exception;

import static com.idb.auth.common.constant.OperationStatus.INVALID_OTP;

import com.idb.auth.common.dto.response.ApiResponse;

import lombok.Getter;

@Getter
public class InvalidOtpException extends RuntimeException {

    private final ApiResponse<?> response;

    public InvalidOtpException(String message) {
        super(message);
        this.response = ApiResponse.builder().status(INVALID_OTP).message(message).build();
    }
}
