package com.idb.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.idb.auth.model.Permission;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("url")
    private String url;

    @JsonProperty("route")
    private String route;

    public PermissionResponse(Permission permission) {
        this.id = permission.getId() == null ? null : permission.getId().toString();
        this.name = permission.getName();
        this.url = permission.getUrl();
        this.route = permission.getRoute();
    }
}
