package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.content.entity.ContentStage;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ChecklistLifecycleContradictoryDatesContractTest {

    @Test
    void pregnancyRejectsEstimatedDueDateEarlierThanLastMenstrualDate() {
        LocalDate lmp = LocalDate.of(2026, 5, 1);
        LocalDate contradictoryEdd = LocalDate.of(2026, 4, 30);
        ChecklistLifecycleEligibilityValue substage = ChecklistLifecycleEligibilityValue.builder()
                .stage(ContentStage.PREGNANCY.name())
                .anchorType(ChecklistAnchorType.LMP)
                .rangeUnit(ChecklistRangeUnit.DAY)
                .startInclusive(0)
                .endInclusive(7)
                .active(true)
                .build();

        assertThatThrownBy(() -> new ChecklistLifecycleEligibilityService().evaluate(
                ContentStage.PREGNANCY,
                substage,
                new ChecklistLifecycleDates(lmp, contradictoryEdd, null, null),
                lmp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LMP")
                .hasMessageContaining("EDD");
    }
}
