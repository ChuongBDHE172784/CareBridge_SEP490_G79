package com.carebridge.backend.journey.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JourneyTimelineItemResponse {
    private String itemType;
    private UUID itemId;
    private Instant occurredAt;
    private Instant recordedAt;
    private String eventType;
    private String fromStage;
    private String toStage;
    private String riskLevel;
    private String stage;
    private UUID sourceIntakeId;
    private UUID sourceEmergencyId;
    private String originAction;
}
