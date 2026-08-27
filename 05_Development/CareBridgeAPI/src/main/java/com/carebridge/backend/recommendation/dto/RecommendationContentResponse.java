package com.carebridge.backend.recommendation.dto;

import com.carebridge.backend.recommendation.entity.RecommendationProfileStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.carebridge.backend.recommendation.dto.RecommendationEnums.*;

public record RecommendationContentResponse(
        String stage,
        Integer pregnancyWeek,
        WeekEligibilityMode weekEligibilityMode,
        RecommendationProfileStatus profileStatus,
        SelectionMode selectionMode,
        CoverageStatus coverageStatus,
        boolean fallbackUsed,
        List<Item> items) {

    public record Item(
            int rank,
            SelectionType selectionType,
            ReasonCode reasonCode,
            String reasonLabel,
            ContentSummary content) {}

    public record ContentSummary(
            UUID id,
            String type,
            String title,
            String summary,
            String stage,
            UUID topicId,
            Instant publishedAt) {}
}
