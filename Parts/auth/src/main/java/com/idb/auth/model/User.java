package com.idb.auth.model;

import static jakarta.persistence.FetchType.EAGER;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends AuditableModel implements UserDetails {

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @ManyToMany(fetch = EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private List<Role> roles = new ArrayList<>();

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(name = "last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;

    /**
     * Watermark for token revocation: any token issued before this instant is
     * rejected. Set by "logout everywhere" rather than maintaining a blacklist.
     */
    @JsonIgnore
    @Column(name = "tokens_valid_after")
    private LocalDateTime tokensValidAfter;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public boolean isAccountNonExpired() {
        return isActive();
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isActive();
    }

    @Override
    public boolean isEnabled() {
        return isActive();
    }

    @Override
    public String toString() {
        return username;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null) ? 1 : this.failedLoginAttempts + 1;
        this.lastFailedLoginAt = LocalDateTime.now();
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLocked = false;
        this.lockedUntil = null;
    }

    public void lockAccount(long lockoutDurationMinutes) {
        this.accountLocked = true;
        this.lockedUntil = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
    }

    /**
     * True only while the lockout window is still open. A stale {@code true} in
     * {@code accountLocked} with an elapsed {@code lockedUntil} does not block
     * login - the window expiring is what unlocks the account.
     */
    public boolean isTemporarilyLocked() {
        return this.accountLocked && this.lockedUntil != null && LocalDateTime.now().isBefore(this.lockedUntil);
    }

    public boolean isAccountLocked() {
        return this.accountLocked;
    }
}
