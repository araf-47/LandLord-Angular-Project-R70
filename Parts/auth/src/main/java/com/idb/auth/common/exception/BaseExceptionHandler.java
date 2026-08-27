package com.idb.auth.common.exception;

import static com.idb.auth.common.constant.CommonConstants.VALIDATION_ERROR_MSG;
import static com.idb.auth.common.constant.OperationStatus.ERROR;
import static com.idb.auth.common.constant.OperationStatus.VALIDATION_ERROR;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.util.ExceptionUtil;
import com.idb.auth.common.util.RequestLogUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Catch-all advice. Ordered LOWEST so {@code AuthExceptionHandler} wins for
 * authentication failures - otherwise the {@code Exception} handler here would
 * be an equally-specific candidate for some of them.
 *
 * <p>Scoped to {@code com.idb.auth} only: this module is embedded as a library
 * into host applications (e.g. landlord-backend, barivara-backend), and an
 * unscoped {@code @RestControllerAdvice} would otherwise intercept the host
 * app's own controllers too - silently rewriting their {@code
 * ResponseStatusException}-based 404s/409s/etc. into a 200 with a generic
 * error body, since the {@code Exception.class} handler below matches
 * everything.
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.idb.auth")
public class BaseExceptionHandler {

    @ExceptionHandler(TraceableException.class)
    public ResponseEntity<ApiResponse<?>> handleTraceableException(TraceableException ex) {
        ExceptionUtil.logErrorWithRequestBody("TraceableException", ex, getCurrentRequest());
        return ResponseEntity.ok(ex.getResponse());
    }

    @ExceptionHandler(LogOnlyException.class)
    public ResponseEntity<ApiResponse<?>> handleLogOnlyException(LogOnlyException ex) {
        if (ex.getMessage() != null) {
            ExceptionUtil.logErrorWithRequestBody("LogOnlyException", ex, getCurrentRequest());
        }
        return ResponseEntity.ok(ex.getResponse());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        HttpServletRequest request = getCurrentRequest();
        log.error("Validation exception occurred \r\n URI: {} \r\n Request Body: {}",
                request != null ? request.getRequestURI() : "unknown",
                RequestLogUtil.getRequestBody(request));

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), error.getDefaultMessage());
            } else {
                errors.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .status(VALIDATION_ERROR)
                .message(VALIDATION_ERROR_MSG)
                .data(errors)
                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        ExceptionUtil.logErrorWithRequestBody("ConstraintViolationException", ex, getCurrentRequest());
        return ResponseEntity.ok(ApiResponse.<Object>builder().status(ERROR).message(ex.getMessage()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllUncaughtException(Exception ex) {
        ExceptionUtil.logErrorWithRequestBody(ex, getCurrentRequest());
        return ResponseEntity.ok(ApiResponse.<Object>builder().status(ERROR).message("Something went wrong").build());
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
