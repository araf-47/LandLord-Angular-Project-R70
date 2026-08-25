package com.idb.auth.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.idb.auth.common.util.StringUtil;
import com.idb.auth.dao.RoleRepository;
import com.idb.auth.model.Permission;
import com.idb.auth.model.Role;
import com.idb.auth.service.RoleService;
import com.idb.auth.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserService userService;

    @Value("${credentials.default.role:ADMIN}")
    private String defaultRoleName;

    @Override
    @Transactional(readOnly = true)
    public List<String> findByActive() {
        return roleRepository.findActiveNames().stream()
                .filter(role -> !role.equals(defaultRoleName))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    @Override
    @Transactional
    public Role save(Role role) {
        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public void createInitialRoles(List<Permission> permissions, boolean clearExistingPermissions) {
        try {
            Map<String, Role> roles = new HashMap<>();
            for (Permission permission : permissions) {
                for (String roleName : permission.getRoles()) {
                    Role role = roles.get(roleName);
                    if (role != null) {
                        role.getPermissions().add(permission);
                        continue;
                    }
                    role = roleRepository.findByName(roleName).orElseGet(Role::new);
                    if (StringUtil.isEmpty(role.getName())) {
                        Set<Permission> rolePermissions = new HashSet<>();
                        rolePermissions.add(permission);
                        role.setName(roleName);
                        role.setPermissions(rolePermissions);
                    } else {
                        if (clearExistingPermissions) {
                            role.getPermissions().clear();
                        }
                        role.getPermissions().add(permission);
                    }
                    roles.put(roleName, role);
                }
            }
            if (!CollectionUtils.isEmpty(roles)) {
                roleRepository.saveAll(roles.values());
                userService.init();
            }
        } catch (Exception e) {
            log.error("Error creating initial roles", e);
        }
    }
}
