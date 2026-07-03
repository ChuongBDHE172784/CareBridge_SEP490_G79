package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PendingContentQueueFilter(
        ReportTargetType targetType,
        @Min(0) int page,
        @Min(1) @Max(50) int size
) {
    public PendingContentQueueFilter {
        if (size == 0) {
            size = 20;
        }
    }
}
