package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SharedDataResponse {
    private UUID groupId;
    private String category;
    private int totalItems;
    private List<SharedDataItemDto> items;
    private Instant asOf;
}
