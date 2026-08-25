package com.idb.auth.common.filter;

import static com.idb.auth.common.constant.OperationStatus.ERROR;
import static com.idb.auth.common.constant.OperationStatus.SESSION_EXPIRED;
import static com.idb.auth.common.constant.OperationStatus.VALIDATION_ERROR;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.util.ExceptionUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Converts anything thrown out of the filter chain (including the security
 * filters, which sit downstream of this one) into a JSON {@code ApiResponse}.
 */
@Slf4j
@Component
@Order(HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class GlobalExceptionFilter extends OncePerRequestFilter {

    /**
     * Cache limit for the re-readable request body. Spring Framework 7 removed the
     * unbounded single-argument {@code ContentCachingRequestWrapper} constructor, so
     * a ceiling has to be chosen explicitly. Auth payloads are small; anything past
     * this is simply not cached, which only degrades diagnostic logging.
     */
    private static final int MAX_CACHED_BODY_BYTES = 64 * 1024;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpServletRequest wrappedRequest = request instanceof ContentCachingRequestWrapper
                ? request
                : new ContentCachingRequestWrapper(request, MAX_CACHED_BODY_BYTES);
        try {
            filterChain.doFilter(wrappedRequest, response);
        } catch (Exception ex) {
            handleException(wrappedRequest, response, ex);
        }
    }

    private void handleException(HttpServletRequest request, HttpServletResponse response, Exception ex)
            throws IOException {

        if (ExceptionUtil.isContentTypeMismatch(request, ex)) {
            String errorMessage = "Invalid content type. Expected application/json, but received "
                    + request.getContentType();
            log.warn("Content type mismatch: {} | URI: {} | Client IP: {}",
                    errorMessage, request.getRequestURI(), request.getRemoteAddr());
            ExceptionUtil.writeErrorResponse(response, VALIDATION_ERROR, errorMessage);
            return;
        }

        if (ExceptionUtil.containsMessagePart(ex, "Authentication object was not found in the SecurityContext")) {
            log.warn("Security context authentication issue: {} | URI: {}", ex.getMessage(), request.getRequestURI());
            ExceptionUtil.writeErrorResponse(response, ERROR, "Authentication required. Please login and try again.");
            return;
        }

        if (ExceptionUtil.containsMessagePart(ex, "Session expired", "Invalid token")) {
            ExceptionUtil.writeErrorResponse(response, SESSION_EXPIRED, ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // The security entry point already wrote a body and a 401/403 - do not
        // overwrite it with a generic error.
        if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED
                || response.getStatus() == HttpServletResponse.SC_FORBIDDEN) {
            return;
        }

        ApiResponse<?> serviceExceptionResponse = ExceptionUtil.extractServiceException(ex);
        if (serviceExceptionResponse != null) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(objectMapper.writeValueAsString(serviceExceptionResponse));
            return;
        }

        ExceptionUtil.logErrorWithRequestBody(ex, request);
        ExceptionUtil.writeErrorResponse(response, ERROR, "Failed to process request. Please try again later.");
    }
}
