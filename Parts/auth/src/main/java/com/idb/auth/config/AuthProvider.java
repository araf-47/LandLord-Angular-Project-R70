package com.idb.auth.config;

import static com.idb.auth.common.constant.CommonConstants.EMPTY_STRING;
import static com.idb.auth.constant.AttemptType.LOGIN;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.idb.auth.common.util.RequestContextUtil;
import com.idb.auth.common.util.RequestLogUtil;
import com.idb.auth.dto.request.BearerAuthenticationToken;
import com.idb.auth.model.User;
import com.idb.auth.service.IpBlockingService;
import com.idb.auth.service.UserService;
import com.idb.auth.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Verifies credentials and owns per-account lockout.
 *
 * <p>Handles two token shapes: a {@link UsernamePasswordAuthenticationToken}
 * (login - password is checked and an access token is minted into the credentials
 * slot) and a {@link BearerAuthenticationToken} (already-validated JWT - the
 * credentials pass through untouched).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthProvider implements AuthenticationProvider {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final IpBlockingService ipBlockingService;

    @Value("${security.account-lockout.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.account-lockout.duration-minutes:30}")
    private long lockoutDurationMinutes;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = Objects.toString(authentication.getPrincipal(), EMPTY_STRING);
        String password = Objects.toString(authentication.getCredentials(), EMPTY_STRING);

        UserDetails userDetails;
        try {
            userDetails = userService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            // Deliberately indistinguishable from a wrong password, to avoid
            // username enumeration.
            throw new BadCredentialsException("Invalid username or password");
        }

        if (userDetails == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!userDetails.isEnabled()) {
            throw new InsufficientAuthenticationException("Account is disabled");
        }

        if (userDetails instanceof User user) {
            if (user.isTemporarilyLocked()) {
                log.warn("Attempt to login to locked account: {}", username);
                throw new LockedException("Account is temporarily locked due to too many failed login attempts. "
                        + "Please try again later.");
            }

            if (authentication instanceof UsernamePasswordAuthenticationToken) {
                if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                    handleFailedLogin(user);
                    throw new BadCredentialsException("Invalid username or password");
                }

                handleSuccessfulLogin(user);
                password = jwtUtil.generateAccessToken(username, userDetails.getPassword());
            }
        }

        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }

    private void handleFailedLogin(User user) {
        try {
            HttpServletRequest request = RequestContextUtil.getRequest();
            user.incrementFailedLoginAttempts();
            ipBlockingService.recordFailedAttempt(RequestContextUtil.getClientIp(request), request.getRequestURI(),
                    user.getUsername(), RequestLogUtil.getRequestBody(request), LOGIN);

            if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                user.lockAccount(lockoutDurationMinutes);
                log.warn("Account locked for user: {} after {} failed attempts",
                        user.getUsername(), user.getFailedLoginAttempts());
            }

            userService.saveUser(user);
        } catch (Exception e) {
            log.error("Failed to update failed login attempts", e);
        }
    }

    private void handleSuccessfulLogin(User user) {
        try {
            ipBlockingService.unblockAllForUser(user.getUsername());
            Integer attempts = user.getFailedLoginAttempts();
            if (user.isAccountLocked() || (attempts != null && attempts > 0)) {
                user.resetFailedLoginAttempts();
                userService.saveUser(user);
                log.info("Reset failed-login state on successful login for user: {}", user.getUsername());
            }
        } catch (Exception e) {
            log.error("Failed to reset login attempts", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.equals(authentication)
                || BearerAuthenticationToken.class.equals(authentication);
    }
}
