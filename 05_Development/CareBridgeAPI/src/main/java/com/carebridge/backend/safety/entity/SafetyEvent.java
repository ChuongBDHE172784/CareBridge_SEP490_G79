package com.carebridge.backend.safety.entity;

import com.carebridge.backend.safety.SafetyEventType;
import com.carebridge.backend.safety.SafetyEventStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "imu_safety_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "imu_session_id", nullable = false)
    private UUID imuSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private SafetyEventType eventType;

    @Column(name = "magnitude", nullable = false, precision = 10, scale = 4)
    private BigDecimal magnitude;

    @Column(name = "user_latitude", precision = 10, scale = 7)
    private BigDecimal userLatitude;

    @Column(name = "user_longitude", precision = 10, scale = 7)
    private BigDecimal userLongitude;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "client_detected_at")
    private Instant clientDetectedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SafetyEventStatus status = SafetyEventStatus.OPEN;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "notes")
    private String notes;

    @Column(name = "signal_key", length = 200)
    private String signalKey;

    @Column(name = "countdown_deadline_at")
    private Instant countdownDeadlineAt;

    @Column(name = "response_type", length = 30)
    private String responseType;

    @Column(name = "response_reason", length = 500)
    private String responseReason;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "escalation_started_at")
    private Instant escalationStartedAt;

    @Column(name = "emergency_session_id")
    private UUID emergencySessionId;

    @Column(name = "created_by", nullable = false)
    private String createdBy;
}
