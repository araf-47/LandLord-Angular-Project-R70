package com.idb.auth.common.exception;

import static com.idb.auth.common.constant.OperationStatus.ERROR;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.util.StringUtil;

import lombok.Getter;

/**
 * Application error that carries both a detailed log message (with caller
 * location) and a sanitised {@link ApiResponse} for the client.
 */
@Getter
public class TraceableException extends Exception {

    private final ApiResponse<?> response;

    private TraceableException(String message, Throwable cause, ApiResponse<?> response) {
        super(message, cause);
        this.response = response;
    }

    private TraceableException(String format, Throwable cause, ApiResponse<?> response, Object... args) {
        super(StringUtil.format(format, args), cause);
        this.response = response;
    }

    public static TraceableException of(String logMessage, Throwable cause, String responseMessage) {
        String callerInfo = getCallerInfo(Thread.currentThread().getStackTrace(), cause);
        return new TraceableException(logMessage != null ? callerInfo + " " + logMessage : callerInfo, cause,
                ApiResponse.builder().status(ERROR).message(responseMessage).build());
    }

    public static TraceableException of(String logMessage, Throwable cause, String responseMessage, Object... args) {
        String callerInfo = getCallerInfo(Thread.currentThread().getStackTrace(), cause);
        return new TraceableException(logMessage != null ? callerInfo + " " + logMessage : callerInfo, cause,
                ApiResponse.builder().status(ERROR).message(responseMessage).build(), args);
    }

    public static String getCallerInfo(StackTraceElement[] stackTrace, Throwable cause) {
        StackTraceElement caller = stackTrace.length > 2 ? stackTrace[2] : null;
        String callerClassName = caller != null ? caller.getClassName() : "Unknown";
        String callerMethodName = caller != null ? caller.getMethodName() : "Unknown";
        int callerLineNumber = caller != null ? caller.getLineNumber() : -1;
        String causeMessage = cause != null ? cause.getMessage() : "Unknown";

        if (cause != null) {
            for (StackTraceElement element : cause.getStackTrace()) {
                if (element.getClassName().equals(callerClassName)
                        && element.getMethodName().equals(callerMethodName)) {
                    callerLineNumber = element.getLineNumber();
                    break;
                }
            }
        }

        return "Error in " + callerClassName + "." + callerMethodName + "(" + callerLineNumber
                + "). Caused by : " + causeMessage;
    }
}
