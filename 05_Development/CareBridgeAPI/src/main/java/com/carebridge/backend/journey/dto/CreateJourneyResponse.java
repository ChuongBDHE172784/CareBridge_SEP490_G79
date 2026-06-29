package com.carebridge.backend.journey.dto;

import lombok.Builder;
import lombok.Data;

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
    private LocalDate estimatedDueDate;
    private String notes;
    private Instant createdAt;
}
