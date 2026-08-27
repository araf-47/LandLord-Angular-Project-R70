package com.landlord.backend.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Writes the response body directly instead of letting {@code ResponseStatusException}
 * fall through to Spring's default handling, which calls {@code sendError()} and
 * triggers a container ERROR dispatch to {@code /error}. That dispatch re-enters
 * the security filter chain, finds no SecurityContext there, and overwrites the
 * real status (404, 409, ...) with a misleading 401 - the same defect class
 * Parts/auth's own AuthAccessDeniedHandler already works around for its 403 path.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", ex.getStatusCode().value());
        body.put("message", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}
