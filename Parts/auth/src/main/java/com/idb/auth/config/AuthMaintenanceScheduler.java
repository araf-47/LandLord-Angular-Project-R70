package com.idb.auth.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.idb.auth.service.IpBlockingService;
import com.idb.auth.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 16.4: both account lockout ({@code User.accountLocked}) and IP blocking
 * ({@code BlockedIp.active}) already self-expire lazily - the flag only flips
 * back on the next login attempt from that user/IP (see
 * {@code AuthProvider.handleSuccessfulLogin}, {@code
 * IpBlockingServiceImpl.isIpBlocked}). An account or IP nobody retries stays
 * marked locked/blocked forever even after its window elapses, which is stale
 * for anything reading that flag directly (the admin blocked-users/blocked-IPs
 * lists). This sweep just catches those up on a schedule; it changes no
 * behavior for login itself, which was already correct.
 *
 * <p>OTP state (codes, generation/validation attempt counters, resend cooldown)
 * needs no equivalent sweep - it lives entirely in a Caffeine cache with its own
 * TTL per entry (see {@code CacheConfig}), so it self-evicts with no DB rows to
 * clean up.
 *
 * <p>{@link IpBlockingService} is always resolvable - {@code NoOpIpBlockingServiceImpl}
 * stands in and returns 0 when {@code auth.ip.block.enabled} is false, this
 * project's default.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthMaintenanceScheduler {

    private final UserService userService;
    private final IpBlockingService ipBlockingService;

    /** Once an hour, on the hour - lockout windows here default to 30 minutes. */
    @Scheduled(cron = "0 0 * * * *")
    public void sweepExpiredLockouts() {
        int unlockedAccounts = userService.unlockExpiredAccounts();
        if (unlockedAccounts > 0) {
            log.info("Account lockout sweep: {} account(s) unlocked", unlockedAccounts);
        }

        int unblockedIps = ipBlockingService.unblockExpired();
        if (unblockedIps > 0) {
            log.info("IP block sweep: {} IP(s) unblocked", unblockedIps);
        }
    }
}
