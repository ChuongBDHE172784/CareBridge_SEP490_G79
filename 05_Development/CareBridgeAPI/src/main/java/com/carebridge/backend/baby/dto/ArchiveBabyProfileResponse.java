package com.carebridge.backend.baby.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ArchiveBabyProfileResponse {

    private UUID babyId;
    private String status;
    private Instant archivedAt;
}
