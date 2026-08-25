package com.idb.auth.service;

import java.util.List;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.dto.response.PermissionResponse;
import com.idb.auth.model.Permission;

public interface PermissionService {

    List<PermissionResponse> getUserPermissions(String username) throws LogOnlyException;

    List<Permission> getAll();

    List<Permission> save(List<Permission> permissions);

    /**
     * Imports permissions.json and returns the url-to-roles matcher list that
     * {@code SecurityConfig} registers. Order is significant - see the
     * implementation.
     */
    List<Permission> loadPermissionsFromResource();

    ApiResponse<String> updateRolePermissions(Long roleId, List<Long> permissionIds)
            throws LogOnlyException, TraceableException;
}
