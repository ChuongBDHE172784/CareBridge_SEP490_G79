package com.carebridge.backend.exercise.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyCheckResponse {

    private UUID safetyCheckId;
    private UUID exerciseId;
    private String resultStatus;
    private Boolean redFlagDetected;
    private String blockedReason;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
}
