package com.carebridge.backend.identity.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewAccountLockAppealRequest(
        @NotNull Decision decision,
        @Size(max = 1000) String reviewNote) {
    public enum Decision {
        APPROVE,
        REJECT
    }
}

