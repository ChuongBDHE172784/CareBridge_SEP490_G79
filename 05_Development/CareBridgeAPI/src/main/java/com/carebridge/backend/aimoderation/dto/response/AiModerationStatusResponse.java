package com.carebridge.backend.aimoderation.dto.response;

import java.time.Instant;

/**
 * Operational status for the System Admin hub. Deliberately contains configuration state
 * only — the API key itself is never exposed anywhere.
 */
public record AiModerationStatusResponse(
        boolean enabled,
        boolean configured,
        String model,
        String state,
        boolean businessToggleEnabled,
        long queuedJobs,
        long processingJobs,
        long failedJobs,
        Instant lastCompletedAt,
        String policySetHash,
        long activePolicies
) {
}
