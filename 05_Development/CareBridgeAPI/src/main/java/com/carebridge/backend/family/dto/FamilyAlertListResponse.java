package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FamilyAlertListResponse {
    private int page;
    private int size;
    private long totalItems;
    private List<FamilyAlertItemDto> alerts;
}
