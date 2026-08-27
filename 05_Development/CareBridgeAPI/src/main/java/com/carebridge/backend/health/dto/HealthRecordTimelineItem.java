package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class HealthRecordTimelineItem {

    private UUID healthRecordId;
    private String recordType;
    private String title;
    private LocalDate recordDate;
    private String sourceType;
    private String sourceName;
    private String fileUrl;
    private UUID journeyId;
    private UUID babyId;
    private Instant createdAt;
}
