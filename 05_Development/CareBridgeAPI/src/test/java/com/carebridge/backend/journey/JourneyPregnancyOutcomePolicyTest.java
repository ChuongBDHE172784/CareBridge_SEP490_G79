package com.carebridge.backend.journey;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.policy.JourneyTransitionPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JourneyPregnancyOutcomePolicyTest {

    private final JourneyTransitionPolicy policy = new JourneyTransitionPolicy();

    @Test
    void finalOutcomesTransitionPregnancyToPostpartum() {
        for (var outcome : new PregnancyOutcomeType[] {
                PregnancyOutcomeType.LIVE_BIRTH,
                PregnancyOutcomeType.PREGNANCY_LOSS,
                PregnancyOutcomeType.STILLBIRTH
        }) {
            assertThat(policy.outcomeTargetStage(JourneyType.PREGNANCY, outcome, false))
                    .isEqualTo(JourneyType.POSTPARTUM);
        }
    }

    @Test
    void ongoingAndUnknownRemainPregnancy() {
        assertThat(policy.outcomeTargetStage(
                        JourneyType.PREGNANCY, PregnancyOutcomeType.ONGOING, false))
                .isEqualTo(JourneyType.PREGNANCY);
        assertThat(policy.outcomeTargetStage(
                        JourneyType.PREGNANCY, PregnancyOutcomeType.UNKNOWN, false))
                .isEqualTo(JourneyType.PREGNANCY);
    }

    @Test
    void postpartumOnlyAcceptsExplicitFinalOutcomeCorrection() {
        assertThatCode(() -> policy.outcomeTargetStage(
                        JourneyType.POSTPARTUM, PregnancyOutcomeType.PREGNANCY_LOSS, true))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> policy.outcomeTargetStage(
                        JourneyType.POSTPARTUM, PregnancyOutcomeType.ONGOING, true))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("OUTCOME_STAGE_CONFLICT"));
        assertThatThrownBy(() -> policy.outcomeTargetStage(
                        JourneyType.POSTPARTUM, PregnancyOutcomeType.LIVE_BIRTH, false))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("OUTCOME_STAGE_CONFLICT"));
    }
}
