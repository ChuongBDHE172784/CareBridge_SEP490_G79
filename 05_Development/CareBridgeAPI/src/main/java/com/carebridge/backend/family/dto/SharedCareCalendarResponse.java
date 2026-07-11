package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SharedCareCalendarResponse {
    private UUID groupId;
    private String groupName;
    private Instant rangeStart;
    private Instant rangeEnd;
    private Integer totalItems;
    private List<CalendarItemDto> items;
}
