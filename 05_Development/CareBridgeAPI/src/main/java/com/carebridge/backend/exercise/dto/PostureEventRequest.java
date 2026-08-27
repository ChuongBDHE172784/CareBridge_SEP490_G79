package com.carebridge.backend.exercise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostureEventRequest {

    @NotNull
    @PositiveOrZero
    private Long eventTimeMs;

    @NotNull
    private Map<String, Object> keypointSummaryJson;
}
