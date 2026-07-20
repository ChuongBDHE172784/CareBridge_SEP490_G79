package com.carebridge.backend.journey.dto;

import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.JourneyDateConfidence;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class CreateJourneyRequest {

    @NotNull
    private JourneyType journeyType;

    @NotNull
    private LocalDate startDate;

    private LocalDate lastMenstrualDate;

    private LocalDate estimatedDueDate;

    private JourneyDateSource dateSource;

    private JourneyDateConfidence dateConfidence;

    @Size(max = 500)
    private String changeReason;

    private Instant effectiveAt;

    private String notes;
}
