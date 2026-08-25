package com.idb.auth.common.util;

import java.io.IOException;

import org.springframework.http.MediaType;

import com.idb.auth.common.constant.OperationStatus;
import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public final class ExceptionUtil {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ExceptionUtil() {
    }

    public static String extractApplicationStackTrace(Throwable ex) {
        if (ex == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = ex.getStackTrace();
        int count = 0;

        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith("com.idb.auth")) {
                appendFrame(sb, element);
                if (++count >= 10) {
                    break;
                }
            }
        }

        if (count == 0 && stackTrace.length > 0) {
            int max = Math.min(3, stackTrace.length);
            for (int i = 0; i < max; i++) {
                appendFrame(sb, stackTrace[i]);
            }
        }

        return sb.toString();
    }

    private static void appendFrame(StringBuilder sb, StackTraceElement element) {
        sb.append("\n    at ").append(element.getClassName())
                .append('.').append(element.getMethodName())
                .append('(').append(element.getFileName())
                .append(':').append(element.getLineNumber())
                .append(')');
    }

    public static boolean isContentTypeMismatch(HttpServletRequest request, Throwable ex) {
        if (ex == null) {
            return false;
        }
        String contentType = request.getContentType();
        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        return ("POST".equals(method) || "PUT".equals(method))
                && contentType != null
                && contentType.toLowerCase().contains("multipart/form-data")
                && (requestURI.contains("/update") || requestURI.contains("/create"));
    }

    public static void writeErrorResponse(HttpServletResponse response, OperationStatus status, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(MAPPER.writeValueAsString(
                ApiResponse.builder().status(status).message(message).build()));
    }

    public static boolean containsMessagePart(Throwable ex, String... parts) {
        if (ex == null || ex.getMessage() == null || parts == null) {
            return false;
        }
        for (String p : parts) {
            if (ex.getMessage().contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks the cause chain looking for a service-layer exception that already
     * carries a client-facing {@link ApiResponse}.
     */
    public static ApiResponse<?> extractServiceException(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof LogOnlyException logOnly) {
                return logOnly.getResponse();
            }
            if (current instanceof TraceableException traceable) {
                return traceable.getResponse();
            }
            current = current.getCause();
        }
        return null;
    }

    public static void logErrorWithRequestBody(String exceptionType, Exception ex, HttpServletRequest request) {
        String appStackTrace = ex.getCause() != null ? extractApplicationStackTrace(ex.getCause()) : "";
        String requestBody = RequestLogUtil.getRequestBody(request);
        String message = extractMessage(ex);

        if (StringUtil.isBlank(exceptionType)) {
            exceptionType = "Unexpected exception";
        }

        if (StringUtil.isNotBlank(requestBody)
                && (requestBody.contains("password") || requestBody.contains("user") || requestBody.contains("token")
                        || requestBody.contains("phone") || requestBody.contains("email"))) {
            requestBody = "Credential details are not logged";
        }

        log.error("{} occurred: {} \r\n URI: {} \r\n Request Body: {} \r\n Stack: {}",
                exceptionType, message,
                request != null ? request.getRequestURI() : "unknown",
                requestBody, appStackTrace);
    }

    public static void logErrorWithRequestBody(Exception ex, HttpServletRequest request) {
        logErrorWithRequestBody(ex.getClass().getSimpleName(), ex, request);
    }

    private static String extractMessage(Exception ex) {
        StringBuilder message = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && !msg.isBlank()) {
                message.append(msg).append("\n");
            }
            current = current.getCause();
        }
        return message.toString();
    }
}
