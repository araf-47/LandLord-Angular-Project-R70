package com.idb.auth.service.impl;

import static com.idb.auth.common.constant.OperationStatus.SUCCESS;
import static com.idb.auth.constant.AuthConstants.CACHE_OTP;
import static com.idb.auth.constant.AuthConstants.CACHE_OTP_ATTEMPTS;
import static com.idb.auth.constant.AuthConstants.CACHE_OTP_RESEND_COOLDOWN;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.idb.auth.common.dto.MailInfo;
import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.common.service.MailService;
import com.idb.auth.common.util.StringUtil;
import com.idb.auth.dao.UserRepository;
import com.idb.auth.model.User;
import com.idb.auth.service.OtpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OTPs are stored BCrypt-hashed in a Caffeine cache whose TTL is the expiry -
 * see {@code cache.config.params}. There is no plaintext copy anywhere, which is
 * why the only way to learn an OTP in a test is through the (mocked) mail seam.
 *
 * <p>Attempt counters are read and written directly through {@link CacheManager}
 * rather than via {@code @CachePut}/{@code @Cacheable}. The reference
 * implementation annotates helper methods and then calls them from
 * {@code validateOtp} on {@code this}: self-invocation bypasses the Spring
 * proxy, so the counters were never actually persisted and the rate limits never
 * triggered.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_VALIDATION_ATTEMPTS = 3;
    private static final int MAX_GENERATION_ATTEMPTS = 3;

    private static final String KEY_PREFIX_GEN = "otp_gen_";
    private static final String KEY_PREFIX_VAL = "otp_val_";

    private final CacheManager cacheManager;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final UserRepository userRepository;
    private final Environment env;

    @Override
    public ApiResponse<String> generateOtp(String username) throws TraceableException, LogOnlyException {
        if (cooldownCache().get(username) != null) {
            log.warn("OTP resend cooldown active for user: {}", username);
            throw TraceableException.of("OTP resend cooldown active for %s", new RuntimeException(),
                    "Please wait before requesting another OTP.", username);
        }
        if (incrementAttempts(KEY_PREFIX_GEN + username) > MAX_GENERATION_ATTEMPTS) {
            log.warn("OTP generation rate limit exceeded for user: {}", username);
            throw TraceableException.of("OTP generation rate limit exceeded for %s", new RuntimeException(),
                    "Too many OTP requests. Please try after some time.", username);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> LogOnlyException.of(null, "User not found"));
        if (StringUtil.isBlank(user.getEmail())) {
            throw LogOnlyException.of(null, "User email not found");
        }

        String otp = generateSecureRandomOtp();
        otpCache().put(username, passwordEncoder.encode(otp));

        sendOtpEmail(user, otp);
        cooldownCache().put(username, Boolean.TRUE);
        return ApiResponse.<String>builder()
                .status(SUCCESS)
                .message("OTP generated successfully. Valid for " + OTP_EXPIRY_MINUTES + " minutes")
                .build();
    }

    @Override
    public boolean validateOtp(String username, String otp) throws TraceableException {
        if (StringUtil.isBlank(otp)) {
            return false;
        }
        if (incrementAttempts(KEY_PREFIX_VAL + username) > MAX_VALIDATION_ATTEMPTS) {
            log.warn("Maximum OTP validation attempts exceeded for user: {}", username);
            return false;
        }

        String storedHashedOtp = otpCache().get(username, String.class);
        if (storedHashedOtp == null) {
            log.warn("No OTP found or expired for user: {}", username);
            return false;
        }

        boolean isValid = passwordEncoder.matches(otp, storedHashedOtp);
        if (isValid) {
            clearCache(username);
            log.info("OTP validated successfully for user: {}", username);
        } else {
            log.warn("Invalid OTP attempt for user: {}", username);
        }
        return isValid;
    }

    @Override
    public String generateSecureRandomOtp() {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Clears the OTP itself and both attempt counters. The counters use prefixed
     * keys, so they have to be evicted explicitly - evicting by bare username (as
     * the reference does) would leave them behind.
     */
    @Override
    public void clearCache(String username) {
        otpCache().evict(username);
        Cache attempts = attemptsCache();
        attempts.evict(KEY_PREFIX_GEN + username);
        attempts.evict(KEY_PREFIX_VAL + username);
        cooldownCache().evict(username);
        log.debug("Cleared OTP cache for user: {}", username);
    }

    @Override
    public void sendOtpEmail(User user, String otp) throws TraceableException {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("name", user.getUsername());
        templateModel.put("email", user.getEmail());
        templateModel.put("otp", otp);
        templateModel.put("expiryMinutes", OTP_EXPIRY_MINUTES);
        templateModel.put("logoUrl", env.getProperty("app.logo.url"));

        mailService.sendTemplatedEmail(MailInfo.builder()
                .to(List.of(user.getEmail()))
                .subject("OTP for " + user.getUsername())
                .templateName("otp")
                .templateModel(templateModel)
                .build());
    }

    private int incrementAttempts(String key) {
        Cache cache = attemptsCache();
        Integer current = cache.get(key, Integer.class);
        int next = (current == null ? 0 : current) + 1;
        cache.put(key, next);
        return next;
    }

    private Cache otpCache() {
        Cache cache = cacheManager.getCache(CACHE_OTP);
        if (cache == null) {
            throw new IllegalStateException("OTP cache '" + CACHE_OTP + "' is not configured");
        }
        return cache;
    }

    private Cache attemptsCache() {
        Cache cache = cacheManager.getCache(CACHE_OTP_ATTEMPTS);
        if (cache == null) {
            throw new IllegalStateException("OTP attempts cache '" + CACHE_OTP_ATTEMPTS + "' is not configured");
        }
        return cache;
    }

    private Cache cooldownCache() {
        Cache cache = cacheManager.getCache(CACHE_OTP_RESEND_COOLDOWN);
        if (cache == null) {
            throw new IllegalStateException("OTP resend cooldown cache '" + CACHE_OTP_RESEND_COOLDOWN
                    + "' is not configured");
        }
        return cache;
    }
}
