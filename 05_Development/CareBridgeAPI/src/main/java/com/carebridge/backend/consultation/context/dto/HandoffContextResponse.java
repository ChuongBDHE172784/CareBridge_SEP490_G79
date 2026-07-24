package com.carebridge.backend.consultation.context.dto;

import java.util.List;

public record HandoffContextResponse(
        String riskLevel,
        String stage,
        String riskSummary,
        List<SharedCitationResponse> citations) {
}
