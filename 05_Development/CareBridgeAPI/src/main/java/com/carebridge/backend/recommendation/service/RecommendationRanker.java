package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.content.entity.ContentItem;
import java.util.Comparator;

/** One comparator for both service fixtures and repository results. */
@org.springframework.stereotype.Component
public class RecommendationRanker {
    public Comparator<Candidate> comparator() {
        return Comparator
                .comparingInt((Candidate value) -> value.priority()).reversed()
                .thenComparing(Comparator.comparingInt(Candidate::matchedCount).reversed())
                .thenComparingInt(value -> value.stageWide() ? 1 : 0)
                .thenComparingInt(Candidate::windowWidth)
                .thenComparing(value -> value.item().getPublishedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(value -> value.item().getId().toString().toLowerCase(java.util.Locale.ROOT));
    }

    public record Candidate(ContentItem item, int matchedCount, boolean stageWide, int windowWidth, int priority) {}
}
