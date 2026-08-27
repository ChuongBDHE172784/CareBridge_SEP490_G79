package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AddHealthRecordResponse {

    private UUID id;
    private String recordType;
    private String title;
    private LocalDate recordDate;
    private String facilityName;
    private String status;
    private List<UUID> fileIds;
    private Instant createdAt;
}
