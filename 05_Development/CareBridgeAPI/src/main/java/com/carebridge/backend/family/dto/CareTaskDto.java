package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CareTaskDto {
    private UUID careTaskId;
    private String title;
    private String description;
    private Instant dueAt;
    private String status;
    private UUID assignedTo;
    private UUID assignedBy;
    private Instant completedAt;
}
