package com.idb.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Key/value application settings. Only used here to remember which version of
 * permissions.json has already been imported, so an unchanged file is not
 * re-imported on every start.
 */
@Getter
@Setter
@Entity
@Table(name = "configurations")
public class Configuration extends AuditableModel {

    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value")
    private String configValue;
}
