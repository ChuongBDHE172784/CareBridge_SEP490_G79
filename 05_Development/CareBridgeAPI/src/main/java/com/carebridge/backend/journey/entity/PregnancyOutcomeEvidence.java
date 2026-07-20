package com.carebridge.backend.journey.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "pregnancy_outcome_evidence",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pregnancy_outcome_submission",
                columnNames = {"journey_id", "submission_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PregnancyOutcomeEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "evidence_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "journey_id", nullable = false, updatable = false)
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
    @Column(name = "source", nullable = false, updatable = false, length = 30)
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

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Pregnancy outcome evidence is append-only");
    }
}
