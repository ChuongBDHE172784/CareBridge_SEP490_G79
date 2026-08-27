package com.carebridge.backend.aimoderation.dto.request;

import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AiRescanRequest(
        @NotNull ReportTargetType targetType,
        @NotNull UUID targetId
) {
}
