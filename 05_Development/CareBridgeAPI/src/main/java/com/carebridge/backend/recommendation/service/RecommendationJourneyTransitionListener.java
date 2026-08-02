package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.journey.event.MotherJourneyTransitioned;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Marks an active profile for review after an event-driven maternal stage transition. */
@Component
@RequiredArgsConstructor
public class RecommendationJourneyTransitionListener {
    private final RecommendationService recommendationService;

    @EventListener
    public void onJourneyTransitioned(MotherJourneyTransitioned event) {
        if (event == null) {
            return;
        }
        boolean stageChanged = event.eventType() == com.carebridge.backend.journey.entity.JourneyTransitionType.STAGE_CHANGED;
        boolean outcomeChangedStage = (event.eventType() == com.carebridge.backend.journey.entity.JourneyTransitionType.OUTCOME_RECORDED
                || event.eventType() == com.carebridge.backend.journey.entity.JourneyTransitionType.OUTCOME_CORRECTED)
                && event.journeyType() == com.carebridge.backend.journey.entity.JourneyType.POSTPARTUM;
        if (!stageChanged && !outcomeChangedStage) {
            return;
        }
        recommendationService.markStageReview(event.ownerUserId(), event.journeyId(), event.journeyType());
    }
}
