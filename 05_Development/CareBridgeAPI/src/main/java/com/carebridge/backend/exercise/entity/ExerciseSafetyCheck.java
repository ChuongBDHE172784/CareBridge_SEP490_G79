package com.carebridge.backend.exercise.entity;

import com.carebridge.backend.exercise.entity.converter.JsonbMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exercise_safety_checks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSafetyCheck {

    @Id
    @Column(name = "safety_check_id", nullable = false)
    private UUID safetyCheckId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Convert(converter = JsonbMapConverter.class)
    @Column(name = "answer_json", columnDefinition = "jsonb")
    private Map<String, Boolean> answerJson;

    @Column(name = "red_flag_detected", nullable = false)
    private Boolean redFlagDetected;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    private SafetyCheckStatus resultStatus;

    @Column(name = "blocked_reason", columnDefinition = "text")
    private String blockedReason;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
