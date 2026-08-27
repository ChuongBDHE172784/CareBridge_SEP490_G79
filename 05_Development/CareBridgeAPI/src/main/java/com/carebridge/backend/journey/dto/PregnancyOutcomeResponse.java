package com.carebridge.backend.journey.dto;

import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PregnancyOutcomeResponse {

    private UUID evidenceId;
    private UUID journeyId;
    private PregnancyOutcomeType outcomeType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate outcomeDate;

    private JourneyType journeyType;
    private long journeyVersion;
    private UUID transitionId;
    private int revisionNumber;
    private Instant effectiveAt;
    private Instant recordedAt;
}
