package com.carebridge.backend.journey.dto;

import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class RecordPregnancyOutcomeRequest {

    @NotNull
    private UUID submissionId;

    @NotNull
    @PositiveOrZero
    private Long expectedJourneyVersion;

    @NotNull
    private PregnancyOutcomeType outcomeType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate outcomeDate;

    @NotNull
    private JourneyDateSource source;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @NotNull
    private Instant effectiveAt;

    private boolean correction;
}
