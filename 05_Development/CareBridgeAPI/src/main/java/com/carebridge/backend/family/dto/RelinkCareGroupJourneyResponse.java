package com.carebridge.backend.family.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RelinkCareGroupJourneyResponse {
    private final UUID groupId;
    private final UUID previousJourneyId;
    private final UUID journeyId;
    private final Instant relinkedAt;
    private final UUID correlationId;
}
