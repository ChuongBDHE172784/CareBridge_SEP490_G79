package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_check_id")
    private UUID safetyCheckId;

    @Column(name = "exercise_id")
    private UUID exerciseId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "answer_json", columnDefinition = "jsonb")
    private String answerJson;

    @Column(name = "red_flag_detected")
    private Boolean redFlagDetected;

    @Column(name = "result_status", length = 20)
    private String resultStatus;

    @Column(name = "blocked_reason", columnDefinition = "TEXT")
    private String blockedReason;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
