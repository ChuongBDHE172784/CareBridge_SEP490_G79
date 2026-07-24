package com.carebridge.backend.consultation.context.dto;

import java.util.List;
import java.util.UUID;

public record HandoffPreviewResponse(
        UUID intakeSessionId,
        String consentPolicyVersion,
        String riskLevel,
        String stage,
        String riskSummary,
        List<SharedCitationResponse> citations,
        List<String> sharedFields,
        List<String> excludedFields) {
}
