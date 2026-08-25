package com.idb.auth.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads the request body for diagnostic logging. Relies on
 * {@code RequestBodyCachingFilter} having wrapped the request, otherwise the
 * body can only be read once.
 */
@Slf4j
public final class RequestLogUtil {

    private static final String UNAVAILABLE = "[Request body not available]";

    private RequestLogUtil() {
    }

    public static String getRequestBody(HttpServletRequest request) {
        if (request == null) {
            return "[No request available]";
        }

        String requestBody = UNAVAILABLE;

        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                try {
                    requestBody = new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
                } catch (Exception e) {
                    log.error("Error reading request body from wrapper", e);
                }
            }
        } else {
            // Catch Exception, not just IOException: reader.lines() wraps a read
            // failure in an UncheckedIOException, and an already-consumed or closed
            // stream throws IllegalStateException. This helper is called from inside
            // the exception handlers, so anything escaping here would mask the real
            // failure it was invoked to describe and turn it into a 500.
            try (BufferedReader reader = request.getReader()) {
                if (reader != null) {
                    requestBody = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            } catch (Exception e) {
                log.debug("Error reading request body: {}", e.getMessage());
            }
        }

        try {
            if (UNAVAILABLE.equals(requestBody) && !request.getParameterMap().isEmpty()) {
                Map<String, String> formData = new HashMap<>();
                request.getParameterMap().forEach((key, values) -> formData.put(key, String.join(", ", values)));
                requestBody = formData.toString();
            }
        } catch (Exception e) {
            log.debug("Error reading request parameters: {}", e.getMessage());
        }

        return requestBody;
    }

    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
