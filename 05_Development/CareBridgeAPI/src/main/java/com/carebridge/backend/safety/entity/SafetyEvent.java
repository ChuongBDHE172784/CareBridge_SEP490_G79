package com.carebridge.backend.safety.entity;

import com.carebridge.backend.safety.SafetyEventType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_events")
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

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by", nullable = false)
    private String createdBy;
}
