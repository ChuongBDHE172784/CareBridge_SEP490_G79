package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "health_observations")
@org.hibernate.annotations.SQLRestriction("legacy_source = 'postpartum_logs'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostpartumLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "health_observation_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "care_subject_id", nullable = false)
    private UUID journeyId;

    @Transient
    private UUID submissionId;

    @Transient
    private LocalDate logDate;

    @Column(name = "value_numeric")
    private Short painLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 30)
    private BleedingLevel bleedingLevel;

    @Transient
    private Short moodLevel;

    @Column(name = "value_secondary")
    private BigDecimal sleepHours;

    @Transient
    private String breastfeedingNote;

    @Column(name = "text_value", columnDefinition = "text")
    private String symptomNote;

    @Builder.Default
    @Transient
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
    @Column(name = "raw_payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payloadJson = new LinkedHashMap<>();

    @Builder.Default
    @Column(name = "source_type", nullable = false, updatable = false, length = 60)
    private String sourceType = "POSTPARTUM_LOG";

    @Builder.Default
    @Column(name = "legacy_source", nullable = false, updatable = false, length = 60)
    private String legacySource = "postpartum_logs";

    @Column(name = "legacy_id", nullable = false, updatable = false, length = 100)
    private String legacyId;

    @Builder.Default
    @Column(name = "subject_type", nullable = false, updatable = false, length = 30)
    private String subjectType = "MOTHER";

    @PrePersist
    @PreUpdate
    void prepareCanonicalObservation() {
        if (legacyId == null && id != null) legacyId = id.toString();
        if (logDate != null) observedAt = logDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        if (payloadJson == null) payloadJson = new LinkedHashMap<>();
        putPayload("submissionId", submissionId);
        putPayload("moodLevel", moodLevel);
        putPayload("breastfeedingNote", breastfeedingNote);
        putPayload("recordStatus", status == null ? null : status.name());
    }

    @PostLoad
    void hydrateCanonicalObservation() {
        if (observedAt != null) logDate = observedAt.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        if (payloadJson == null) return;
        Object submission = payloadJson.get("submissionId");
        submissionId = submission == null || submission.toString().isBlank()
                ? null : UUID.fromString(submission.toString());
        Object mood = payloadJson.get("moodLevel");
        moodLevel = mood instanceof Number number ? number.shortValue()
                : mood == null ? null : Short.valueOf(mood.toString());
        Object breastfeeding = payloadJson.get("breastfeedingNote");
        breastfeedingNote = breastfeeding == null ? null : breastfeeding.toString();
        Object recordStatus = payloadJson.get("recordStatus");
        status = recordStatus == null || recordStatus.toString().isBlank()
                ? PostpartumLogStatus.ACTIVE : PostpartumLogStatus.valueOf(recordStatus.toString());
    }

    private void putPayload(String key, Object value) {
        if (value == null) payloadJson.remove(key); else payloadJson.put(key, value);
    }

}
