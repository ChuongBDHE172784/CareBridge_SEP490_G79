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
public class CancelFamilyTaskResponse {
    private UUID careTaskId;
    private UUID careGroupId;
    private UUID assignedTo;
    private UUID assignedBy;
    private String title;
    private String status;
    private Instant updatedAt;
}
