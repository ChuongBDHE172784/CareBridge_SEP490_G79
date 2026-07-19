package com.carebridge.backend.journey.dto;

import com.carebridge.backend.journey.entity.JourneyDateConfidence;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.JourneyTransitionType;
import com.carebridge.backend.journey.entity.JourneyType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class JourneyTransitionResponse {
    UUID transitionId;
    JourneyTransitionType eventType;
    JourneyType fromStage;
    JourneyType toStage;
    List<String> changedFields;
    JourneyDateSource source;
    JourneyDateConfidence confidence;
    String reason;
    Instant effectiveAt;
    Instant recordedAt;
    long journeyVersion;
}
