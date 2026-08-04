package com.carebridge.backend.recommendation.dto;

import com.carebridge.backend.recommendation.entity.RecommendationProfileStatus;
import java.time.Instant;
import java.util.Map;

public record RecommendationProfileResponse(
        RecommendationProfileStatus status,
        boolean requiresAction,
        boolean profileComplete,
        int schemaVersion,
        int profileRevision,
        Instant completedAt,
        ConsentSummary consent,
        Map<String, Object> profile,
        Map<String, Object> derived) {

    public record ConsentSummary(
            String state,
            Long consentGrantId,
            String policyVersion,
            Instant grantedAt,
            Instant expiresAt) {}
}
