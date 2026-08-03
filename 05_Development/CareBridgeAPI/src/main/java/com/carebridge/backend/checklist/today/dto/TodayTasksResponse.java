package com.carebridge.backend.checklist.today.dto;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonInclude;

public record TodayTasksResponse(
        Instant asOf,
        String zoneId,
        int horizonDays,
        TodayTaskSections sections,
        TodayTaskCounts counts,
        UUID correlationId,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonUnwrapped TodaySequenceProjection sequence) {
    public TodayTasksResponse(
            Instant asOf,
            String zoneId,
            int horizonDays,
            TodayTaskSections sections,
            TodayTaskCounts counts,
            UUID correlationId) {
        this(asOf, zoneId, horizonDays, sections, counts, correlationId, null);
    }
}
