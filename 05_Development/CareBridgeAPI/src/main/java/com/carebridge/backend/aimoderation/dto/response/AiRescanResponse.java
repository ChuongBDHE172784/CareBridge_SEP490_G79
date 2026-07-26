package com.carebridge.backend.aimoderation.dto.response;

import java.util.UUID;

/** jobId is null when an identical scan was already queued/processing (collapsed duplicate). */
public record AiRescanResponse(UUID jobId, boolean queued) {
}
