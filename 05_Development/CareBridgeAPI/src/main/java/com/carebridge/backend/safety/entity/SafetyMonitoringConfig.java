package com.carebridge.backend.safety.entity;

import com.carebridge.backend.safety.SensitivityLevel;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Safety configuration as the domain works with it.
 *
 * <p>No longer an @Entity: the data lives in typed columns on {@code users}
 * (V3 §3.9) and is projected here by {@link com.carebridge.backend.safety.repository.SafetyConfigStore}.
 * Kept as a class rather than inlined so the DTOs, mapper and SafetyConfigChanged
 * event keep their existing shape.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyMonitoringConfig {

    private UUID id;

    private UUID userId;

    private boolean fallDetectionEnabled;

    private SensitivityLevel sensitivityLevel;

    private boolean emergencyAutoAlert;

    @Builder.Default    private int countdownSeconds = 30;

    private boolean sensorPermissionGranted;

    private Instant sensorPermissionRecordedAt;

    private Instant updatedAt;

    private UUID updatedBy;
}
