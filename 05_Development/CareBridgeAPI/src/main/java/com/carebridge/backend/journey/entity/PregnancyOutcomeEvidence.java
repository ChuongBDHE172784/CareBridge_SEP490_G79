package com.carebridge.backend.journey.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "mother_journey_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pregnancy_outcome_submission",
                columnNames = {"mother_journey_id", "submission_id", "legacy_source"}))
@org.hibernate.annotations.SQLRestriction("legacy_source = 'PREGNANCY_OUTCOME'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PregnancyOutcomeEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mother_journey_id", nullable = false, updatable = false)
    private UUID journeyId;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome_type", nullable = false, updatable = false, length = 30)
    private PregnancyOutcomeType outcomeType;

    @Column(name = "outcome_date", updatable = false)
    private LocalDate outcomeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_source", nullable = false, updatable = false, length = 30)
    private JourneyDateSource source;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "reason", nullable = false, updatable = false, length = 500)
    private String reason;

    @Column(name = "effective_at", nullable = false, updatable = false)
    private Instant effectiveAt;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "revision_number", nullable = false, updatable = false)
    private int revisionNumber;

    @Column(name = "supersedes_evidence_id", updatable = false)
    private UUID supersedesEvidenceId;

    @Column(name = "journey_version", nullable = false, updatable = false)
    private long journeyVersion;

    @Column(name = "semantic_hash", nullable = false, updatable = false, length = 500)
    private String semanticHash;

    @Column(name = "correction", nullable = false, updatable = false)
    private boolean correction;

    @Builder.Default
    @Column(name = "event_type", nullable = false, updatable = false, length = 60)
    private String eventType = "PREGNANCY_OUTCOME_EVIDENCE";

    @Builder.Default
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "event_payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private String payloadJson = "{}";

    @Builder.Default
    @Column(name = "schema_version", nullable = false, updatable = false, length = 30)
    private String schemaVersion = "1";

    @Builder.Default
    @Column(name = "legacy_source", nullable = false, updatable = false, length = 60)
    private String legacySource = "PREGNANCY_OUTCOME";

    @Column(name = "legacy_id", nullable = false, updatable = false, length = 100)
    private String legacyId;

    @PrePersist
    void prepareCanonicalEvent() {
        legacyId = id.toString();
    }

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Pregnancy outcome evidence is append-only");
    }
}
