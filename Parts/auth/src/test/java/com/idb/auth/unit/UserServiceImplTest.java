package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.dao.RoleRepository;
import com.idb.auth.dao.UserRepository;
import com.idb.auth.dto.request.ChangePasswordRequest;
import com.idb.auth.dto.request.UserRegistrationRequest;
import com.idb.auth.model.Role;
import com.idb.auth.model.User;
import com.idb.auth.service.OtpService;
import com.idb.auth.service.UserService;
import com.idb.auth.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private OtpService otpService;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, roleRepository, passwordEncoder, otpService);
        ReflectionTestUtils.setField(userService, "defaultUsername", "admin");
        ReflectionTestUtils.setField(userService, "defaultPassword", "Admin@12345");
        ReflectionTestUtils.setField(userService, "defaultRoleName", "ADMIN");
        ReflectionTestUtils.setField(userService, "adminPhone", "+8801700000000");
        ReflectionTestUtils.setField(userService, "adminEmail", "admin@test.local");
        ReflectionTestUtils.setField(userService, "defaultTwoFactorEnabled", false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User user(String username, String rawPassword) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(username + "@test.local");
        user.setPhone("+8801711111111");
        user.setActive(true);
        Role role = new Role();
        role.setName("ADMIN");
        user.setRoles(new ArrayList<>(List.of(role)));
        return user;
    }

    private UserRegistrationRequest registration() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("newUser");
        request.setPassword("Valid@Pass1");
        request.setEmail("new@test.local");
        request.setPhone("+8801722222222");
        request.setRoles(List.of("USER"));
        return request;
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    @DisplayName("registration hashes the password rather than storing it")
    void registrationHashesPassword() throws Exception {
        when(roleRepository.findByNameIn(List.of("USER"))).thenReturn(List.of(new Role()));

        User created = userService.registerUser(registration());

        assertThat(created.getPassword()).isNotEqualTo("Valid@Pass1").startsWith("$2");
        assertThat(passwordEncoder.matches("Valid@Pass1", created.getPassword())).isTrue();
    }

    @Test
    @DisplayName("registration enforces the username pattern, which the reference never checked")
    void registrationEnforcesUsernamePattern() {
        UserRegistrationRequest request = registration();
        request.setUsername("has_underscore");

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(LogOnlyException.class)
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("Invalid username"));
    }

    @Test
    @DisplayName("registration rejects a weak password, a bad email, a bad phone and no roles")
    void registrationValidatesEverything() {
        UserRegistrationRequest weak = registration();
        weak.setPassword("weak");
        assertThatThrownBy(() -> userService.registerUser(weak))
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("Invalid password"));

        UserRegistrationRequest badEmail = registration();
        badEmail.setEmail("not-an-email");
        assertThatThrownBy(() -> userService.registerUser(badEmail))
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("Invalid email"));

        UserRegistrationRequest badPhone = registration();
        badPhone.setPhone("nope");
        assertThatThrownBy(() -> userService.registerUser(badPhone))
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("Invalid phone number"));

        UserRegistrationRequest noRoles = registration();
        noRoles.setRoles(List.of());
        assertThatThrownBy(() -> userService.registerUser(noRoles))
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("Roles are required"));
    }

    @Test
    @DisplayName("a blank email is allowed - only a malformed one is rejected")
    void blankEmailIsAllowed() throws Exception {
        UserRegistrationRequest request = registration();
        request.setEmail(null);
        when(roleRepository.findByNameIn(any())).thenReturn(List.of(new Role()));

        assertThat(userService.registerUser(request)).isNotNull();
    }

    @Test
    @DisplayName("a duplicate username is refused")
    void duplicateUsernameIsRefused() {
        when(userRepository.existsByUsername("newUser")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(registration()))
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("Username already exists"));
    }

    @Test
    @DisplayName("loadUserByUsername translates a missing user into the Spring Security type")
    void loadUserByUsernameTranslatesFailure() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    @DisplayName("change-password requires the OTP first, then the old password")
    void changePasswordChecksOtpThenOldPassword() throws Exception {
        User user = user("alice", "Old@Pass123");
        authenticateAs(user);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOtp("123456");
        request.setOldPassword("Old@Pass123");
        request.setPassword("New@Pass456");

        when(otpService.validateOtp("alice", "123456")).thenReturn(false);
        assertThatThrownBy(() -> userService.changePassword(request))
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("Otp is incorrect"));
        verify(userRepository, never()).save(any());

        when(otpService.validateOtp("alice", "123456")).thenReturn(true);
        request.setOldPassword("Wrong@Pass999");
        assertThatThrownBy(() -> userService.changePassword(request))
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("Old password is incorrect"));
        verify(userRepository, never()).save(any());

        request.setOldPassword("Old@Pass123");
        assertThat(userService.changePassword(request).getMessage()).isEqualTo("Password changed successfully");
        assertThat(passwordEncoder.matches("New@Pass456", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("revoking sessions stamps the watermark for the caller only")
    void revokeAllSessionsStampsTheCaller() throws Exception {
        User user = user("alice", "Old@Pass123");
        authenticateAs(user);

        assertThat(userService.revokeAllSessions().getMessage())
                .isEqualTo("All active sessions have been logged out");
        verify(userRepository).revokeTokensIssuedBefore(org.mockito.ArgumentMatchers.eq("alice"), any());
    }

    @Test
    @DisplayName("an unauthenticated caller cannot reach the self-service operations")
    void selfServiceRequiresAuthentication() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> UserService.getCurrentUserDetails())
                .isInstanceOf(LogOnlyException.class)
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("User not authenticated"));
    }

    @Test
    @DisplayName("a principal that is not a User is rejected rather than cast-crashing")
    void nonUserPrincipalIsRejected() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("just-a-string", null));

        assertThatThrownBy(() -> UserService.getCurrentUserDetails()).isInstanceOf(LogOnlyException.class);
    }

    @Test
    @DisplayName("toggling 2FA persists the new setting")
    void toggleTwoFactor() throws Exception {
        User user = user("alice", "Old@Pass123");
        authenticateAs(user);

        assertThat(userService.toggleTwoFactorAuth(true).getMessage()).contains("enabled");
        assertThat(user.isTwoFactorEnabled()).isTrue();

        assertThat(userService.toggleTwoFactorAuth(false).getMessage()).contains("disabled");
        assertThat(user.isTwoFactorEnabled()).isFalse();
    }

    @Test
    @DisplayName("unblocking is a no-op for an account that is not locked")
    void unblockOnlyActsOnLockedAccounts() throws Exception {
        User unlocked = user("alice", "pw");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(unlocked));
        assertThat(userService.unblockUser("alice")).isFalse();
        verify(userRepository, never()).save(any());

        User locked = user("bob", "pw");
        locked.lockAccount(30);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(locked));
        assertThat(userService.unblockUser("bob")).isTrue();
        assertThat(locked.isAccountLocked()).isFalse();

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThat(userService.unblockUser("ghost")).isFalse();
    }

    @Test
    @DisplayName("the primary role prefers ADMIN, otherwise the first role")
    void primaryRole() {
        User admin = user("alice", "pw");
        assertThat(userService.getPrimaryRole(admin)).isEqualTo("ADMIN");

        User plain = user("bob", "pw");
        Role userRole = new Role();
        userRole.setName("USER");
        plain.setRoles(new ArrayList<>(List.of(userRole)));
        assertThat(userService.getPrimaryRole(plain)).isEqualTo("USER");

        User roleless = user("carol", "pw");
        roleless.setRoles(new ArrayList<>());
        assertThat(userService.getPrimaryRole(roleless)).isEmpty();
        assertThat(userService.getPrimaryRole(null)).isEmpty();
    }

    @Test
    @DisplayName("saving a null user is refused rather than NPEing in the repository")
    void saveNullUserIsRefused() {
        assertThatThrownBy(() -> userService.saveUser(null)).isInstanceOf(LogOnlyException.class);
    }

    @Test
    @DisplayName("2FA status for an unknown user is reported false, not thrown")
    void twoFactorStatusForUnknownUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThat(userService.isTwoFactorEnabled("ghost")).isFalse();
    }

    @Test
    @DisplayName("init seeds the default admin when absent and is idempotent afterwards")
    void initSeedsTheDefaultAdmin() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        when(roleRepository.findByNameIn(List.of("ADMIN"))).thenReturn(List.of(adminRole));

        userService.init();
        verify(userRepository).save(any());

        // Already present with roles: nothing further is written.
        User existing = user("admin", "Admin@12345");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));
        org.mockito.Mockito.reset(userRepository);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));
        userService.init();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePasswordUnchecked skips the OTP and old-password gates - it is the reset path")
    void changePasswordUncheckedBypassesGates() throws Exception {
        User user = user("alice", "Old@Pass123");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        userService.changePasswordUnchecked("alice", "Reset@Pass456");

        assertThat(passwordEncoder.matches("Reset@Pass456", user.getPassword())).isTrue();
        verify(otpService, never()).validateOtp(anyString(), anyString());
    }
}
