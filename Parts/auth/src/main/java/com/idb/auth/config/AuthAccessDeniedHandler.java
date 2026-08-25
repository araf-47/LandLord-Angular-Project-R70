package com.idb.auth.config;

import static com.idb.auth.common.constant.OperationStatus.ACCESS_DENIED;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.util.RequestContextUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Renders an authorization failure as 403 plus the standard {@code ApiResponse}
 * envelope.
 *
 * <p>Writing the body directly is load-bearing. The default handler calls
 * {@code response.sendError(403)}, which makes the container run an ERROR
 * dispatch to {@code /error}. That re-enters the security filter chain, but
 * {@code AuthFilter} is a {@code OncePerRequestFilter} and so skips error
 * dispatches by default - the re-entered chain therefore finds an empty
 * {@code SecurityContext}, {@code AuthorizationFilter} throws
 * {@code AuthenticationCredentialsNotFoundException}, and the honest 403 is
 * overwritten by a misleading 401. Committing the response here means no error
 * dispatch ever happens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        log.warn("Access denied for {} from IP: {}", request.getRequestURI(), RequestContextUtil.getClientIp(request));

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.builder()
                .status(ACCESS_DENIED)
                .message("You are not authorized to access this resource")
                .build()));
    }
}
