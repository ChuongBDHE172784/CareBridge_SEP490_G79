package com.carebridge.backend.content.dto.response;

import java.time.Instant;
import java.util.List;

// Aggregate-only response — no entity fields, no row-level PII (ADR-004)
public record CommunityDashboardResponse(
        UserMetrics userMetrics,
        ContentCountMetrics questionMetrics,
        ContentCountMetrics answerMetrics,
        ReportMetrics reportMetrics,
        List<TrendingTopic> trendingTopics,
        Instant generatedAt) {
}
