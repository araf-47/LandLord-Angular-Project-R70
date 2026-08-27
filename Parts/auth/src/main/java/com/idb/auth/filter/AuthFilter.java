package com.idb.auth.filter;

import static com.idb.auth.common.constant.CommonConstants.ACCESS_TOKEN_HEADER;
import static com.idb.auth.common.constant.CommonConstants.EMPTY_STRING;
import static com.idb.auth.common.constant.CommonConstants.PUBLIC_GET_URLS;
import static com.idb.auth.common.constant.CommonConstants.PUBLIC_URLS;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.idb.auth.common.util.StringUtil;
import com.idb.auth.config.AuthEntryPoint;
import com.idb.auth.config.AuthManager;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Establishes the {@link SecurityContextHolder} authentication for every
 * non-public request by delegating to {@link AuthManager}.
 *
 * <p>When the access token was silently rotated (expired token + valid refresh
 * token), the new one is echoed back in the {@code x-access-token} response
 * header - that is how a client learns about the rotation without a separate
 * refresh call.
 *
 * <p>Authentication failures are handed to {@link AuthEntryPoint} here rather
 * than being allowed to propagate. This filter sits upstream of Spring
 * Security's {@code ExceptionTranslationFilter}, so a thrown
 * {@code AuthenticationException} would escape the security chain entirely and
 * get flattened by {@code GlobalExceptionFilter} into an HTTP 200 with a generic
 * ERROR body - leaving the entry point unreachable, the 401/403 status codes
 * wrong, and the stale-token headers never blanked. Calling it directly is what
 * makes the configured entry point actually run.
 *
 * <p>{@code PathPatternRequestMatcher} replaces the reference project's
 * {@code AntPathRequestMatcher}, which Spring Security 7 removed. The matcher
 * list is built lazily because {@code PUBLIC_URLS} is populated by
 * {@code AuthApplication.initPublicUrls()}, which may run after this bean is
 * constructed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private final AuthManager authManager;
    private final AuthEntryPoint authEntryPoint;
    private volatile RequestMatcher publicUrls;
    private volatile RequestMatcher publicGetUrls;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        RequestMatcher matcher = publicUrls;
        if (matcher == null) {
            List<RequestMatcher> matchers = PUBLIC_URLS.stream()
                    .map(PathPatternRequestMatcher::pathPattern)
                    .map(RequestMatcher.class::cast)
                    .toList();
            matcher = new OrRequestMatcher(matchers.toArray(new RequestMatcher[0]));
            publicUrls = matcher;
        }
        if (matcher.matches(request)) {
            return true;
        }

        if (!"GET".equalsIgnoreCase(request.getMethod()) || PUBLIC_GET_URLS.isEmpty()) {
            return false;
        }
        RequestMatcher getMatcher = publicGetUrls;
        if (getMatcher == null) {
            List<RequestMatcher> matchers = PUBLIC_GET_URLS.stream()
                    .map(PathPatternRequestMatcher::pathPattern)
                    .map(RequestMatcher.class::cast)
                    .toList();
            getMatcher = new OrRequestMatcher(matchers.toArray(new RequestMatcher[0]));
            publicGetUrls = getMatcher;
        }
        return getMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth;
        try {
            auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(
                    Objects.toString(request.getParameter("username"), EMPTY_STRING),
                    Objects.toString(request.getParameter("password"), EMPTY_STRING)));
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            authEntryPoint.commence(request, response, e);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(auth);

        if (auth != null) {
            String rotatedAccessToken = Objects.toString(auth.getCredentials(), EMPTY_STRING);
            if (StringUtil.isNotEmpty(rotatedAccessToken)) {
                response.setHeader(ACCESS_TOKEN_HEADER, rotatedAccessToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
