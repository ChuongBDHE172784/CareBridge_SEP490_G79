package com.carebridge.backend.content.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReassignContentRequest(
        @NotNull(message = "expertId is required")
        UUID expertId,
        String reason
) {}
