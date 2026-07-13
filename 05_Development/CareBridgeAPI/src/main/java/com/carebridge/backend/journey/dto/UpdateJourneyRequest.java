package com.carebridge.backend.journey.dto;

import com.carebridge.backend.journey.entity.JourneyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateJourneyRequest {

    private JourneyType journeyType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastMenstrualDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate estimatedDueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDate;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    /** User-settable statuses: ACTIVE, COMPLETED only. ARCHIVED is system-only. */
    private String status;
}
