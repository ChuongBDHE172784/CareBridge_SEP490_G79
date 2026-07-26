package com.carebridge.backend.journey.service;

import java.util.UUID;

public record ProjectionResult(boolean created, UUID outcomeId) {
    public static ProjectionResult skipped() {
        return new ProjectionResult(false, null);
    }
}
