package com.carebridge.backend.family.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UpdateFamilyTaskRequest {
    private String title;
    private String description;
    private Instant dueAt;
    private UUID assigneeMemberId;
}
