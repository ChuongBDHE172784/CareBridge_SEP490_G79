package com.carebridge.backend.checklist.today.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;
import java.util.UUID;

/** Explicit checklist response kept separate from the retired mixed Today DTO. */
public record CurrentChecklistResponse(
        Instant asOf,
        String zoneId,
        int horizonDays,
        CurrentChecklistSections sections,
        TodayTaskCounts counts,
        UUID correlationId,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonUnwrapped TodaySequenceProjection sequence) {
}
