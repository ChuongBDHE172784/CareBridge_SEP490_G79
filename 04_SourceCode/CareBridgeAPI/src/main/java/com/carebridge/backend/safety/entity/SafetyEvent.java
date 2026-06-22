package com.carebridge.backend.safety.entity;

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
@Table(name = "safety_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_event_id")
    private UUID safetyEventId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "setting_id")
    private UUID settingId;

    @Column(name = "detected_at")
    private Instant detectedAt;

    @Column(name = "event_type", length = 30)
    private String eventType;

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;

    @Column(name = "peak_acceleration")
    private BigDecimal peakAcceleration;

    @Column(name = "angular_velocity")
    private BigDecimal angularVelocity;

    @Column(name = "inactivity_seconds")
    private Integer inactivitySeconds;

    @Column(name = "user_response", length = 30)
    private String userResponse;

    @Column(name = "response_at")
    private Instant responseAt;

    @Column(name = "false_positive_reason", length = 80)
    private String falsePositiveReason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
