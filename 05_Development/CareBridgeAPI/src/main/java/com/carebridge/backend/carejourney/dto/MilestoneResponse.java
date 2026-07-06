package com.carebridge.backend.carejourney.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class MilestoneResponse {

    private UUID milestoneId;
    private UUID babyId;
    private String milestoneType;
    private LocalDate achievedDate;
    private String note;
    private String sourceType;
    private UUID recordedBy;
    private Instant createdAt;
}
