package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ContentDecision;
import jakarta.validation.constraints.NotNull;

public record ContentDecisionRequest(
        @NotNull ContentDecision decision,
        String reason
) {}
