package com.carebridge.backend.exercise.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CanonicalObservationMappingTest {

    @Test
    void exerciseSafetyStoresNonColumnFieldsInRawPayload() {
        ExerciseSafetyCheck check = ExerciseSafetyCheck.builder()
                .safetyCheckId(UUID.randomUUID())
                .exerciseId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .journeyId(UUID.randomUUID())
                .answerJson(Map.of("Q1", true))
                .redFlagDetected(false)
                .resultStatus(SafetyCheckStatus.CLEARED)
                .createdAt(OffsetDateTime.now())
                .build();

        check.prepareCanonicalObservation();

        assertThat(check.getSubjectType()).isEqualTo("MOTHER");
        assertThat(check.getCanonicalPayload())
                .containsKeys("exerciseTemplateId", "ownerUserId", "answer")
                .containsEntry("recordStatus", "CLEARED");
    }

    @Test
    void postureFeedbackRoundTripsPayloadProjection() {
        UUID configId = UUID.randomUUID();
        PostureFeedbackEvent event = PostureFeedbackEvent.builder()
                .feedbackEventId(UUID.randomUUID())
                .exerciseSessionId(UUID.randomUUID())
                .journeyId(UUID.randomUUID())
                .postureConfigId(configId)
                .eventTimeMs(1500L)
                .postureCode("GOOD_FORM")
                .confidenceScore(new BigDecimal("0.95"))
                .build();

        event.prepareCanonicalObservation();
        event.setPostureConfigId(null);
        event.setPostureCode(null);
        event.setEventTimeMs(null);
        event.hydrateCanonicalObservation();

        assertThat(event.getPostureConfigId()).isEqualTo(configId);
        assertThat(event.getPostureCode()).isEqualTo("GOOD_FORM");
        assertThat(event.getEventTimeMs()).isEqualTo(1500L);
    }
}
