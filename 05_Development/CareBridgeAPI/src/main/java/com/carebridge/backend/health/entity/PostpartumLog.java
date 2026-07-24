package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "maternal_observations")
@org.hibernate.annotations.SQLRestriction("legacy_source = 'POSTPARTUM_LOG'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostpartumLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "observation_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mother_journey_id", nullable = false)
    private UUID journeyId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "observation_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "numeric_value")
    private Short painLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 30)
    private BleedingLevel bleedingLevel;

    @Column(name = "mood_level")
    private Short moodLevel;

    @Column(name = "secondary_numeric_value")
    private BigDecimal sleepHours;

    @Column(name = "breastfeeding_note", columnDefinition = "text")
    private String breastfeedingNote;

    @Column(name = "text_value", columnDefinition = "text")
    private String symptomNote;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 20)
    private PostpartumLogStatus status = PostpartumLogStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "observation_type", nullable = false, updatable = false, length = 60)
    private String observationType = "POSTPARTUM_LOG";

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Builder.Default
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private String payloadJson = "{}";

    @Builder.Default
    @Column(name = "schema_version", nullable = false, updatable = false, length = 30)
    private String schemaVersion = "1";

    @Builder.Default
    @Column(name = "source_type", nullable = false, updatable = false, length = 60)
    private String sourceType = "POSTPARTUM_LOG";

    @Builder.Default
    @Column(name = "legacy_source", nullable = false, updatable = false, length = 60)
    private String legacySource = "POSTPARTUM_LOG";

    @Column(name = "legacy_id", nullable = false, updatable = false, length = 100)
    private String legacyId;

    @PrePersist
    @PreUpdate
    void prepareCanonicalObservation() {
        legacyId = id.toString();
        observedAt = logDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    }
}
