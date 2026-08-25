package com.idb.auth.common.exception;

import static com.idb.auth.common.constant.OperationStatus.ERROR;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.util.StringUtil;

import lombok.Getter;

/**
 * Expected, non-exceptional failure (bad input, missing record). Logged without
 * a stack trace and returned to the client as an {@code ERROR} ApiResponse.
 */
@Getter
public class LogOnlyException extends Exception {

    private final ApiResponse<?> response;

    private LogOnlyException(String message, ApiResponse<?> response) {
        super(message);
        this.response = response;
    }

    private LogOnlyException(String message, ApiResponse<?> response, Object... args) {
        super(StringUtil.format(message, args));
        this.response = response;
    }

    public static LogOnlyException of(String logMessage, String responseMessage) {
        return new LogOnlyException(logMessage,
                ApiResponse.builder().status(ERROR).message(responseMessage).build());
    }

    public static LogOnlyException of(String logMessage, String responseMessage, Object... args) {
        return new LogOnlyException(logMessage,
                ApiResponse.builder().status(ERROR).message(responseMessage).build(), args);
    }
}
