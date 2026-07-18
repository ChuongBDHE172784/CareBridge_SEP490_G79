package com.carebridge.backend.journey.dto;

import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.JourneyDateConfidence;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.Instant;

@Data
public class UpdateJourneyRequest {

    private JourneyType journeyType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastMenstrualDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate estimatedDueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDate;

    private JourneyDateSource dateSource;

    private JourneyDateConfidence dateConfidence;

    @Size(max = 500)
    private String changeReason;

    private Instant effectiveAt;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    /** User-settable statuses: ACTIVE, COMPLETED only. ARCHIVED is system-only. */
    private String status;
}
