package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** UC-14 Report Content or Account (CB-MOD-IMP-014 §8.1). */
@Getter
@Setter
public class CreateReportRequest {

    @NotNull(message = "targetType is required")
    private ReportTargetType targetType;

    @NotNull(message = "targetId is required")
    private UUID targetId;

    @NotNull(message = "category is required")
    private ReportCategory category;

    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;
}
