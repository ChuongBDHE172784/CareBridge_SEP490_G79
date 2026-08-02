package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.recommendation.dto.RecommendationEnums.WeekEligibilityMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class RecommendationContextResolver {
    private final Clock clock;

    @Autowired
    public RecommendationContextResolver() { this(Clock.systemUTC()); }
    public RecommendationContextResolver(Clock clock) { this.clock = clock; }

    public RecommendationContext resolve(MotherJourney journey) {
        ContentStage stage = switch (journey.getJourneyType()) {
            case PRE_PREGNANCY -> ContentStage.PRE_PREGNANCY;
            case PREGNANCY -> ContentStage.PREGNANCY;
            case POSTPARTUM -> ContentStage.POSTPARTUM;
            case BABY_CARE -> throw new IllegalArgumentException("BABY_CARE is outside maternal recommendations");
        };
        if (journey.getJourneyType() != JourneyType.PREGNANCY) {
            return new RecommendationContext(stage, null, RecommendationContext.WeekState.NOT_APPLICABLE,
                    WeekEligibilityMode.NOT_APPLICABLE);
        }
        LocalDate today = LocalDate.now(clock.withZone(com.carebridge.backend.recommendation.RecommendationConstants.BUSINESS_ZONE));
        LocalDate lmp = journey.getLastMenstrualDate();
        if (lmp == null && journey.getEstimatedDueDate() != null) lmp = journey.getEstimatedDueDate().minusDays(280);
        if (lmp == null) {
            return new RecommendationContext(stage, null, RecommendationContext.WeekState.MISSING,
                    WeekEligibilityMode.STAGE_WIDE_ONLY_MISSING);
        }
        long days = ChronoUnit.DAYS.between(lmp, today);
        int week = (int) Math.floorDiv(days, 7);
        if (week < 0 || week > 42) {
            return new RecommendationContext(stage, null, RecommendationContext.WeekState.OUT_OF_RANGE,
                    WeekEligibilityMode.STAGE_WIDE_ONLY_OUT_OF_RANGE);
        }
        return new RecommendationContext(stage, week, RecommendationContext.WeekState.KNOWN,
                WeekEligibilityMode.BOUNDED_AND_STAGE_WIDE);
    }
}
