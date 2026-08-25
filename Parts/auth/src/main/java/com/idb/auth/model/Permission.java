package com.idb.auth.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permissions")
public class Permission extends AuditableModel {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "url")
    private String url;

    @Column(name = "route")
    private String route;

    /**
     * Roles come from permissions.json, not from the database - the persisted side
     * of the relationship lives on {@link Role#getPermissions()}. This field only
     * carries the parsed file contents through the import.
     */
    @Transient
    private transient List<String> roles = new ArrayList<>();

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj instanceof Permission other && other.getName() != null && other.getName().equals(name);
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }
}
