package com.carebridge.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyTransitionType;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.event.MotherJourneyTransitioned;
import com.carebridge.backend.recommendation.service.RecommendationJourneyTransitionListener;
import com.carebridge.backend.recommendation.service.RecommendationService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationJourneyTransitionListenerTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000009901");
    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000009902");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000009903");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000009904");

    @Mock
    private RecommendationService recommendationService;

    @Test
    void relevantStageAndPostpartumOutcomeEventsInvokeReviewMarking() {
        RecommendationJourneyTransitionListener listener = new RecommendationJourneyTransitionListener(
                recommendationService);

        listener.onJourneyTransitioned(event(JourneyTransitionType.STAGE_CHANGED, JourneyType.PREGNANCY));
        listener.onJourneyTransitioned(event(JourneyTransitionType.OUTCOME_RECORDED, JourneyType.POSTPARTUM));
        listener.onJourneyTransitioned(event(JourneyTransitionType.OUTCOME_CORRECTED, JourneyType.POSTPARTUM));

        verify(recommendationService, times(1))
                .markStageReview(OWNER_ID, JOURNEY_ID, JourneyType.PREGNANCY);
        verify(recommendationService, times(2))
                .markStageReview(OWNER_ID, JOURNEY_ID, JourneyType.POSTPARTUM);
        verifyNoMoreInteractions(recommendationService);
    }

    @Test
    void nullAndIrrelevantEventsDoNotInvokeReviewMarking() {
        RecommendationJourneyTransitionListener listener = new RecommendationJourneyTransitionListener(
                recommendationService);

        listener.onJourneyTransitioned(null);
        listener.onJourneyTransitioned(event(JourneyTransitionType.DATES_CHANGED, JourneyType.PREGNANCY));
        listener.onJourneyTransitioned(event(JourneyTransitionType.OUTCOME_RECORDED, JourneyType.PREGNANCY));
        listener.onJourneyTransitioned(event(JourneyTransitionType.OUTCOME_CORRECTED, JourneyType.PREGNANCY));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void recommendationFailureIsContainedAtThePostCommitListenerBoundary() {
        RecommendationJourneyTransitionListener listener = new RecommendationJourneyTransitionListener(
                recommendationService);
        MotherJourneyTransitioned event = event(JourneyTransitionType.STAGE_CHANGED, JourneyType.PREGNANCY);
        doThrow(new IllegalStateException("synthetic review failure"))
                .when(recommendationService)
                .markStageReview(eq(OWNER_ID), eq(JOURNEY_ID), eq(JourneyType.PREGNANCY));

        assertThatCode(() -> listener.onJourneyTransitioned(event)).doesNotThrowAnyException();

        verify(recommendationService).markStageReview(OWNER_ID, JOURNEY_ID, JourneyType.PREGNANCY);
    }

    private MotherJourneyTransitioned event(JourneyTransitionType type, JourneyType journeyType) {
        return new MotherJourneyTransitioned(
                EVENT_ID,
                JOURNEY_ID,
                OWNER_ID,
                type,
                journeyType,
                JourneyStatus.ACTIVE,
                1L,
                Instant.parse("2026-08-05T00:00:00Z"),
                CORRELATION_ID);
    }
}
