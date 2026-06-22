package com.carebridge.backend.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_log_id")
    private UUID id;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 80)
    private AuditAction action;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "old_value_json", columnDefinition = "jsonb")
    private String oldValueJson;

    @Column(name = "new_value_json", columnDefinition = "jsonb")
    private String newValueJson;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Instant getTimestamp() {
        return createdAt;
    }

    public void setTimestamp(Instant timestamp) {
        this.createdAt = timestamp;
    }

    public UUID getUserId() {
        return actorUserId;
    }

    public void setUserId(UUID userId) {
        this.actorUserId = userId;
    }

    public String getResourceType() {
        return entityType;
    }

    public void setResourceType(String resourceType) {
        this.entityType = resourceType;
    }

    public String getResourceId() {
        return entityId == null ? null : entityId.toString();
    }

    public void setResourceId(String resourceId) {
        this.entityId = parseUuid(resourceId);
    }

    public String getDetails() {
        return newValueJson;
    }

    public void setDetails(String details) {
        this.newValueJson = details;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Audit logs are append-only");
    }
}
