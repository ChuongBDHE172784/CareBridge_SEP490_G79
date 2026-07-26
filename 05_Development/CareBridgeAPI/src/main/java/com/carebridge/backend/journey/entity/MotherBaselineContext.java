package com.carebridge.backend.journey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
@SQLRestriction("event_category = 'BASELINE_CONTEXT'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotherBaselineContext {

    private static final String LEGACY_SCHEMA_VERSION = "MOTHER_BASELINE_V1";

    @Id
    @Column(name = "audit_event_id", nullable = false)
    private UUID id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID ownerUserId;

    @Transient private UUID submissionId;

    @Transient private long revision;
    @Transient private String schemaVersion;

    @Transient private String source;

    @Transient private LifecycleGoal lifecycleGoal;
    @Transient private String locale;
    @Transient private String timeZone;
    @Transient private String preferences;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "subject_reference_id")
    private UUID journeyId;

    @Builder.Default
    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType = "mother_journeys";

    @Column(name = "resource_id")
    private UUID resourceId;

    @Transient private Instant effectiveAt;

    @Builder.Default
    @Column(name = "event_category", nullable = false, updatable = false, length = 80)
    private String eventCategory = "BASELINE_CONTEXT";

    @Builder.Default
    @Transient
    private String eventOrigin = "BASELINE_CONTEXT";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @PrePersist
    void prepareCanonicalEvent() {
        if (id == null) id = UUID.randomUUID();
        if (recordedAt == null) recordedAt = effectiveAt == null ? Instant.now() : effectiveAt;
        effectiveAt = recordedAt;
        resourceId = journeyId;
        Map<String, Object> value = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        value.put("revision", revision);
        value.put("schemaVersion", schemaVersion);
        value.put("lifecycleGoal", lifecycleGoal == null ? null : lifecycleGoal.name());
        value.put("locale", locale);
        value.put("timeZone", timeZone);
        value.put("preferences", preferences);
        value.put("source", source);
        value.put("submissionId", submissionId);
        payload = value;
    }

    @PostLoad
    void hydrateCanonicalEvent() {
        effectiveAt = recordedAt;
        if (payload == null) return;
        Object version = payload.get("revision");
        revision = version instanceof Number n ? n.longValue()
                : version == null ? 0 : Long.parseLong(version.toString());
        schemaVersion = text("schemaVersion");
        if (schemaVersion == null || schemaVersion.isBlank()) {
            schemaVersion = LEGACY_SCHEMA_VERSION;
        }
        lifecycleGoal = payload.get("lifecycleGoal") == null ? null
                : LifecycleGoal.valueOf(payload.get("lifecycleGoal").toString());
        locale = text("locale");
        timeZone = text("timeZone");
        preferences = text("preferences");
        source = text("source");
        submissionId = payload.get("submissionId") == null ? null
                : UUID.fromString(payload.get("submissionId").toString());
    }

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Mother baseline context is append-only");
    }

    private String text(String key) {
        return payload.get(key) == null ? null : payload.get(key).toString();
    }
}
