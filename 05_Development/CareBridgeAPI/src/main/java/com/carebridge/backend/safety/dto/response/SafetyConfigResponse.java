package com.carebridge.backend.safety.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyConfigResponse {
    private UUID id;
    private UUID userId;
    private boolean fallDetectionEnabled;
    private String sensitivityLevel;
    private boolean emergencyAutoAlert;
    private boolean locationSharingEnabled;
    private int countdownSeconds;
    private boolean sensorPermissionGranted;
    private Instant sensorPermissionRecordedAt;
    private Instant updatedAt;
}
