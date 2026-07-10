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
public class LeaveCareGroupResponse {
    private UUID groupId;
    private Instant leftAt;
    private int reassignedTaskCount;
}
