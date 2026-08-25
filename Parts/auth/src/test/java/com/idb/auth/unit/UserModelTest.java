package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.idb.auth.model.Role;
import com.idb.auth.model.User;

/** The account-lockout state machine, which lives entirely on the entity. */
class UserModelTest {

    private User user() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("hash");
        return user;
    }

    @Test
    @DisplayName("incrementing from null starts at 1 and stamps the failure time")
    void incrementFromNull() {
        User user = user();
        user.setFailedLoginAttempts(null);
        user.incrementFailedLoginAttempts();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLastFailedLoginAt()).isNotNull();
    }

    @Test
    void incrementAccumulates() {
        User user = user();
        for (int i = 0; i < 3; i++) {
            user.incrementFailedLoginAttempts();
        }
        assertThat(user.getFailedLoginAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("locking sets the flag and a future deadline")
    void lockAccountSetsDeadline() {
        User user = user();
        user.lockAccount(30);
        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.isTemporarilyLocked()).isTrue();
        assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("an elapsed deadline stops blocking login even though the flag is still set")
    void elapsedDeadlineIsNotTemporarilyLocked() {
        User user = user();
        user.lockAccount(30);
        user.setLockedUntil(LocalDateTime.now().minusSeconds(1));

        // isAccountLocked stays true - it records that a lock happened - but
        // isTemporarilyLocked is what gates authentication, so the window elapsing
        // is what lets the user back in. No scheduled job clears the flag.
        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.isTemporarilyLocked()).isFalse();
    }

    @Test
    @DisplayName("a set flag with no deadline is not a lock")
    void flagWithoutDeadlineIsNotALock() {
        User user = user();
        user.setAccountLocked(true);
        user.setLockedUntil(null);
        assertThat(user.isTemporarilyLocked()).isFalse();
    }

    @Test
    @DisplayName("reset clears the counter, the flag and the deadline together")
    void resetClearsEverything() {
        User user = user();
        user.incrementFailedLoginAttempts();
        user.lockAccount(30);

        user.resetFailedLoginAttempts();

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.isAccountLocked()).isFalse();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.isTemporarilyLocked()).isFalse();
    }

    @Test
    @DisplayName("every UserDetails predicate is driven by the single active flag")
    void userDetailsFlagsFollowActive() {
        User user = user();
        user.setActive(true);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();

        user.setActive(false);
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.isAccountNonExpired()).isFalse();
        assertThat(user.isAccountNonLocked()).isFalse();
        assertThat(user.isCredentialsNonExpired()).isFalse();
    }

    @Test
    @DisplayName("a lockout does not disable the account - the two flags are independent")
    void lockoutDoesNotDisableTheAccount() {
        User user = user();
        user.setActive(true);
        user.lockAccount(30);
        // isEnabled() is still true, so AuthProvider's disabled-check passes and the
        // dedicated isTemporarilyLocked() branch is what produces ACCOUNT_LOCKED
        // rather than USER_INACTIVE.
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isTemporarilyLocked()).isTrue();
    }

    @Test
    @DisplayName("authorities are the role list verbatim, with no ROLE_ prefix")
    void authoritiesAreRolesVerbatim() {
        User user = user();
        Role admin = new Role();
        admin.setName("ADMIN");
        user.setRoles(List.of(admin));

        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ADMIN");
        assertThat(admin.getAuthority()).isEqualTo("ADMIN");
        assertThat(admin.toString()).isEqualTo("ADMIN");
    }

    @Test
    void toStringIsTheUsername() {
        assertThat(user()).hasToString("alice");
    }

    @Test
    @DisplayName("hasId distinguishes an unsaved entity from a persisted one")
    void hasId() {
        User user = user();
        assertThat(user.hasId()).isFalse();
        user.setId(0L);
        assertThat(user.hasId()).isFalse();
        user.setId(7L);
        assertThat(user.hasId()).isTrue();
    }
}
