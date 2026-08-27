package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ArchiveHealthRecordResponse {

    private UUID healthRecordId;
    private String status;
    private Instant updatedAt;
}
