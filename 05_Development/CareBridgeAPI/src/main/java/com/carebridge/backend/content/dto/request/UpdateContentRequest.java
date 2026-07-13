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
        @NotNull ContentStage stage,
        UUID topicId,
        @NotNull ContentStatus status,
        String sourceLabel,
        List<@jakarta.validation.Valid ContentSourceRequest> sources
) {}
