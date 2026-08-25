package com.idb.auth.filter;

import static com.idb.auth.constant.AuthConstants.CORS_ORIGINS;
import static com.idb.auth.constant.AuthConstants.IP_BLOCKING_BLOCK_DURATION_HOURS;
import static com.idb.auth.constant.AuthConstants.IP_BLOCKING_ENABLED;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.idb.auth.common.constant.OperationStatus;
import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.util.RequestContextUtil;
import com.idb.auth.constant.AuthConstants;
import com.idb.auth.service.IpBlockingService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Rejects blocked IPs before anything else in the chain runs, so a blocked
 * source cannot even reach the login endpoint.
 *
 * <p>Writes its own CORS headers: the response is produced before Spring
 * Security's CORS filter, so without them a browser sees an opaque CORS failure
 * instead of the 429 and its explanation.
 *
 * <p>The unblock endpoint is exempt, otherwise an administrator whose own IP got
 * blocked could never lift the block.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(name = IP_BLOCKING_ENABLED, havingValue = "true")
public class IpBlockingFilter extends OncePerRequestFilter {

    private final IpBlockingService ipBlockingService;
    private final ObjectMapper objectMapper;
    private final Environment env;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ipAddress = RequestContextUtil.getClientIp(request);
        String endpoint = request.getRequestURI();

        if (isInfrastructureEndpoint(endpoint) || AuthConstants.isIpBlockExempt(endpoint)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (ipBlockingService.isIpBlocked(ipAddress)) {
            log.warn("Blocked request from IP: {} to endpoint: {}", ipAddress, endpoint);
            writeBlockedIpResponse(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeBlockedIpResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int blockDurationHours = env.getProperty(IP_BLOCKING_BLOCK_DURATION_HOURS, Integer.class, 24);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .status(OperationStatus.IP_BLOCKED)
                .message(("Your IP address has been blocked due to too many failed authentication attempts. "
                        + "Please try again after %d hours.").formatted(blockDurationHours))
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        setCorsHeaders(request, response);

        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }

    private void setCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        String allowedOrigins = env.getProperty(CORS_ORIGINS, "*");

        if (origin != null && (allowedOrigins.equals("*") || allowedOrigins.contains(origin))) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            response.setHeader("Access-Control-Allow-Origin", allowedOrigins);
        }

        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods",
                env.getProperty("cors.allowed-methods", "GET,POST,PUT,DELETE,OPTIONS"));
        response.setHeader("Access-Control-Allow-Headers",
                env.getProperty("cors.allowed-headers", "Content-Type,Authorization,x-access-token,x-refresh-token"));
        response.setHeader("Access-Control-Expose-Headers",
                env.getProperty("cors.exposed-headers", "Content-Type,Authorization,x-access-token,x-refresh-token"));

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setStatus(HttpStatus.OK.value());
        }
    }

    /**
     * Endpoints that must answer regardless of the caller's block state.
     *
     * <p>{@code /actuator} is here because a liveness or readiness probe must never
     * be gated by a security counter: a blocked address would otherwise make the
     * instance look dead to its load balancer and get it pulled from rotation.
     *
     * <p>Note that {@code /api/v3/auth/login} is deliberately NOT exempt even
     * though it is a public URL - blocking brute-force login attempts is the whole
     * point of this filter.
     */
    private boolean isInfrastructureEndpoint(String endpoint) {
        return endpoint.startsWith("/v3/api-docs")
                || endpoint.startsWith("/swagger-ui")
                || endpoint.startsWith("/actuator");
    }
}
