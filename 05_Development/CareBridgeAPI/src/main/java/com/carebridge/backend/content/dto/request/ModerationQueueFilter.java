package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ModerationQueueFilter(
        ReportTargetType targetType,
        ReportStatus status,
        @Min(0) int page,
        @Min(1) @Max(50) int size
) {
    public ModerationQueueFilter {
        if (status == null) {
            status = ReportStatus.PENDING;
        }
        if (size == 0) {
            size = 20;
        }
    }
}
