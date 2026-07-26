package com.carebridge.backend.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
@org.hibernate.annotations.SQLRestriction("event_origin = 'AUDIT_LOG'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_event_id", updatable = false, nullable = false)
    private java.util.UUID auditLogId;

    @Column(name = "occurred_at", nullable = false)
    private Instant createdAt;

    @Column(name = "actor_user_id")
    private java.util.UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_category", nullable = false, length = 80)
    private AuditAction action;

    @Column(name = "resource_type", length = 100)
    private String entityType;

    @Column(name = "resource_id")
    private java.util.UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_payload_jsonb", columnDefinition = "jsonb")
    private String newValueJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_payload_jsonb", columnDefinition = "jsonb")
    private String oldValueJson;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Builder.Default
    @Column(name = "event_origin", nullable = false, updatable = false)
    private String eventOrigin = "AUDIT_LOG";

    @PrePersist
    void prepareCanonicalEvent() {
        eventOrigin = "AUDIT_LOG";
    }

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Audit logs are append-only");
    }
}
