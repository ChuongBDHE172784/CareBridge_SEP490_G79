package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SharedDataItemDto {
    private UUID itemId;
    private String itemType;
    private String title;
    private String summary;
    private Instant occurredAt;
    private String status;
}
