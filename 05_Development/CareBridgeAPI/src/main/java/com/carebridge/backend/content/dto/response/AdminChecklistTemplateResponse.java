package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record AdminChecklistTemplateResponse(
        UUID id,
        String name,
        @Nullable @Schema(nullable = true) ContentStage stage,
        ChecklistTemplateStatus status,
        String description,
        Integer versionNo,
        @Nullable @Schema(nullable = true) Instant updatedAt,
        long itemCount) {
}
