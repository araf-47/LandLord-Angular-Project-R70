package com.idb.auth.service.impl;

import static com.idb.auth.common.constant.OperationStatus.ERROR;
import static com.idb.auth.common.constant.OperationStatus.OTP_REQUIRED;
import static com.idb.auth.common.constant.OperationStatus.SUCCESS;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.common.util.JsonUtil;
import com.idb.auth.common.util.RequestContextUtil;
import com.idb.auth.constant.AttemptType;
import com.idb.auth.dto.request.ForgotPasswordRequest;
import com.idb.auth.dto.request.LoginRequest;
import com.idb.auth.dto.response.AuthResponse;
import com.idb.auth.exception.InvalidOtpException;
import com.idb.auth.exception.IpBlockedException;
import com.idb.auth.exception.OtpRequiredException;
import com.idb.auth.model.BlockedIp;
import com.idb.auth.model.User;
import com.idb.auth.service.AuthService;
import com.idb.auth.service.IpBlockingService;
import com.idb.auth.service.OtpService;
import com.idb.auth.service.UserService;
import com.idb.auth.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final IpBlockingService ipBlockingService;

    @Value("${jwt.token.expiration.access}")
    private long accessTokenExpiration;

    @Value("${auth.ip.block.block.duration.hours:1440}")
    private int blockDurationHours;

    @Value("${auth.ip.block.max.failed.attempts:10}")
    private int maxFailedAttempts;

    /**
     * Password check first, then the second factor. Both layers of throttling are
     * in play: per-account lockout (enforced by {@code AuthProvider}) and per-IP
     * blocking (enforced here and by {@code IpBlockingFilter}).
     */
    @Override
    public ApiResponse<AuthResponse> login(LoginRequest request) throws TraceableException {
        String clientIp = RequestContextUtil.getClientIp();
        String endpoint = RequestContextUtil.getCurrentRequestUrl();
        String requestBody = JsonUtil.toJson(request);

        if (ipBlockingService.isIpBlocked(clientIp)) {
            throw new IpBlockedException(clientIp, blockDurationHours);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            User user = (User) authentication.getPrincipal();

            if (user.isTwoFactorEnabled()) {
                if (request.getOtp() != null && !request.getOtp().isEmpty()) {
                    if (!otpService.validateOtp(request.getUsername(), request.getOtp())) {
                        ipBlockingService.recordFailedAttempt(clientIp, endpoint, request.getUsername(),
                                requestBody, AttemptType.INVALID_OTP);
                        throw new InvalidOtpException("Invalid OTP");
                    }
                } else {
                    // Credentials were valid: issue a challenge instead of a token.
                    otpService.generateOtp(request.getUsername());
                    return ApiResponse.<AuthResponse>builder()
                            .status(OTP_REQUIRED)
                            .message("OTP required")
                            .build();
                }
            }

            // AuthProvider put the freshly minted access token in the credentials
            // slot of the successful authentication.
            AuthResponse response = new AuthResponse();
            response.setAccessToken(authentication.getCredentials().toString());
            response.setRefreshToken(jwtUtil.generateRefreshToken(user.getUsername(), user.getPassword()));
            response.setExpiresInSeconds(accessTokenExpiration);

            return ApiResponse.<AuthResponse>builder()
                    .status(SUCCESS)
                    .message("Login successful")
                    .data(response)
                    .build();
        } catch (InvalidOtpException | OtpRequiredException | LockedException | IpBlockedException e) {
            throw e;
        } catch (BadCredentialsException e) {
            throw withRemainingAttempts(clientIp, e);
        } catch (Exception e) {
            throw TraceableException.of("Login failed for user %s", e, "Login failed", request.getUsername());
        }
    }

    /**
     * Tells the caller how many attempts remain before the IP itself gets blocked,
     * but only while the IP is not blocked yet.
     */
    private BadCredentialsException withRemainingAttempts(String clientIp, BadCredentialsException e) {
        BlockedIp failedAttempt = ipBlockingService.getBlockedIp(clientIp);
        if (failedAttempt != null && !failedAttempt.isActive() && failedAttempt.getFailedLoginAttempts() != null) {
            int remainingAttempts = maxFailedAttempts - failedAttempt.getFailedLoginAttempts();
            if (remainingAttempts > 0) {
                return new BadCredentialsException(
                        "Invalid username or password. You have %d attempts remaining before your IP is blocked."
                                .formatted(remainingAttempts));
            }
        }
        return e;
    }

    @Override
    public ApiResponse<String> forgotPassword(ForgotPasswordRequest request) throws TraceableException {
        String clientIp = RequestContextUtil.getClientIp();
        String endpoint = RequestContextUtil.getCurrentRequestUrl();
        String requestBody = JsonUtil.toJson(request);

        if (ipBlockingService.isIpBlocked(clientIp)) {
            throw new IpBlockedException(clientIp, blockDurationHours);
        }

        try {
            if (otpService.validateOtp(request.getUsername(), request.getOtp())) {
                User user = userService.findByUsername(request.getUsername());
                userService.changePasswordUnchecked(user.getUsername(), request.getPassword());

                // A successful reset also clears an account lockout - that is the
                // documented self-service unlock path.
                if (user.isAccountLocked()) {
                    user.resetFailedLoginAttempts();
                    userService.saveUser(user);
                    log.info("Account unlocked for user {} after successful password reset", request.getUsername());
                }

                return ApiResponse.<String>builder()
                        .status(SUCCESS)
                        .message("Password reset successful")
                        .build();
            }

            ipBlockingService.recordFailedAttempt(clientIp, endpoint, request.getUsername(),
                    requestBody, AttemptType.INVALID_OTP);

            return ApiResponse.<String>builder()
                    .status(ERROR)
                    .message("Invalid otp")
                    .build();
        } catch (IpBlockedException e) {
            throw e;
        } catch (Exception e) {
            ipBlockingService.recordFailedAttempt(clientIp, endpoint, request.getUsername(),
                    requestBody, AttemptType.INVALID_OTP);
            throw TraceableException.of("Password reset failed for user %s", e, "Invalid username",
                    request.getUsername());
        }
    }
}
