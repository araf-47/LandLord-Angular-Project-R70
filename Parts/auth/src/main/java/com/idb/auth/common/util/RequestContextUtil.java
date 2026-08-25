package com.idb.auth.common.util;

import static com.idb.auth.common.constant.CommonConstants.AUTH_HEADER;
import static com.idb.auth.common.constant.CommonConstants.REFRESH_TOKEN_HEADER;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestContextUtil {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private RequestContextUtil() {
    }

    public static String getCurrentRequestUrl() {
        return getRequest().getRequestURL().toString();
    }

    public static String getClientIp() {
        return getClientIp(getRequest());
    }

    public static String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (StringUtil.isNotEmpty(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static Map<String, String> getHeaders() {
        return getHeaders(getRequest());
    }

    public static Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator()
                .forEachRemaining(headerName -> headers.put(headerName, request.getHeader(headerName)));
        return headers;
    }

    public static String getAuthorization() {
        return getRequest().getHeader(AUTH_HEADER);
    }

    public static String getRefreshToken() {
        return getRequest().getHeader(REFRESH_TOKEN_HEADER);
    }

    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }
}
