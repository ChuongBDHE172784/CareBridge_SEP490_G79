package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exercise_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSession {

    @Id
    @Column(name = "exercise_session_id", nullable = false)
    private UUID exerciseSessionId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "safety_check_id")
    private UUID safetyCheckId;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "paused_seconds")
    private Integer pausedSeconds;

    @Column(name = "completion_percent")
    private BigDecimal completionPercent;

    @Column(name = "posture_score")
    private BigDecimal postureScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false, length = 20)
    private SessionStatus sessionStatus;

    @Column(name = "warning_count")
    private Integer warningCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_json", columnDefinition = "jsonb")
    private String summaryJson;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
