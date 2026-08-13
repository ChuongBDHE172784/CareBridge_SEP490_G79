package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ChecklistPeriodIdentityTest {

    @Test
    void oncePerWindowUsesTheEvaluatedLifecycleWindow() {
        LocalDate delivery = LocalDate.of(2026, 8, 13);
        ChecklistEligibilityDecision decision = ChecklistEligibilityDecision.eligible(
                delivery, delivery, delivery);

        assertThat(ChecklistPeriodIdentity.periodKey(
                ChecklistScheduleType.WEEKLY,
                ChecklistMaterializationPolicy.ONCE_PER_WINDOW,
                decision,
                delivery))
                .isEqualTo("O:2026-08-13:2026-08-13");
    }

    @Test
    void weeklyPregnancyIdentityStillUsesTheEvaluatedAnchorWeek() {
        LocalDate lmp = LocalDate.of(2026, 1, 1);
        LocalDate effectiveDate = lmp.plusWeeks(30);
        ChecklistEligibilityDecision decision = ChecklistEligibilityDecision.eligible(
                lmp, effectiveDate, effectiveDate);

        assertThat(ChecklistPeriodIdentity.periodKey(
                ChecklistScheduleType.WEEKLY,
                ChecklistMaterializationPolicy.EACH_WEEK,
                decision,
                effectiveDate))
                .isEqualTo("W:G:0030:2026-07-30");
    }
}
