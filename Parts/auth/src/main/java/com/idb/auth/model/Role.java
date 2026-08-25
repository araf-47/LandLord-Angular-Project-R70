package com.idb.auth.model;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.LAZY;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A role IS the granted authority - {@link #getAuthority()} returns the role
 * name verbatim, with no {@code ROLE_} prefix. That is why authorization rules
 * use {@code hasAnyAuthority} rather than {@code hasAnyRole}.
 */
@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role extends AuditableModel implements GrantedAuthority {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @JsonIgnore
    @BatchSize(size = 999)
    @ManyToMany(fetch = LAZY, cascade = { MERGE, PERSIST })
    @JoinTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id", referencedColumnName = "id"))
    private Set<Permission> permissions = new HashSet<>();

    @Override
    public String getAuthority() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
