package com.moneysab.cardexis.domain.entity;

import com.moneysab.cardexis.domain.entity.BaseEntity;
import jakarta.persistence.*;

/**
 * Role entity - shared with API-1 for authentication.
 */
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(name = "role_name", unique = true, nullable = false)
    private String roleName;

    @Column(name = "description")
    private String description;

    // Getters and Setters
    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}