package com.idb.auth.controller;

import static com.idb.auth.common.constant.OperationStatus.SUCCESS;
import static com.idb.auth.constant.AuthConstants.URL_TEST_CONTROLLER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.idb.auth.common.dto.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * Probe endpoints that exist so the integration tests can observe authorization
 * decisions without depending on any business feature.
 *
 * <p>The role requirements for {@code /any}, {@code /admin-only} and
 * {@code /user-only} come from permissions.json (URL-based rules registered in
 * {@code SecurityConfig}); {@code /method-secured} instead exercises
 * {@code @EnableMethodSecurity}, which is a separate enforcement path.
 */
@RestController
@RequestMapping(URL_TEST_CONTROLLER)
@RequiredArgsConstructor
public class TestController {

    @GetMapping(value = "/any", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> any(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .status(SUCCESS)
                .message("authenticated")
                .data(Map.of(
                        "username", authentication.getName(),
                        "authorities", authorities(authentication)))
                .build());
    }

    @GetMapping(value = "/admin-only", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> adminOnly(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(SUCCESS)
                .message("admin area")
                .data(authentication.getName())
                .build());
    }

    @GetMapping(value = "/user-only", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> userOnly(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(SUCCESS)
                .message("user area")
                .data(authentication.getName())
                .build());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping(value = "/method-secured", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> methodSecured(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(SUCCESS)
                .message("method secured")
                .data(authentication.getName())
                .build());
    }

    private List<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
    }
}
