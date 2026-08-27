package com.carebridge.backend.carejourney.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class BabyLogSummaryResponse {

    private UUID babyId;
    private String period;
    private Instant fromDate;
    private Instant toDate;
    private Map<String, LogTypeSummary> summaries;
    private String aiInsight;
}
