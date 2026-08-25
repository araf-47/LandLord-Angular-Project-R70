package com.idb.auth.common.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Wraps the request so the body can be read more than once. Required because
 * {@code AuthManager} reads the body to record failed-attempt request data, and
 * the controller then needs to deserialise the same body.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestBodyCachingFilter extends OncePerRequestFilter {

    /**
     * Cache limit for the re-readable request body. Spring Framework 7 removed the
     * unbounded single-argument {@code ContentCachingRequestWrapper} constructor, so
     * a ceiling has to be chosen explicitly. Auth payloads are small; anything past
     * this is simply not cached, which only degrades diagnostic logging.
     */
    private static final int MAX_CACHED_BODY_BYTES = 64 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getContentType() != null && request.getContentType().contains("multipart/form-data")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_CACHED_BODY_BYTES);

        try {
            filterChain.doFilter(wrappedRequest, response);
        } catch (Exception e) {
            if (isServiceLayerException(e)) {
                log.debug("Service layer exception in filter chain: {}, Type: {}, URI: {}",
                        e.getMessage(), e.getClass().getName(), wrappedRequest.getRequestURI());
            } else {
                log.error("Unhandled exception in filter chain: {}, Type: {}, URI: {}",
                        e.getMessage(), e.getClass().getName(), wrappedRequest.getRequestURI());
            }
            throw e;
        }
    }

    private boolean isServiceLayerException(Throwable e) {
        if (e == null) {
            return false;
        }
        if (e instanceof LogOnlyException || e instanceof TraceableException) {
            return true;
        }
        return isServiceLayerException(e.getCause());
    }
}
