package com.carebridge.backend.journey.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
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
