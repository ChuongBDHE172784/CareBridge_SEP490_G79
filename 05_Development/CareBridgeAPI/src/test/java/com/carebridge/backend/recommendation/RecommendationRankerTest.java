package com.carebridge.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.recommendation.service.RecommendationRanker;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationRankerTest {

    private final RecommendationRanker ranker = new RecommendationRanker();

    @Test
    void priorityPrecedesMatchedSignalCount() {
        ContentItem highPriority = article("00000000-0000-0000-0000-000000000001");
        ContentItem moreSignals = article("00000000-0000-0000-0000-000000000002");

        var ordered = java.util.stream.Stream.of(
                        new RecommendationRanker.Candidate(moreSignals, 3, false, 2, 60),
                        new RecommendationRanker.Candidate(highPriority, 1, false, 2, 61))
                .sorted(ranker.comparator())
                .toList();

        assertThat(ordered.get(0).item()).isSameAs(highPriority);
    }

    @Test
    void boundedWindowPrecedesStageWideWhenOtherKeysTie() {
        ContentItem bounded = article("00000000-0000-0000-0000-000000000001");
        ContentItem stageWide = article("00000000-0000-0000-0000-000000000002");

        var ordered = java.util.stream.Stream.of(
                        new RecommendationRanker.Candidate(stageWide, 1, true, 43, 10),
                        new RecommendationRanker.Candidate(bounded, 1, false, 5, 10))
                .sorted(ranker.comparator())
                .toList();

        assertThat(ordered.get(0).item()).isSameAs(bounded);
    }

    @Test
    void newerPublicationPrecedesOlderAndNullPublicationIsLast() {
        ContentItem newer = article("00000000-0000-0000-0000-000000000001");
        newer.setPublishedAt(Instant.parse("2026-08-02T00:00:00Z"));
        ContentItem older = article("00000000-0000-0000-0000-000000000002");
        older.setPublishedAt(Instant.parse("2026-08-01T00:00:00Z"));
        ContentItem unpublished = article("00000000-0000-0000-0000-000000000003");
        unpublished.setPublishedAt(null);

        var ordered = java.util.stream.Stream.of(
                        new RecommendationRanker.Candidate(unpublished, 1, false, 2, 10),
                        new RecommendationRanker.Candidate(older, 1, false, 2, 10),
                        new RecommendationRanker.Candidate(newer, 1, false, 2, 10))
                .sorted(ranker.comparator())
                .toList();

        assertThat(ordered).extracting(candidate -> candidate.item().getId().toString())
                .containsExactly(newer.getId().toString(), older.getId().toString(), unpublished.getId().toString());
    }

    @Test
    void uuidTieBreakIsCanonicalLowercaseAscending() {
        ContentItem first = article("00000000-0000-0000-0000-000000000001");
        ContentItem second = article("00000000-0000-0000-0000-000000000002");
        Instant published = Instant.parse("2026-08-01T00:00:00Z");
        first.setPublishedAt(published);
        second.setPublishedAt(published);

        var ordered = java.util.stream.Stream.of(
                        new RecommendationRanker.Candidate(second, 1, true, 43, 10),
                        new RecommendationRanker.Candidate(first, 1, true, 43, 10))
                .sorted(ranker.comparator())
                .toList();

        assertThat(ordered.get(0).item().getId()).isEqualTo(first.getId());
    }

    private ContentItem article(String id) {
        return ContentItem.builder()
                .id(UUID.fromString(id))
                .type(ContentType.ARTICLE)
                .status(ContentStatus.APPROVED)
                .stage(ContentStage.PREGNANCY)
                .build();
    }
}
