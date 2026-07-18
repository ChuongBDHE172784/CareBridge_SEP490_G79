package com.carebridge.backend.journey.dto;

import lombok.Builder;
import lombok.Data;
import com.carebridge.backend.journey.entity.JourneyDateConfidence;
import com.carebridge.backend.journey.entity.JourneyDateSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateJourneyResponse {

    private UUID id;
    private String journeyType;
    private String status;
    private LocalDate startDate;
    private LocalDate lastMenstrualDate;
    private LocalDate estimatedDueDate;
    private String notes;
    private long version;
    private JourneyDateSource dateSource;
    private JourneyDateConfidence dateConfidence;
    private Instant createdAt;
}
