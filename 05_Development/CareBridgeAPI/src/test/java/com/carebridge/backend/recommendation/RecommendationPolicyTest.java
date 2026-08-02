package com.carebridge.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.recommendation.dto.RecommendationEnums.WeekEligibilityMode;
import com.carebridge.backend.recommendation.service.RecommendationContext;
import com.carebridge.backend.recommendation.service.RecommendationContextResolver;
import com.carebridge.backend.recommendation.service.RecommendationEligibilityPolicy;
import com.carebridge.backend.recommendation.service.RecommendationRanker;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationPolicyTest {

    @Test
    void missingPregnancyWeekAdmitsOnlyStageWideContent() {
        RecommendationContext context = new RecommendationContextResolver(
                Clock.fixed(Instant.parse("2026-08-02T00:00:00Z"), ZoneOffset.UTC))
                .resolve(MotherJourney.builder().journeyType(JourneyType.PREGNANCY).build());

        assertThat(context.weekState()).isEqualTo(RecommendationContext.WeekState.MISSING);
        assertThat(context.weekEligibilityMode()).isEqualTo(WeekEligibilityMode.STAGE_WIDE_ONLY_MISSING);
        RecommendationEligibilityPolicy policy = new RecommendationEligibilityPolicy();
        assertThat(policy.isHardEligible(article(null, null), context)).isTrue();
        assertThat(policy.isHardEligible(article((short) 12, (short) 20), context)).isFalse();
    }

    @Test
    void eligibilityRequiresApprovedArticleAndExactStage() {
        RecommendationEligibilityPolicy policy = new RecommendationEligibilityPolicy();
        RecommendationContext context = new RecommendationContext(
                ContentStage.PREGNANCY, 12, RecommendationContext.WeekState.KNOWN,
                WeekEligibilityMode.BOUNDED_AND_STAGE_WIDE);

        assertThat(policy.isHardEligible(article((short) 12, (short) 12), context)).isTrue();
        assertThat(policy.isHardEligible(ContentItem.builder()
                .id(UUID.randomUUID()).type(ContentType.ARTICLE).status(ContentStatus.DRAFT)
                .stage(ContentStage.PREGNANCY).eligibleFromWeek((short) 12).eligibleToWeek((short) 12)
                .build(), context)).isFalse();
        assertThat(policy.isHardEligible(ContentItem.builder()
                .id(UUID.randomUUID()).type(ContentType.FAQ).status(ContentStatus.APPROVED)
                .stage(ContentStage.PREGNANCY).build(), context)).isFalse();
    }

    @Test
    void rankingUsesStableUuidAfterAllSemanticKeys() {
        RecommendationRanker ranker = new RecommendationRanker();
        ContentItem first = article(null, null);
        first.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        first.setPublishedAt(Instant.parse("2026-08-01T00:00:00Z"));
        ContentItem second = article(null, null);
        second.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        second.setPublishedAt(first.getPublishedAt());

        var candidates = java.util.stream.Stream.of(
                        new RecommendationRanker.Candidate(second, 1, true, 43, 10),
                        new RecommendationRanker.Candidate(first, 1, true, 43, 10))
                .sorted(ranker.comparator())
                .toList();
        assertThat(candidates.get(0).item().getId()).isEqualTo(first.getId());
    }

    private ContentItem article(Short from, Short to) {
        return ContentItem.builder()
                .id(UUID.randomUUID())
                .type(ContentType.ARTICLE)
                .status(ContentStatus.APPROVED)
                .stage(ContentStage.PREGNANCY)
                .eligibleFromWeek(from)
                .eligibleToWeek(to)
                .publishedAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build();
    }
}
