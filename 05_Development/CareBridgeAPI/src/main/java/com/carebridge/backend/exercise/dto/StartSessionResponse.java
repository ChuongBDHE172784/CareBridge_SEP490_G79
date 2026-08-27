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
public class StartSessionResponse {

    private UUID exerciseSessionId;
    private UUID exerciseId;
    private UUID userId;
    private UUID safetyCheckId;
    private UUID journeyId;
    private String sessionStatus;
    private OffsetDateTime startedAt;
    private Boolean supportsPostureAnalysis;
}
