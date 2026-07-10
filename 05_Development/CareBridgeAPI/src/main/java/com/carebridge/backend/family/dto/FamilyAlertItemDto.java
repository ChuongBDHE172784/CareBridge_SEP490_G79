package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class FamilyAlertItemDto {
    private UUID alertId;
    private String title;
    private String body;
    private boolean isRead;
    private Instant createdAt;
}
