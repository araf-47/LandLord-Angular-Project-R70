package com.idb.auth.controller;

import static com.idb.auth.common.constant.OperationStatus.SUCCESS;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_GET_USER_PERMISSIONS;
import static com.idb.auth.constant.AuthConstants.ENDPOINT_ROLE_PERMISSIONS;
import static com.idb.auth.constant.AuthConstants.URL_PERMISSION_CONTROLLER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.dto.request.RolePermissionRequest;
import com.idb.auth.dto.response.PermissionResponse;
import com.idb.auth.service.PermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(URL_PERMISSION_CONTROLLER)
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping(value = ENDPOINT_GET_USER_PERMISSIONS, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getUserPermissions(Principal principal)
            throws LogOnlyException {
        return ResponseEntity.ok(ApiResponse.<List<PermissionResponse>>builder()
                .status(SUCCESS)
                .data(permissionService.getUserPermissions(principal.getName()))
                .build());
    }

    @PostMapping(value = ENDPOINT_ROLE_PERMISSIONS, consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> updateRolePermissions(
            @Valid @RequestBody RolePermissionRequest request) throws LogOnlyException, TraceableException {
        return ResponseEntity.ok(
                permissionService.updateRolePermissions(request.getRoleId(), request.getPermissionIds()));
    }
}
