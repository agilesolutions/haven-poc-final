package com.agilesolutions.service_b.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity model representing entity information stored in the database.
 * 
 * This entity is retrieved by external callers through Service A
 * and returned with entity information (name, description, version).
 */
@jakarta.persistence.Entity
@Table(name = "entity", indexes = {
        @Index(name = "idx_entity_name", columnList = "name"),
        @Index(name = "idx_entity_active", columnList = "is_active"),
        @Index(name = "idx_entity_created_at", columnList = "created_at"),
        @Index(name = "idx_entity_updated_at", columnList = "updated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entity {

    /**
     * Unique identifier for the entity
     */
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    /**
     * Name of the entity
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Description of the entity
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Version of the entity
     */
    @Column(name = "version", nullable = false, length = 50)
    private String version;

    /**
     * Timestamp when the entity was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the entity was last updated
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User who created this entity
     */
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /**
     * User who last updated this entity
     */
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    /**
     * Flag indicating if the entity is active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Pre-persist hook to set creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    /**
     * Pre-update hook to update the last updated timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

