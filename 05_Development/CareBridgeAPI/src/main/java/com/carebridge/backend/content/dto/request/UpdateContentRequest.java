package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.util.List;

public record UpdateContentRequest(
        @NotBlank @Size(max = 500) String title,
        @Size(max = 50000) String body,
        @Size(max = 150) String summary,
        @NotNull ContentStage stage,
        UUID topicId,
        List<UUID> tagIds,
        @NotNull ContentStatus status,
        String sourceLabel,
        List<@jakarta.validation.Valid ContentSourceRequest> sources
) {
    /** Backward-compatible constructor for existing API callers that do not yet send tags. */
    public UpdateContentRequest(
            String title, String body, ContentStage stage, UUID topicId, ContentStatus status,
            String sourceLabel, List<ContentSourceRequest> sources) {
        this(title, body, null, stage, topicId, null, status, sourceLabel, sources);
    }
}
