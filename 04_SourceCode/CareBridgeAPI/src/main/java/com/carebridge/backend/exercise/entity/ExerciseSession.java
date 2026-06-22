package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exercise_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "exercise_session_id")
    private UUID exerciseSessionId;

    @Column(name = "exercise_id")
    private UUID exerciseId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "safety_check_id")
    private UUID safetyCheckId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "paused_seconds")
    private Integer pausedSeconds;

    @Column(name = "completion_percent")
    private BigDecimal completionPercent;

    @Column(name = "posture_score")
    private BigDecimal postureScore;

    @Column(name = "session_status", length = 20)
    private String sessionStatus;

    @Column(name = "warning_count")
    private Integer warningCount;

    @Column(name = "summary_json", columnDefinition = "jsonb")
    private String summaryJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
