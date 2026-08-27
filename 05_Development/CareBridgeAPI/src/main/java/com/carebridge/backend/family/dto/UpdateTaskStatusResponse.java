package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UpdateTaskStatusResponse {
    private UUID careTaskId;
    private UUID careGroupId;
    private String status;
    private Instant completedAt;
    private Instant updatedAt;
}
