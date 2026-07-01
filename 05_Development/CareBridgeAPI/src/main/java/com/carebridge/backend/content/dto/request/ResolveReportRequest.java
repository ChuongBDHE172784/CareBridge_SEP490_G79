package com.carebridge.backend.content.dto.request;

import jakarta.validation.constraints.NotNull;

public record ResolveReportRequest(
        @NotNull ResolutionOutcome outcome,
        String reason
) {}
