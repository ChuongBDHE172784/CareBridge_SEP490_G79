package com.carebridge.backend.journey.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JourneyTransitionPageResponse {

    private List<JourneyTransitionResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
