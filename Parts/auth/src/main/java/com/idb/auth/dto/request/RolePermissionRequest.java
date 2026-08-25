package com.idb.auth.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionRequest {
    @NotNull(message = "Role ID is required")
    private Long roleId;

    @NotEmpty(message = "At least one permission ID is required")
    private List<Long> permissionIds;
}
