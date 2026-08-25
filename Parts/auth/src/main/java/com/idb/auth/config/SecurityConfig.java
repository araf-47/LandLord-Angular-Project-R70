package com.idb.auth.config;

import static com.idb.auth.common.constant.CommonConstants.PUBLIC_URLS;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;

import com.idb.auth.common.util.StringUtil;
import com.idb.auth.filter.AuthFilter;
import com.idb.auth.model.Permission;
import com.idb.auth.service.PermissionService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Stateless JWT security. Notable choices, all inherited from the reference
 * implementation:
 *
 * <ul>
 * <li>{@code anonymous} is disabled, so there is no half-authenticated state -
 * a request is either authenticated by {@code AuthFilter} or rejected.
 * <li>Authorization rules are <b>data</b>, loaded from permissions.json at
 * startup by {@code PermissionService}. Matching is first-match-wins, so the
 * declaration order in that file is significant.
 * <li>{@code hasAnyAuthority}, not {@code hasAnyRole}: {@code Role} is itself
 * the {@code GrantedAuthority} and carries no {@code ROLE_} prefix.
 * <li>CSRF is off because there are no cookies or sessions to forge against.
 * <li>Both the entry point and the access-denied handler write their response
 * body directly instead of calling {@code sendError}, so the container never
 * runs an ERROR dispatch back through this chain. See
 * {@link AuthAccessDeniedHandler} for why that matters.
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthEntryPoint authEntryPoint;
    private final AuthAccessDeniedHandler accessDeniedHandler;
    private final AuthFilter authFilter;
    private final AuthManager authManager;
    private final AuthProvider authProvider;
    private final UserDetailsService userDetailsService;
    private final PermissionService permissionService;
    private final Environment env;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(this::corsConfiguration))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .anonymous(anonymous -> anonymous.disable())
                .authenticationManager(authManager)
                .authenticationProvider(authProvider)
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(this::configureRequestAuthorization)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfiguration corsConfiguration(HttpServletRequest request) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(splitProperty("cors.allowed-origins", "*"));
        corsConfiguration.setAllowedMethods(splitProperty("cors.allowed-methods", "GET,POST,PUT,DELETE,OPTIONS"));
        corsConfiguration.setAllowedHeaders(splitProperty("cors.allowed-headers", "*"));
        corsConfiguration.setExposedHeaders(splitProperty("cors.exposed-headers", "x-access-token,x-refresh-token"));
        corsConfiguration.setAllowCredentials(true);
        return corsConfiguration;
    }

    private List<String> splitProperty(String key, String defaultValue) {
        return Arrays.asList(env.getProperty(key, defaultValue).split(" *, *"));
    }

    private void configureRequestAuthorization(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(PUBLIC_URLS.toArray(new String[0])).permitAll();

        List<Permission> permissions = permissionService.loadPermissionsFromResource();
        if (!CollectionUtils.isEmpty(permissions)) {
            for (Permission permission : permissions) {
                if (StringUtil.isNotBlank(permission.getUrl()) && !CollectionUtils.isEmpty(permission.getRoles())) {
                    auth.requestMatchers(permission.getUrl())
                            .hasAnyAuthority(permission.getRoles().toArray(new String[0]));
                }
            }
        }

        auth.anyRequest().authenticated();
    }
}
