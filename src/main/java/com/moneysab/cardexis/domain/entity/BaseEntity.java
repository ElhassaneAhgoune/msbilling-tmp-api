package com.moneysab.cardexis.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Base entity class providing common fields and functionality for all domain entities.
 * 
 * This abstract class implements the foundation for all entities in the Visa EPIN system,
 * providing UUID-based primary keys, audit timestamps, and optimistic locking support.
 * 
 * Features:
 * - UUID primary key generation
 * - Automatic audit timestamp management
 * - Optimistic locking with version control
 * - JPA lifecycle callbacks
 * - Consistent equals/hashCode implementation
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    /**
     * Unique identifier for the entity.
     * Uses UUID for globally unique identification across distributed systems.
     */
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;
    
    /**
     * Timestamp when the entity was first created.
     * Automatically populated by Spring Data JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the entity was last modified.
     * Automatically updated by Spring Data JPA auditing on each save operation.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Version field for optimistic locking.
     * Prevents concurrent modification conflicts by tracking entity version.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
    /**
     * Default constructor for JPA.
     */
    protected BaseEntity() {
        // JPA requires a no-arg constructor
    }
    
    /**
     * Gets the unique identifier of the entity.
     * 
     * @return the entity ID, or null if not yet persisted
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Sets the unique identifier of the entity.
     * Note: This should typically only be used by the persistence framework.
     * 
     * @param id the entity ID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }
    
    /**
     * Gets the creation timestamp of the entity.
     * 
     * @return the timestamp when the entity was created
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the creation timestamp of the entity.
     * Note: This is typically managed automatically by JPA auditing.
     * 
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Gets the last modification timestamp of the entity.
     * 
     * @return the timestamp when the entity was last updated
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Sets the last modification timestamp of the entity.
     * Note: This is typically managed automatically by JPA auditing.
     * 
     * @param updatedAt the last modification timestamp to set
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Gets the version number for optimistic locking.
     * 
     * @return the current version number
     */
    public Long getVersion() {
        return version;
    }
    
    /**
     * Sets the version number for optimistic locking.
     * Note: This is typically managed automatically by JPA.
     * 
     * @param version the version number to set
     */
    public void setVersion(Long version) {
        this.version = version;
    }
    
    /**
     * Checks if the entity is new (not yet persisted).
     * 
     * @return true if the entity has no ID, false otherwise
     */
    public boolean isNew() {
        return id == null;
    }
    
    /**
     * Lifecycle callback executed before the entity is persisted.
     * Can be overridden by subclasses to perform custom pre-persist logic.
     */
    @PrePersist
    protected void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }
    
    /**
     * Lifecycle callback executed before the entity is updated.
     * Can be overridden by subclasses to perform custom pre-update logic.
     */
    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Lifecycle callback executed after the entity is loaded from the database.
     * Can be overridden by subclasses to perform custom post-load logic.
     */
    @PostLoad
    protected void postLoad() {
        // Default implementation does nothing
        // Subclasses can override to add custom logic
    }
    
    /**
     * Compares this entity with another object for equality.
     * Two entities are considered equal if they have the same ID and are of the same type.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        BaseEntity that = (BaseEntity) obj;
        
        // If both entities are new (no ID), they are not equal
        if (id == null || that.id == null) {
            return false;
        }
        
        return Objects.equals(id, that.id);
    }
    
    /**
     * Generates a hash code for the entity based on its ID.
     * 
     * @return the hash code of the entity
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    /**
     * Returns a string representation of the entity.
     * 
     * @return string representation including class name and ID
     */
    @Override
    public String toString() {
        return String.format("%s{id=%s, version=%d, createdAt=%s, updatedAt=%s}",
            getClass().getSimpleName(), id, version, createdAt, updatedAt);
    }
}