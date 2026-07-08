package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "posture_feedback_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureFeedbackEvent {

    @Id
    @Column(name = "feedback_event_id", nullable = false)
    private UUID feedbackEventId;

    @Column(name = "exercise_session_id", nullable = false)
    private UUID exerciseSessionId;

    @Column(name = "posture_config_id")
    private UUID postureConfigId;

    @Column(name = "event_time_ms", nullable = false)
    private Long eventTimeMs;

    @Column(name = "posture_code", length = 80)
    private String postureCode;

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "feedback_text", columnDefinition = "text")
    private String feedbackText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keypoint_summary_json", columnDefinition = "jsonb")
    private String keypointSummaryJson;
}
