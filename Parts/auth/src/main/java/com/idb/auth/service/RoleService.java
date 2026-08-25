package com.idb.auth.service;

import java.util.List;
import java.util.Optional;

import com.idb.auth.model.Permission;
import com.idb.auth.model.Role;

public interface RoleService {

    List<String> findByActive();

    /**
     * Creates or updates the roles referenced by permissions.json and wires their
     * permission sets, then seeds the default admin user.
     */
    void createInitialRoles(List<Permission> permissions, boolean clearExistingPermissions);

    Optional<Role> findById(Long id);

    Role save(Role role);
}
