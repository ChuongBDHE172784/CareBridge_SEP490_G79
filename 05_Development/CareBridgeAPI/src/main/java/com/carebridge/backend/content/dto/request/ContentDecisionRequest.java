package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ContentDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContentDecisionRequest(
        @NotNull ContentDecision decision,
        @Size(max = 2000) String reason
) {}
