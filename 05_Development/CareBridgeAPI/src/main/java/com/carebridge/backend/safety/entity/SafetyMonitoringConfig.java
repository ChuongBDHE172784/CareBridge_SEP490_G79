package com.carebridge.backend.safety.entity;

import com.carebridge.backend.safety.SensitivityLevel;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_monitoring_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyMonitoringConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "fall_detection_enabled", nullable = false)
    private boolean fallDetectionEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensitivity_level", nullable = false, length = 10)
    private SensitivityLevel sensitivityLevel;

    @Column(name = "emergency_auto_alert", nullable = false)
    private boolean emergencyAutoAlert;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
