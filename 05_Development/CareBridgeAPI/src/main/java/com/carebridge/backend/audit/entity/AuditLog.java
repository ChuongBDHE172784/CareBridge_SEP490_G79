package com.carebridge.backend.audit.entity;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
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
// Belt and braces: new journey/baby event rows are written with
// event_origin = 'JOURNEY_EVENT', but rows migrated by the canonical
// convergence migration (and rows on the immutable live table, which cannot
// be updated) still carry the default event_origin = 'AUDIT_LOG' with a
// journey event_category. Those categories are not AuditAction constants, so
// they must be excluded here or loading them through AuditLog crashes enum
// hydration.
@org.hibernate.annotations.SQLRestriction("""
        event_origin = 'AUDIT_LOG'
        AND event_category NOT IN (
            'BASELINE_CONTEXT',
            'MOTHER_JOURNEY_TRANSITION',
            'PREGNANCY_OUTCOME_EVIDENCE',
            'SAFETY_OUTCOME',
            'DATA_MIGRATION',
            'MODERATION_REVIEW',
            'MODERATION_APPROVE',
            'MODERATION_HIDE',
            'MODERATION_LOCK',
            'MODERATION_REQUEST_REVISION',
            'MODERATION_LABEL',
            'MODERATION_WARN',
            'MODERATION_SUSPEND',
            'MODERATION_RESTRICT',
            'MODERATION_ESCALATE',
            'MODERATION_UNDO',
            'MODERATION_AI_FEEDBACK_SUBMITTED')
        AND event_category NOT LIKE 'BABY\\_LINK\\_%'
        AND event_category NOT LIKE 'MOTHER\\_JOURNEY\\_%'
        """)
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

    @Column(name = "actor_type", length = 20)
    private String actorType;

    @Column(name = "actor_service", length = 80)
    private String actorService;

    @Column(name = "subject_user_id")
    private java.util.UUID subjectUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_category", nullable = false, length = 80)
    private AuditAction action;

    @Column(name = "resource_type", length = 100)
    private String entityType;

    @Column(name = "resource_id")
    private java.util.UUID entityId;

    @Column(name = "reason_code", length = 80)
    private String reasonCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "care_context_type", length = 10)
    private ChecklistCareContextType careContextType;

    @Column(name = "care_context_id")
    private java.util.UUID careContextId;

    @Column(name = "template_version_id")
    private java.util.UUID templateVersionId;

    @Column(name = "checklist_task_instance_id")
    private java.util.UUID checklistTaskInstanceId;

    @Column(name = "correlation_id")
    private java.util.UUID correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_payload_jsonb", columnDefinition = "jsonb")
    private String newValueJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_payload_jsonb", columnDefinition = "jsonb")
    private String oldValueJson;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Builder.Default
    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = Boolean.FALSE;

    @Builder.Default
    @Column(name = "event_origin", nullable = false, updatable = false)
    private String eventOrigin = "AUDIT_LOG";

    @PrePersist
    void prepareCanonicalEvent() {
        if (eventOrigin == null || eventOrigin.isBlank()) {
            eventOrigin = "AUDIT_LOG";
        }
    }

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Audit logs are append-only");
    }
}
