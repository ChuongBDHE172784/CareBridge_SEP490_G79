package com.carebridge.backend.safety.entity;

import com.carebridge.backend.safety.SensitivityLevel;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyMonitoringConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_config_id")
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

    @Column(name = "countdown_seconds", nullable = false)
    @Builder.Default
    private int countdownSeconds = 30;

    @Column(name = "sensor_permission_granted", nullable = false)
    private boolean sensorPermissionGranted;

    @Column(name = "sensor_permission_recorded_at")
    private Instant sensorPermissionRecordedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
