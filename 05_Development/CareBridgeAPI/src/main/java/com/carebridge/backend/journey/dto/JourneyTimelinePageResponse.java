package com.carebridge.backend.journey.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JourneyTimelinePageResponse {
    private List<JourneyTimelineItemResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
