package com.idb.auth.service.impl;

import static com.idb.auth.common.constant.OperationStatus.SUCCESS;
import static com.idb.auth.constant.AuthConstants.CONFIG_KEY_PERMISSION_FILE_VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.common.util.StringUtil;
import com.idb.auth.dao.ConfigurationRepository;
import com.idb.auth.dao.PermissionRepository;
import com.idb.auth.dto.response.PermissionResponse;
import com.idb.auth.model.Configuration;
import com.idb.auth.model.Permission;
import com.idb.auth.model.Role;
import com.idb.auth.service.PermissionService;
import com.idb.auth.service.RoleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final ConfigurationRepository configurationRepository;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final RoleService roleService;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getUserPermissions(String username) throws LogOnlyException {
        return permissionRepository.findByUsername(username).stream()
                .map(PermissionResponse::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> getAll() {
        return permissionRepository.findAllActive();
    }

    @Override
    @Transactional
    public List<Permission> save(List<Permission> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            return permissions;
        }

        // LinkedHashMap, not the default HashMap: the returned order becomes the
        // order SecurityConfig registers its requestMatchers in, and matching is
        // first-match-wins. With hash ordering, which broad pattern shadows which
        // specific rule could change between JVM runs, making role enforcement
        // nondeterministic. Declaration order in permissions.json must survive.
        Map<String, Permission> newPermissions = permissions.stream()
                .filter(p -> StringUtil.isNotEmpty(p.getName()) && !CollectionUtils.isEmpty(p.getRoles()))
                .collect(Collectors.toMap(Permission::getName, Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));

        Map<String, Permission> existingByName = new HashMap<>();
        permissionRepository.findByNameIn(List.copyOf(newPermissions.keySet()))
                .forEach(existing -> existingByName.put(existing.getName(), existing));

        List<Permission> orderedResult = new ArrayList<>();
        for (Permission permission : newPermissions.values()) {
            List<String> roles = permission.getRoles();
            Permission persistable = existingByName.getOrDefault(permission.getName(), permission);

            persistable.setUrl(permission.getUrl());
            persistable.setRoute(permission.getRoute());
            persistable = permissionRepository.save(persistable);
            // roles is @Transient - re-attach it so createInitialRoles can read it
            // off the now-persisted (and id-bearing) instance.
            persistable.setRoles(roles);
            orderedResult.add(persistable);
        }
        return orderedResult;
    }

    @Override
    @Transactional
    public List<Permission> loadPermissionsFromResource() {
        String configuredPath = environment.getProperty("permissions.file.path");
        List<Permission> permissions = new ArrayList<>();

        try {
            JsonNode permissionsJson = readPermissionsJson(configuredPath);
            if (permissionsJson != null) {
                String currentVersion = getPermissionFileVersion();
                String newVersion = permissionsJson.get("version").asText();
                boolean clearExistingPermissions = permissionsJson.get("clearExistingPermissions").asBoolean();
                permissions = getPermissionsFromConfigJson(permissionsJson.get("permissions"));

                if (StringUtil.isEmpty(currentVersion) || StringUtil.isUpdatedVersion(currentVersion, newVersion)) {
                    log.info("{} permissions from version {} to {}",
                            StringUtil.isEmpty(currentVersion) ? "Initial loading of" : "Updating",
                            currentVersion, newVersion);
                    if (!CollectionUtils.isEmpty(permissions)) {
                        permissions = save(permissions);
                        if (!CollectionUtils.isEmpty(permissions)) {
                            updatePermissionFileVersion(newVersion);
                            roleService.createInitialRoles(permissions, clearExistingPermissions);
                        }
                    }
                } else {
                    log.info("Permission version {} is up to date", currentVersion);
                    // The file was not re-imported, but the parsed roles are still what
                    // SecurityConfig needs, so they are returned as-is.
                }
            }
        } catch (IOException e) {
            log.error("Error loading permissions from {}", configuredPath, e);
        }

        if (CollectionUtils.isEmpty(permissions)) {
            throw new IllegalStateException("No security permissions were loaded from " + configuredPath
                    + ". Refusing to start with an empty authorization matcher set, which would leave every"
                    + " endpoint open to any authenticated user.");
        }
        return permissions;
    }

    /**
     * Accepts either {@code classpath:name.json} or {@code file:relative/name.json}
     * (resolved against {@code file.base.path}), matching the reference project's
     * two-part property format.
     */
    private JsonNode readPermissionsJson(String configuredPath) throws IOException {
        if (StringUtil.isEmpty(configuredPath)) {
            log.error("permissions.file.path is not configured");
            return null;
        }

        String[] parts = configuredPath.split(":", 2);
        String locationType = parts.length > 1 ? parts[0] : "classpath";
        String path = parts.length > 1 ? parts[1] : parts[0];

        if ("file".equals(locationType)) {
            Path resolved = Paths.get(environment.getProperty("file.base.path", "./"), path);
            return objectMapper.readTree(Files.readAllBytes(resolved));
        }

        Resource resource = new DefaultResourceLoader().getResource("classpath:" + path);
        if (!resource.exists()) {
            log.error("Permissions resource not found on classpath: {}", path);
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readTree(in);
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> updateRolePermissions(Long roleId, List<Long> permissionIds)
            throws LogOnlyException, TraceableException {
        try {
            Role role = roleService.findById(roleId)
                    .orElseThrow(() -> LogOnlyException.of("Role not found with id: " + roleId, "Role not found"));

            role.getPermissions().clear();

            String message = "Permissions updated ";
            if (!CollectionUtils.isEmpty(permissionIds)) {
                List<Permission> permissions = permissionRepository.findAllById(permissionIds);
                message += permissions.size() != permissionIds.size() ? "partially." : "successfully";
                role.getPermissions().addAll(permissions);
            }

            roleService.save(role);

            return ApiResponse.<String>builder().status(SUCCESS).message(message).build();
        } catch (LogOnlyException e) {
            throw e;
        } catch (Exception e) {
            throw TraceableException.of("Error in updateRolePermissions(): ", e, "Permission update failed");
        }
    }

    private List<Permission> getPermissionsFromConfigJson(JsonNode permissionsJson) {
        List<Permission> permissions = new ArrayList<>();
        if (permissionsJson == null) {
            return permissions;
        }
        // Jackson 3 renamed JsonNode.fields() to properties().
        permissionsJson.properties().forEach(entry -> {
            JsonNode valueNode = entry.getValue();

            Permission permission = new Permission();
            permission.setName(entry.getKey());
            permission.setUrl(valueNode.get("url").asText());
            permission.setRoute(valueNode.get("route").asText());

            JsonNode rolesNode = valueNode.get("roles");
            if (rolesNode != null && rolesNode.isArray()) {
                rolesNode.forEach(role -> permission.getRoles().add(role.asText()));
            }

            permissions.add(permission);
        });
        return permissions;
    }

    private String getPermissionFileVersion() {
        return configurationRepository.findByConfigKey(CONFIG_KEY_PERMISSION_FILE_VERSION)
                .map(Configuration::getConfigValue)
                .orElse(null);
    }

    /**
     * Upsert rather than the reference project's bare {@code UPDATE}: with no
     * pre-seeded row an UPDATE affects zero rows, the version stays null, and
     * permissions.json is re-imported on every single start.
     */
    private void updatePermissionFileVersion(String version) {
        Configuration config = configurationRepository.findByConfigKey(CONFIG_KEY_PERMISSION_FILE_VERSION)
                .orElseGet(() -> {
                    Configuration created = new Configuration();
                    created.setConfigKey(CONFIG_KEY_PERMISSION_FILE_VERSION);
                    return created;
                });
        config.setConfigValue(version);
        configurationRepository.save(config);
    }
}
