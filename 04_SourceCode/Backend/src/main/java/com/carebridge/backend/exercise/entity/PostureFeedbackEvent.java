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
@Table(name = "posture_feedback_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureFeedbackEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feedback_event_id")
    private UUID feedbackEventId;

    @Column(name = "exercise_session_id")
    private UUID exerciseSessionId;

    @Column(name = "posture_config_id")
    private UUID postureConfigId;

    @Column(name = "event_time_ms")
    private Long eventTimeMs;

    @Column(name = "posture_code", length = 80)
    private String postureCode;

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "feedback_text", length = 500)
    private String feedbackText;

    @Column(name = "keypoint_summary_json", columnDefinition = "jsonb")
    private String keypointSummaryJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
