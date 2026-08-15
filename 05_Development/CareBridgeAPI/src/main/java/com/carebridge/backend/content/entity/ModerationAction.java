package com.carebridge.backend.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_events")
@org.hibernate.annotations.SQLRestriction("event_category LIKE 'MODERATION_%' AND event_category <> 'MODERATION_ACTION'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_event_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "subject_reference_id", columnDefinition = "uuid")
    private UUID reportId;

    @Column(name = "resource_id", columnDefinition = "uuid")
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 30)
    private ReportTargetType targetType;

    @Transient
    private ModerationActionType actionType;

    @Column(name = "actor_user_id", columnDefinition = "uuid")
    private UUID moderatorUserId;

    @Transient
    private String reason;

    @Column(name = "occurred_at")
    private Instant actionAt;

    @Transient
    private Instant expiresAt;

    @Column(name = "event_category", nullable = false, length = 80)
    private String eventCategory;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @PrePersist
    @PreUpdate
    void prepareCanonicalEvent() {
        eventCategory = actionType == null ? "MODERATION_REVIEW" : "MODERATION_" + actionType.name();
        if (payload == null) payload = new LinkedHashMap<>();
        if (reason == null) payload.remove("reason"); else payload.put("reason", reason);
        if (expiresAt == null) payload.remove("expiresAt"); else payload.put("expiresAt", expiresAt.toString());
    }

    @PostLoad
    void hydrateCanonicalEvent() {
        if (eventCategory != null && eventCategory.startsWith("MODERATION_")) {
            String value = eventCategory.substring("MODERATION_".length());
            try {
                actionType = ModerationActionType.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                actionType = null;
            }
        }
        if (payload == null) return;
        Object persistedReason = payload.get("reason");
        reason = persistedReason == null ? null : persistedReason.toString();
        Object persistedExpiry = payload.get("expiresAt");
        expiresAt = persistedExpiry == null || persistedExpiry.toString().isBlank()
                ? null : Instant.parse(persistedExpiry.toString());
    }
}
