package com.carebridge.backend.journey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "mother_journey_events")
@SQLRestriction("legacy_source = 'MOTHER_BASELINE'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotherBaselineContext {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "journey_version", nullable = false)
    private long revision;

    @Column(name = "schema_version", nullable = false, length = 40)
    private String schemaVersion;

    @Column(name = "event_source", nullable = false, length = 30)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_goal", nullable = false, length = 40)
    private LifecycleGoal lifecycleGoal;

    @Column(nullable = false, length = 20)
    private String locale;

    @Column(name = "time_zone", nullable = false, length = 80)
    private String timeZone;

    @Column(nullable = false, length = 300)
    private String preferences;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "mother_journey_id")
    private UUID journeyId;

    @Builder.Default
    @Column(name = "event_type", nullable = false, updatable = false, length = 60)
    private String eventType = "BASELINE_CONTEXT";

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private String payloadJson = "{}";

    @Column(name = "effective_at", nullable = false, updatable = false)
    private Instant effectiveAt;

    @Builder.Default
    @Column(name = "legacy_source", nullable = false, updatable = false, length = 60)
    private String legacySource = "MOTHER_BASELINE";

    @Column(name = "legacy_id", nullable = false, updatable = false, length = 100)
    private String legacyId;

    @PrePersist
    void prepareCanonicalEvent() {
        if (effectiveAt == null) {
            effectiveAt = recordedAt;
        }
        legacyId = id.toString();
    }
}
