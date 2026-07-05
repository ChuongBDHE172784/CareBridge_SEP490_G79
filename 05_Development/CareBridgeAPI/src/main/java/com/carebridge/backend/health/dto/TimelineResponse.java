package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TimelineResponse {

    private List<HealthRecordTimelineItem> items;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
