package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.dao.ConfigurationRepository;
import com.idb.auth.dao.PermissionRepository;
import com.idb.auth.dao.RoleRepository;
import com.idb.auth.model.Configuration;
import com.idb.auth.model.Permission;
import com.idb.auth.model.Role;
import com.idb.auth.service.RoleService;
import com.idb.auth.service.UserService;
import com.idb.auth.service.impl.PermissionServiceImpl;
import com.idb.auth.service.impl.RoleServiceImpl;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionAndRoleServiceTest {

    @Mock private PermissionRepository permissionRepository;
    @Mock private ConfigurationRepository configurationRepository;
    @Mock private RoleService roleService;
    @Mock private RoleRepository roleRepository;
    @Mock private UserService userService;

    private PermissionServiceImpl permissionService;
    private RoleServiceImpl roleServiceImpl;
    private MockEnvironment env;

    @BeforeEach
    void setUp() {
        env = new MockEnvironment()
                .withProperty("permissions.file.path", "classpath:permissions.json")
                .withProperty("file.base.path", "./");
        permissionService = new PermissionServiceImpl(permissionRepository, configurationRepository,
                JsonMapper.builder().build(), env, roleService);
        roleServiceImpl = new RoleServiceImpl(roleRepository, userService);
        ReflectionTestUtils.setField(roleServiceImpl, "defaultRoleName", "ADMIN");

        when(permissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(permissionRepository.findByNameIn(anyList())).thenReturn(List.of());
        when(configurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Permission permission(String name, String url, String... roles) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setUrl(url);
        permission.setRoute("/route/" + name);
        permission.setRoles(new ArrayList<>(List.of(roles)));
        return permission;
    }

    @Test
    @DisplayName("save preserves declaration order, because URL matching is first-match-wins")
    void savePreservesDeclarationOrder() {
        List<Permission> input = List.of(
                permission("SPECIFIC", "/api/v3/thing/detail", "ADMIN"),
                permission("BROAD", "/api/v3/thing/**", "ADMIN"),
                permission("OTHER", "/api/v3/other", "USER"));

        List<Permission> saved = permissionService.save(input);

        // Hash ordering here would let the broad pattern shadow the specific rule on
        // some JVM runs and not others, making role enforcement nondeterministic.
        assertThat(saved).extracting(Permission::getName).containsExactly("SPECIFIC", "BROAD", "OTHER");
    }

    @Test
    @DisplayName("save drops entries with no name or no roles rather than registering an open matcher")
    void saveDropsIncompleteEntries() {
        List<Permission> saved = permissionService.save(List.of(
                permission("GOOD", "/api/v3/good", "ADMIN"),
                permission("NO_ROLES", "/api/v3/noroles"),
                permission(null, "/api/v3/noname", "ADMIN")));

        assertThat(saved).extracting(Permission::getName).containsExactly("GOOD");
    }

    @Test
    @DisplayName("save reuses an existing row so a re-import does not duplicate permissions")
    void saveReusesExistingRows() {
        Permission existing = permission("SAME", "/old/url", "ADMIN");
        existing.setId(42L);
        when(permissionRepository.findByNameIn(anyList())).thenReturn(List.of(existing));

        List<Permission> saved = permissionService.save(List.of(permission("SAME", "/new/url", "USER")));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getId()).isEqualTo(42L);
        assertThat(saved.get(0).getUrl()).isEqualTo("/new/url");
        // roles is @Transient, so it must be re-attached to the persisted instance
        // for createInitialRoles to see it.
        assertThat(saved.get(0).getRoles()).containsExactly("USER");
    }

    @Test
    @DisplayName("a duplicate name in the file keeps the first declaration")
    void duplicateNamesKeepTheFirst() {
        List<Permission> saved = permissionService.save(List.of(
                permission("DUP", "/first", "ADMIN"),
                permission("DUP", "/second", "USER")));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getUrl()).isEqualTo("/first");
    }

    @Test
    @DisplayName("an empty or null input is returned untouched")
    void emptyInputIsPassthrough() {
        assertThat(permissionService.save(List.of())).isEmpty();
        assertThat(permissionService.save(null)).isNull();
    }

    @Test
    @DisplayName("the real permissions.json imports, seeds roles and records its version")
    void loadsTheRealPermissionsFile() {
        when(configurationRepository.findByConfigKey("permission.file.version")).thenReturn(Optional.empty());

        List<Permission> loaded = permissionService.loadPermissionsFromResource();

        assertThat(loaded).isNotEmpty();
        assertThat(loaded).allSatisfy(p -> {
            assertThat(p.getName()).isNotBlank();
            assertThat(p.getRoles()).isNotEmpty();
        });

        ArgumentCaptor<Configuration> config = ArgumentCaptor.forClass(Configuration.class);
        verify(configurationRepository).save(config.capture());
        assertThat(config.getValue().getConfigKey()).isEqualTo("permission.file.version");
        assertThat(config.getValue().getConfigValue()).isNotBlank();

        verify(roleService).createInitialRoles(anyList(), org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    @DisplayName("an unchanged version is not re-imported, but the parsed rules are still returned")
    void unchangedVersionSkipsTheImport() {
        Configuration recorded = new Configuration();
        recorded.setConfigKey("permission.file.version");
        recorded.setConfigValue("99.0.0.0");
        when(configurationRepository.findByConfigKey("permission.file.version")).thenReturn(Optional.of(recorded));

        List<Permission> loaded = permissionService.loadPermissionsFromResource();

        // SecurityConfig still needs the matcher list, so returning it is required
        // even when nothing was written.
        assertThat(loaded).isNotEmpty();
        verify(permissionRepository, never()).save(any());
        verify(roleService, never()).createInitialRoles(anyList(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    @DisplayName("the version is upserted, so a first run actually records it")
    void versionIsUpsertedNotBlindlyUpdated() {
        when(configurationRepository.findByConfigKey("permission.file.version")).thenReturn(Optional.empty());

        permissionService.loadPermissionsFromResource();

        // The reference issued a bare UPDATE, which affects zero rows when none
        // exists - so the version stayed null and the file was re-imported on every
        // single start.
        verify(configurationRepository).save(any(Configuration.class));
    }

    @Test
    @DisplayName("startup refuses to continue when no permissions could be loaded")
    void emptyMatcherSetIsFatal() {
        env.setProperty("permissions.file.path", "classpath:does-not-exist.json");

        assertThatThrownBy(() -> permissionService.loadPermissionsFromResource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to start");
    }

    @Test
    @DisplayName("a missing permissions.file.path is fatal rather than silently permissive")
    void missingPathIsFatal() {
        env.setProperty("permissions.file.path", "");

        assertThatThrownBy(() -> permissionService.loadPermissionsFromResource())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("updating a role's permissions replaces the set and reports partial matches")
    void updateRolePermissionsReplaces() throws Exception {
        Role role = new Role();
        role.setName("USER");
        role.getPermissions().add(permission("OLD", "/old", "USER"));
        when(roleService.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(permission("NEW1", "/n1", "USER"), permission("NEW2", "/n2", "USER")));

        assertThat(permissionService.updateRolePermissions(1L, List.of(10L, 11L)).getMessage())
                .isEqualTo("Permissions updated successfully");
        assertThat(role.getPermissions()).extracting(Permission::getName).containsExactlyInAnyOrder("NEW1", "NEW2");

        // A requested id that does not resolve is reported rather than silently lost.
        when(permissionRepository.findAllById(List.of(10L, 999L)))
                .thenReturn(List.of(permission("NEW1", "/n1", "USER")));
        assertThat(permissionService.updateRolePermissions(1L, List.of(10L, 999L)).getMessage())
                .isEqualTo("Permissions updated partially.");
    }

    @Test
    @DisplayName("an empty permission list clears the role")
    void updateRolePermissionsWithEmptyListClears() throws Exception {
        Role role = new Role();
        role.getPermissions().add(permission("OLD", "/old", "USER"));
        when(roleService.findById(1L)).thenReturn(Optional.of(role));

        permissionService.updateRolePermissions(1L, List.of());

        assertThat(role.getPermissions()).isEmpty();
    }

    @Test
    @DisplayName("an unknown role id is a not-found error, not a silent no-op")
    void unknownRoleIsRejected() {
        when(roleService.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.updateRolePermissions(99L, List.of(1L)))
                .isInstanceOf(LogOnlyException.class)
                .satisfies(e -> assertThat(((LogOnlyException) e).getResponse().getMessage())
                        .isEqualTo("Role not found"));
    }

    @Test
    @DisplayName("createInitialRoles groups permissions per role without duplicating a role")
    void createInitialRolesGroupsByRole() {
        when(roleRepository.findByName(any())).thenReturn(Optional.empty());

        roleServiceImpl.createInitialRoles(List.of(
                permission("A", "/a", "ADMIN", "USER"),
                permission("B", "/b", "ADMIN"),
                permission("C", "/c", "USER")), true);

        // Captured as Iterable, not List: createInitialRoles passes the map's
        // values() view, and Mockito 5's captor matcher is type-aware.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Role>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(roleRepository).saveAll(captor.capture());
        List<Role> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);

        assertThat(saved).extracting(Role::getName).containsExactlyInAnyOrder("ADMIN", "USER");
        assertThat(saved).allSatisfy(role -> assertThat(role.getPermissions()).isNotEmpty());
        Role admin = saved.stream().filter(r -> r.getName().equals("ADMIN")).findFirst().orElseThrow();
        assertThat(admin.getPermissions()).extracting(Permission::getName).containsExactlyInAnyOrder("A", "B");

        // Seeding the default user is chained off role creation.
        verify(userService).init();
    }

    @Test
    @DisplayName("an existing role has its set cleared first when the file says so")
    void createInitialRolesRespectsClearFlag() {
        Role existing = new Role();
        existing.setName("ADMIN");
        existing.getPermissions().add(permission("STALE", "/stale", "ADMIN"));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(existing));

        roleServiceImpl.createInitialRoles(List.of(permission("FRESH", "/fresh", "ADMIN")), true);

        assertThat(existing.getPermissions()).extracting(Permission::getName).containsExactly("FRESH");
    }

    @Test
    @DisplayName("with the clear flag off, an existing role accumulates instead")
    void createInitialRolesCanAccumulate() {
        Role existing = new Role();
        existing.setName("ADMIN");
        existing.getPermissions().add(permission("KEEP", "/keep", "ADMIN"));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(existing));

        roleServiceImpl.createInitialRoles(List.of(permission("FRESH", "/fresh", "ADMIN")), false);

        assertThat(existing.getPermissions()).extracting(Permission::getName)
                .containsExactlyInAnyOrder("KEEP", "FRESH");
    }

    @Test
    @DisplayName("role listing hides the default admin role")
    void roleListingHidesTheDefaultRole() {
        when(roleRepository.findActiveNames()).thenReturn(List.of("ADMIN", "USER", "AUDITOR"));

        assertThat(roleServiceImpl.findByActive()).containsExactly("USER", "AUDITOR");
    }
}
