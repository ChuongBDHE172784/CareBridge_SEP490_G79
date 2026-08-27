package com.carebridge.backend.family.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTaskDetailResponse {
    private UUID careTaskId;
    private UUID careGroupId;
    private String title;
    private String description;
    private Instant dueAt;
    private String status;
    private UUID assignedTo;
    private String assignedToName;
    private UUID assignedBy;
    private String assignedByName;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
