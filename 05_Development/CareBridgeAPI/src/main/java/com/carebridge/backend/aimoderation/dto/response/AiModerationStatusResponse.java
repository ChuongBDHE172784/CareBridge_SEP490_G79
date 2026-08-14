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
        /**
         * What the provider reported running on the last successful call. model is
         * usually an alias - gemini-flash-latest is resolved on Google's side - so this
         * is the only place the actual release shows up. Null until a call succeeds.
         */
        String resolvedModel,
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
