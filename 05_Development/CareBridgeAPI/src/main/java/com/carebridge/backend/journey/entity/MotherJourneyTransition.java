package com.carebridge.backend.journey.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "mother_journey_transitions")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MotherJourneyTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transition_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "journey_id", nullable = false)
    private UUID journeyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private JourneyTransitionType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage", length = 20)
    private JourneyType fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", length = 20)
    private JourneyType toStage;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> changes = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private JourneyDateSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", length = 20)
    private JourneyDateConfidence confidence;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "journey_version", nullable = false)
    private long journeyVersion;

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Mother journey transitions are append-only");
    }
}
