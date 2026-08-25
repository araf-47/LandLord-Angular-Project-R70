package com.idb.auth.service;

import static com.idb.auth.constant.AuthConstants.ROLE_ADMIN;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.dto.request.ChangePasswordRequest;
import com.idb.auth.dto.request.UserRegistrationRequest;
import com.idb.auth.model.User;

public interface UserService extends UserDetailsService {

    User registerUser(UserRegistrationRequest request) throws LogOnlyException;

    User update(UserRegistrationRequest request, User oldUser) throws LogOnlyException;

    ApiResponse<?> changePassword(ChangePasswordRequest request) throws LogOnlyException, TraceableException;

    void changePasswordUnchecked(String username, String newPassword) throws LogOnlyException;

    ApiResponse<String> revokeAllSessions() throws LogOnlyException;

    ApiResponse<String> toggleTwoFactorAuth(boolean enabled) throws TraceableException;

    boolean isTwoFactorEnabled(String username);

    User findByUsername(String username) throws LogOnlyException;

    User saveUser(User user) throws LogOnlyException;

    boolean existsByUsername(String username);

    boolean isAdminUser(User user);

    String getPrimaryRole(User user);

    List<User> findAllBlockedUsers();

    boolean unblockUser(String username) throws TraceableException;

    /** Seeds the default administrator if it does not yet exist. */
    void init();

    static User getCurrentUserDetails() throws LogOnlyException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw LogOnlyException.of(null, "User not authenticated");
        }
        return user;
    }

    static boolean isAdminUser() throws LogOnlyException {
        User user = getCurrentUserDetails();
        return user.getRoles().stream().anyMatch(role -> ROLE_ADMIN.equals(role.getName()));
    }

    static void validateAdminAccess() throws LogOnlyException {
        if (!isAdminUser()) {
            throw LogOnlyException.of("Access denied. Admin privileges required.", "Unauthorized access attempt");
        }
    }
}
