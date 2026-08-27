package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CreateCareGroupResponse {

    private UUID id;
    private String groupName;
    private String description;
    private String status;
    private int memberCount;
    private Instant createdAt;
}
