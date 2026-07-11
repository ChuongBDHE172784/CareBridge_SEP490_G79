package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CareTasksResponse {
    private UUID groupId;
    private int totalTasks;
    private List<CareTaskDto> tasks;
}
