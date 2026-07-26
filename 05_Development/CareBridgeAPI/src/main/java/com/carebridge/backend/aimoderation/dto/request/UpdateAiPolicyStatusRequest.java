package com.carebridge.backend.aimoderation.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateAiPolicyStatusRequest(@NotNull Boolean active) {
}
