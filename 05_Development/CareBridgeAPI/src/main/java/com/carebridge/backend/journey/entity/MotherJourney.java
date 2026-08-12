package com.carebridge.backend.journey.entity;

import com.carebridge.backend.recommendation.entity.RecommendationProfileStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "mother_journeys")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MotherJourney {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "journey_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "care_subject_id", nullable = false)
    private UUID careSubjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "journey_type", nullable = false, length = 20)
    private JourneyType journeyType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "last_menstrual_date")
    private LocalDate lastMenstrualDate;

    @Column(name = "estimated_due_date")
    private LocalDate estimatedDueDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "pregnancy_outcome", length = 30)
    private PregnancyOutcomeType pregnancyOutcome;

    @Column(name = "pregnancy_outcome_date")
    private LocalDate pregnancyOutcomeDate;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JourneyStatus status = JourneyStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_source", length = 30)
    private JourneyDateSource dateSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_confidence", length = 20)
    private JourneyDateConfidence dateConfidence;

    /** Server-owned pregnancy dating basis; unrelated Journey edits use version. */
    @Enumerated(EnumType.STRING)
    @Column(name = "gestational_dating_basis", length = 20)
    private GestationalDatingBasis gestationalDatingBasis;

    /** Monotonic pregnancy dating revision reconstructed from immutable transitions. */
    @Column(name = "gestational_dating_revision")
    private Long gestationalDatingRevision;

    @Column(name = "gestational_dating_effective_at")
    private Instant gestationalDatingEffectiveAt;

    @Column(name = "gestational_dating_quarantine_reason_code", length = 80)
    private String gestationalDatingQuarantineReasonCode;

    /** Existing onboarding snapshot (comma-separated SupportPreference names). */
    @Column(name = "baseline_preferences", length = 300)
    private String baselinePreferences;

    /**
     * Timezone captured during onboarding.  Checklist repair runs without a request
     * header, so this persisted value is the only per-owner calendar authority available
     * to an app-independent scheduler.  The repair job validates it and falls back to its
     * configured business zone when it is absent or stale.
     */
    @Column(name = "baseline_time_zone", length = 80)
    private String baselineTimeZone;

    /** Server-authored consented profile. Raw values are cleared when inactive. */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendation_profile_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> recommendationProfileJson = new LinkedHashMap<>();

    @Builder.Default
    @Column(name = "recommendation_profile_version", nullable = false)
    private short recommendationProfileVersion = 0;

    @Column(name = "recommendation_profile_completed_at")
    private Instant recommendationProfileCompletedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_profile_status", nullable = false, length = 24)
    private RecommendationProfileStatus recommendationProfileStatus = RecommendationProfileStatus.NOT_STARTED;

    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    private long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
