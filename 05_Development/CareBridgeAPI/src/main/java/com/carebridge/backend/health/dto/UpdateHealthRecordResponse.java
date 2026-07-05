package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UpdateHealthRecordResponse {

    private UUID healthRecordId;
    private String title;
    private String recordType;
    private LocalDate recordDate;
    private String sourceType;
    private String sourceName;
    private String fileUrl;
    private String status;
    private Instant updatedAt;
}
