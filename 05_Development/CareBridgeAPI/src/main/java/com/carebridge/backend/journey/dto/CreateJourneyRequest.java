package com.carebridge.backend.journey.dto;

import com.carebridge.backend.journey.entity.JourneyType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateJourneyRequest {

    @NotNull
    private JourneyType journeyType;

    @NotNull
    private LocalDate startDate;

    private LocalDate lastMenstrualDate;

    private LocalDate estimatedDueDate;

    private String notes;
}
