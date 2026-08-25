package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.env.MockEnvironment;

import com.idb.auth.constant.AttemptType;
import com.idb.auth.dao.BlockedIpRepository;
import com.idb.auth.model.BlockedIp;
import com.idb.auth.service.impl.IpBlockingServiceImpl;

/**
 * Per-IP thresholds. Each {@link AttemptType} has its own limit, and only two of
 * the four maintain separate counters, so the mapping is easy to get wrong.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IpBlockingServiceImplTest {

    private static final String IP = "203.0.113.7";

    @Mock private BlockedIpRepository repository;

    private MockEnvironment env;
    private IpBlockingServiceImpl service;

    @BeforeEach
    void setUp() {
        env = new MockEnvironment()
                .withProperty("auth.ip.block.max.failed.attempts", "4")
                .withProperty("auth.ip.block.max.unauthenticated.attempts", "3")
                .withProperty("auth.ip.block.max.invalid.jwt.attempts", "3")
                .withProperty("auth.ip.block.max.invalid.otp.attempts", "2")
                .withProperty("auth.ip.block.block.duration.hours", "24");
        service = new IpBlockingServiceImpl(repository, env);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Drives n attempts of one type against a single accumulating row. */
    private BlockedIp record(AttemptType type, int times) throws Exception {
        BlockedIp row = null;
        for (int i = 0; i < times; i++) {
            when(repository.findByIpAddress(IP)).thenReturn(Optional.ofNullable(row));
            row = service.recordFailedAttempt(IP, "/api/v3/test/any", "alice", "{}", type);
        }
        return row;
    }

    @Test
    @DisplayName("a first attempt creates an inactive row - recording is not blocking")
    void firstAttemptCreatesInactiveRow() throws Exception {
        BlockedIp row = record(AttemptType.UNAUTHENTICATED, 1);

        assertThat(row.getFailedAttempts()).isEqualTo(1);
        assertThat(row.getFailedUnauthenticatedAttempts()).isEqualTo(1);
        assertThat(row.isActive()).isFalse();
        assertThat(row.getUnblockAt()).isNull();
        assertThat(row.getIpAddress()).isEqualTo(IP);
        assertThat(row.getUsername()).isEqualTo("alice");
        assertThat(row.getLastFailureType()).isEqualTo("UNAUTHENTICATED");
        assertThat(row.getReason()).isEqualTo("Unauthenticated access attempt");
    }

    @Test
    @DisplayName("UNAUTHENTICATED blocks exactly at its own threshold")
    void unauthenticatedThreshold() throws Exception {
        assertThat(record(AttemptType.UNAUTHENTICATED, 2).isActive()).isFalse();

        BlockedIp blocked = record(AttemptType.UNAUTHENTICATED, 3);
        assertThat(blocked.isActive()).isTrue();
        assertThat(blocked.getUnblockAt()).isAfter(LocalDateTime.now().plusHours(23));
    }

    @Test
    @DisplayName("LOGIN uses the failed-attempts threshold and its own counter")
    void loginThreshold() throws Exception {
        assertThat(record(AttemptType.LOGIN, 3).isActive()).isFalse();

        BlockedIp blocked = record(AttemptType.LOGIN, 4);
        assertThat(blocked.isActive()).isTrue();
        assertThat(blocked.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(blocked.getReason()).isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("INVALID_JWT has a threshold distinct from LOGIN even though they share a counter")
    void invalidJwtThreshold() throws Exception {
        BlockedIp blocked = record(AttemptType.INVALID_JWT, 3);
        assertThat(blocked.isActive()).isTrue();
        assertThat(blocked.getReason()).isEqualTo("Invalid JWT token");
        assertThat(blocked.getLastFailureType()).isEqualTo("INVALID_JWT");
    }

    @Test
    @DisplayName("INVALID_OTP has the tightest threshold")
    void invalidOtpThreshold() throws Exception {
        assertThat(record(AttemptType.INVALID_OTP, 1).isActive()).isFalse();

        BlockedIp blocked = record(AttemptType.INVALID_OTP, 2);
        assertThat(blocked.isActive()).isTrue();
        assertThat(blocked.getReason()).isEqualTo("Invalid OTP");
    }

    @Test
    @DisplayName("LOGIN and INVALID_OTP share failedLoginAttempts, so mixed failures accumulate together")
    void loginAndOtpShareACounter() throws Exception {
        BlockedIp row = null;
        when(repository.findByIpAddress(IP)).thenReturn(Optional.empty());
        row = service.recordFailedAttempt(IP, "/e", "alice", "{}", AttemptType.LOGIN);

        when(repository.findByIpAddress(IP)).thenReturn(Optional.of(row));
        row = service.recordFailedAttempt(IP, "/e", "alice", "{}", AttemptType.INVALID_OTP);

        // Two different attempt types, one shared counter - and INVALID_OTP's
        // threshold of 2 is now met, so the second failure blocks.
        assertThat(row.getFailedLoginAttempts()).isEqualTo(2);
        assertThat(row.getFailedAttempts()).isEqualTo(2);
        assertThat(row.isActive()).isTrue();
    }

    @Test
    @DisplayName("thresholds fall back to defaults when the properties are absent")
    void thresholdDefaultsApply() throws Exception {
        service = new IpBlockingServiceImpl(repository, new MockEnvironment());
        when(repository.findByIpAddress(IP)).thenReturn(Optional.empty());

        BlockedIp row = service.recordFailedAttempt(IP, "/e", "alice", "{}", AttemptType.INVALID_OTP);
        // Default INVALID_OTP threshold is 5, so one attempt must not block.
        assertThat(row.isActive()).isFalse();
    }

    @Test
    @DisplayName("an unknown IP is not blocked")
    void unknownIpIsNotBlocked() {
        when(repository.findByIpAddress(IP)).thenReturn(Optional.empty());
        assertThat(service.isIpBlocked(IP)).isFalse();
    }

    @Test
    @DisplayName("an active block with a future deadline is blocked")
    void activeBlockIsBlocked() {
        BlockedIp row = BlockedIp.builder().ipAddress(IP).endpoint("/e")
                .blockedAt(LocalDateTime.now()).unblockAt(LocalDateTime.now().plusHours(1))
                .active(true).build();
        when(repository.findByIpAddress(IP)).thenReturn(Optional.of(row));

        assertThat(service.isIpBlocked(IP)).isTrue();
    }

    @Test
    @DisplayName("an elapsed deadline is lifted lazily on the next check, and persisted")
    void expiredBlockIsLiftedLazily() {
        BlockedIp row = BlockedIp.builder().ipAddress(IP).endpoint("/e")
                .blockedAt(LocalDateTime.now().minusDays(2)).unblockAt(LocalDateTime.now().minusHours(1))
                .active(true).build();
        when(repository.findByIpAddress(IP)).thenReturn(Optional.of(row));

        assertThat(service.isIpBlocked(IP)).isFalse();
        // There is no scheduled sweep, so the check itself has to clear the flag.
        assertThat(row.isActive()).isFalse();
        verify(repository).save(row);
    }

    @Test
    @DisplayName("an inactive row with no deadline is simply not blocked")
    void inactiveRowWithoutDeadline() {
        BlockedIp row = BlockedIp.builder().ipAddress(IP).endpoint("/e")
                .blockedAt(LocalDateTime.now()).active(false).build();
        when(repository.findByIpAddress(IP)).thenReturn(Optional.of(row));

        assertThat(service.isIpBlocked(IP)).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("unblocking a known IP deletes the row; an unknown IP reports false")
    void unblockIp() throws Exception {
        BlockedIp row = BlockedIp.builder().ipAddress(IP).endpoint("/e")
                .blockedAt(LocalDateTime.now()).active(true).build();
        when(repository.findByIpAddress(IP)).thenReturn(Optional.of(row));
        assertThat(service.unblockIp(IP)).isTrue();
        verify(repository).delete(row);

        when(repository.findByIpAddress("10.0.0.1")).thenReturn(Optional.empty());
        assertThat(service.unblockIp("10.0.0.1")).isFalse();
    }

    @Test
    @DisplayName("unblocking a username clears every one of its rows and reports the count")
    void unblockAllForUser() throws Exception {
        List<BlockedIp> rows = List.of(
                BlockedIp.builder().ipAddress("10.0.0.1").endpoint("/e").blockedAt(LocalDateTime.now()).build(),
                BlockedIp.builder().ipAddress("10.0.0.2").endpoint("/e").blockedAt(LocalDateTime.now()).build());
        when(repository.findByUsername("alice")).thenReturn(rows);

        assertThat(service.unblockAllForUser("alice")).isEqualTo(2);
        verify(repository).deleteAll(rows);
    }

    @Test
    @DisplayName("unblocking a username with no rows is a no-op reporting zero")
    void unblockAllForUserWithNoRows() throws Exception {
        when(repository.findByUsername("ghost")).thenReturn(List.of());

        assertThat(service.unblockAllForUser("ghost")).isZero();
        verify(repository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("getBlockedIp surfaces the row so login can report the remaining budget")
    void getBlockedIp() {
        BlockedIp row = BlockedIp.builder().ipAddress(IP).endpoint("/e")
                .blockedAt(LocalDateTime.now()).failedLoginAttempts(2).active(false).build();
        when(repository.findByIpAddress(IP)).thenReturn(Optional.of(row));

        assertThat(service.getBlockedIp(IP)).isSameAs(row);

        when(repository.findByIpAddress("10.0.0.9")).thenReturn(Optional.empty());
        assertThat(service.getBlockedIp("10.0.0.9")).isNull();
    }

    @Test
    @DisplayName("a repository failure is wrapped, not leaked as a raw runtime exception")
    void repositoryFailureIsWrapped() {
        when(repository.findByIpAddress(IP)).thenThrow(new RuntimeException("db down"));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.recordFailedAttempt(IP, "/e", "alice", "{}", AttemptType.LOGIN))
                .isInstanceOf(com.idb.auth.common.exception.TraceableException.class);
    }
}
