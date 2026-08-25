package com.idb.auth.service.impl;

import static com.idb.auth.common.constant.OperationStatus.SUCCESS;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.common.util.StringUtil;
import com.idb.auth.common.util.ValidationUtil;
import com.idb.auth.dao.RoleRepository;
import com.idb.auth.dao.UserRepository;
import com.idb.auth.dto.request.ChangePasswordRequest;
import com.idb.auth.dto.request.UserRegistrationRequest;
import com.idb.auth.model.User;
import com.idb.auth.service.OtpService;
import com.idb.auth.service.UserService;
import com.idb.auth.util.AuthUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Value("${credentials.default.username:admin}")
    private String defaultUsername;

    @Value("${credentials.default.password:password}")
    private String defaultPassword;

    @Value("${credentials.default.role:ADMIN}")
    private String defaultRoleName;

    @Value("${credentials.default.phone}")
    private String adminPhone;

    @Value("${credentials.default.email}")
    private String adminEmail;

    @Value("${credentials.default.two-factor-enabled:false}")
    private boolean defaultTwoFactorEnabled;

    @Override
    @Transactional
    public void init() {
        User user = userRepository.findByUsername(defaultUsername).orElseGet(User::new);
        if (user.hasId() && !CollectionUtils.isEmpty(user.getRoles())) {
            return;
        }
        if (!user.hasId()) {
            user.setUsername(defaultUsername);
            user.setPassword(passwordEncoder.encode(defaultPassword));
            user.setEmail(adminEmail);
            user.setPhone(adminPhone);
            user.setTwoFactorEnabled(defaultTwoFactorEnabled);
        }
        if (CollectionUtils.isEmpty(user.getRoles())) {
            user.setRoles(new ArrayList<>(roleRepository.findByNameIn(List.of(defaultRoleName))));
        }
        userRepository.save(user);
        log.info("Default user '{}' initialised with role '{}'", defaultUsername, defaultRoleName);
    }

    @Override
    @Transactional
    public User registerUser(UserRegistrationRequest request) throws LogOnlyException {
        validateUserRegistration(request);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRoles(new ArrayList<>(roleRepository.findByNameIn(request.getRoles())));

        return userRepository.save(user);
    }

    private void validateUserRegistration(UserRegistrationRequest request) throws LogOnlyException {
        if (!AuthUtil.isValidUsername(request.getUsername())) {
            throw LogOnlyException.of(null, "Invalid username");
        }
        if (!AuthUtil.isValidStrongPassword(request.getPassword())) {
            throw LogOnlyException.of(null, "Invalid password");
        }
        if (StringUtil.isNotBlank(request.getEmail()) && !ValidationUtil.isValidEmail(request.getEmail())) {
            throw LogOnlyException.of(null, "Invalid email");
        }
        if (!ValidationUtil.isValidPhone(request.getPhone())) {
            throw LogOnlyException.of(null, "Invalid phone number");
        }
        if (CollectionUtils.isEmpty(request.getRoles())) {
            throw LogOnlyException.of(null, "Roles are required");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw LogOnlyException.of("Username already exists %s", "Username already exists", request.getUsername());
        }
    }

    @Override
    @Transactional
    public User update(UserRegistrationRequest request, User user) throws LogOnlyException {
        final User oldUser = validateUserUpdate(request, user);
        if (StringUtil.isNotEmpty(request.getPassword())) {
            oldUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (StringUtil.isNotEmpty(request.getEmail())) {
            oldUser.setEmail(request.getEmail());
        }
        if (StringUtil.isNotEmpty(request.getPhone())) {
            oldUser.setPhone(request.getPhone());
        }
        if (!CollectionUtils.isEmpty(request.getRoles())) {
            oldUser.getRoles().clear();
            oldUser.getRoles().addAll(roleRepository.findByNameIn(request.getRoles()));
        }
        return userRepository.save(oldUser);
    }

    private User validateUserUpdate(UserRegistrationRequest request, User user) throws LogOnlyException {
        User target = user != null ? user
                : userRepository.findById(request.getId())
                        .orElseThrow(() -> LogOnlyException.of(null, "User not found"));
        if (StringUtil.isNotEmpty(request.getEmail()) && !request.getEmail().equals(target.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw LogOnlyException.of(null, "Email already exists");
        }
        if (StringUtil.isNotEmpty(request.getPhone()) && !request.getPhone().equals(target.getPhone())
                && userRepository.existsByPhone(request.getPhone())) {
            throw LogOnlyException.of(null, "Phone number already exists");
        }
        if (StringUtil.isNotBlank(request.getPassword()) && !AuthUtil.isValidStrongPassword(request.getPassword())) {
            throw LogOnlyException.of(null, "Invalid password");
        }
        return target;
    }

    @Override
    @Transactional
    public ApiResponse<?> changePassword(ChangePasswordRequest request) throws TraceableException, LogOnlyException {
        User user = UserService.getCurrentUserDetails();
        try {
            if (!otpService.validateOtp(user.getUsername(), request.getOtp())) {
                throw LogOnlyException.of("User %s attempted to change password with incorrect otp",
                        "Otp is incorrect", user.getUsername());
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw LogOnlyException.of("User %s attempted to change password with incorrect old password",
                        "Old password is incorrect", user.getUsername());
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
            return ApiResponse.builder().status(SUCCESS).message("Password changed successfully").build();
        } catch (LogOnlyException e) {
            throw e;
        } catch (Exception e) {
            throw TraceableException.of("Password update failed for user %s", e, "Password update failed",
                    user.getUsername());
        }
    }

    @Override
    @Transactional
    public void changePasswordUnchecked(String username, String newPassword) throws LogOnlyException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> LogOnlyException.of(null, "User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            return findByUsername(username);
        } catch (LogOnlyException e) {
            throw new UsernameNotFoundException("Invalid username or password");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) throws LogOnlyException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> LogOnlyException.of(null, "User not found"));
    }

    @Override
    public boolean isAdminUser(User user) {
        return user.getRoles().stream().anyMatch(role -> defaultRoleName.equals(role.getName()));
    }

    @Override
    @Transactional
    public User saveUser(User user) throws LogOnlyException {
        if (user == null) {
            throw LogOnlyException.of(null, "User cannot be null");
        }
        return userRepository.save(user);
    }

    @Override
    public String getPrimaryRole(User user) {
        if (user == null || CollectionUtils.isEmpty(user.getRoles())) {
            return "";
        }
        if (isAdminUser(user)) {
            return defaultRoleName;
        }
        return user.getRoles().get(0).getName();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional
    public ApiResponse<String> toggleTwoFactorAuth(boolean enabled) throws TraceableException {
        try {
            User user = UserService.getCurrentUserDetails();
            user.setTwoFactorEnabled(enabled);
            userRepository.save(user);

            return ApiResponse.<String>builder()
                    .status(SUCCESS)
                    .message(enabled ? "Two-factor authentication enabled successfully"
                            : "Two-factor authentication disabled successfully")
                    .build();
        } catch (Exception e) {
            throw TraceableException.of("Failed to toggle two-factor authentication", e,
                    "Failed to update two-factor authentication settings");
        }
    }

    @Override
    public boolean isTwoFactorEnabled(String username) {
        try {
            return findByUsername(username).isTwoFactorEnabled();
        } catch (LogOnlyException e) {
            log.error("User not found when checking 2FA status: {}", username);
            return false;
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> revokeAllSessions() throws LogOnlyException {
        User user = UserService.getCurrentUserDetails();
        userRepository.revokeTokensIssuedBefore(user.getUsername(), LocalDateTime.now());
        log.info("Revoked all sessions for user {}", user.getUsername());
        return ApiResponse.<String>builder()
                .status(SUCCESS)
                .message("All active sessions have been logged out")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllBlockedUsers() {
        return userRepository.findAllLockedUsers();
    }

    @Override
    @Transactional
    public boolean unblockUser(String username) throws TraceableException {
        try {
            return userRepository.findByUsername(username)
                    .map(user -> {
                        if (user.isAccountLocked()) {
                            user.resetFailedLoginAttempts();
                            userRepository.save(user);
                            log.info("User {} has been manually unblocked", username);
                            return true;
                        }
                        return false;
                    })
                    .orElse(false);
        } catch (Exception e) {
            throw TraceableException.of("Failed to unblock user %s", e, "Failed to unblock user", username);
        }
    }
}
