package com.carebridge.backend.content.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ResolveReportRequest(
        @NotNull ResolutionOutcome outcome,
        String reason,
        Instant expiresAt
) {
    public ResolveReportRequest(ResolutionOutcome outcome, String reason) {
        this(outcome, reason, null);
    }
}
