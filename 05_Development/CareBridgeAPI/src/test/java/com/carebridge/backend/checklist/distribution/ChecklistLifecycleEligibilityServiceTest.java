package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.content.entity.ContentStage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ChecklistLifecycleEligibilityServiceTest {

    private final ChecklistLifecycleEligibilityService service = new ChecklistLifecycleEligibilityService();

    @Test
    void prePregnancyUsesNeutralWindowWithoutFabricatingAnAnchorDate() {
        var neutral = substage("PRE_PREGNANCY_ALL", ContentStage.PRE_PREGNANCY,
                ChecklistAnchorType.NONE, ChecklistRangeUnit.DAY, 0, 0);

        var decision = service.evaluate(ContentStage.PRE_PREGNANCY, neutral,
                new ChecklistLifecycleDates(null, null, null, null), LocalDate.of(2026, 7, 30));

        assertThat(decision.eligible()).isTrue();
        assertThat(decision.windowStart()).isNull();
        assertThat(decision.windowEnd()).isNull();
        assertThat(decision.failureCode()).isNull();
    }

    @Test
    void pregnancyRequiresTheNamedAnchorAndHonorsInclusiveDayAndWeekBoundaries() {
        var lmp = LocalDate.of(2026, 1, 1);
        var dates = new ChecklistLifecycleDates(lmp, lmp.plusWeeks(40), null, null);
        var days = substage("PREGNANCY_DAY_7_14", ContentStage.PREGNANCY,
                ChecklistAnchorType.LMP, ChecklistRangeUnit.DAY, 7, 14);

        assertThat(service.evaluate(ContentStage.PREGNANCY, days, dates, lmp.plusDays(6)).eligible()).isFalse();
        assertThat(service.evaluate(ContentStage.PREGNANCY, days, dates, lmp.plusDays(7)).eligible()).isTrue();
        assertThat(service.evaluate(ContentStage.PREGNANCY, days, dates, lmp.plusDays(14)).eligible()).isTrue();
        assertThat(service.evaluate(ContentStage.PREGNANCY, days, dates, lmp.plusDays(15)).eligible()).isFalse();

        var weeks = substage("PREGNANCY_WEEK_1", ContentStage.PREGNANCY,
                ChecklistAnchorType.LMP, ChecklistRangeUnit.WEEK, 1, 1);
        assertThat(service.evaluate(ContentStage.PREGNANCY, weeks, dates, lmp.plusDays(13)).eligible()).isTrue();
        assertThat(service.evaluate(ContentStage.PREGNANCY, weeks, dates, lmp.plusDays(14)).eligible()).isFalse();
    }

    @Test
    void missingOrContradictoryPregnancyAnchorFailsSafely() {
        var lmpStage = substage("PREGNANCY_LMP", ContentStage.PREGNANCY,
                ChecklistAnchorType.LMP, ChecklistRangeUnit.DAY, 0, 10);
        var missing = service.evaluate(ContentStage.PREGNANCY, lmpStage,
                new ChecklistLifecycleDates(null, null, null, null), LocalDate.of(2026, 1, 1));
        assertThat(missing.eligible()).isFalse();
        assertThat(missing.failureCode()).isEqualTo("LIFECYCLE_ANCHOR_MISSING");

        var birthStage = substage("WRONG", ContentStage.PREGNANCY,
                ChecklistAnchorType.BIRTH_DATE, ChecklistRangeUnit.DAY, 0, 10);
        assertThatThrownBy(() -> service.evaluate(ContentStage.PREGNANCY, birthStage,
                new ChecklistLifecycleDates(null, null, null, LocalDate.of(2026, 1, 1)),
                LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anchor");
    }

    @Test
    void pregnancyDerivesLmpWhenTheJourneyWasCapturedFromEdd() {
        var estimatedDueDate = LocalDate.of(2027, 4, 6);
        var derivedLmp = estimatedDueDate.minusDays(280);
        var firstTrimester = substage("PREGNANCY_WEEK_0_12", ContentStage.PREGNANCY,
                ChecklistAnchorType.LMP, ChecklistRangeUnit.WEEK, 0, 12);

        var fromEdd = service.evaluate(ContentStage.PREGNANCY, firstTrimester,
                new ChecklistLifecycleDates(null, estimatedDueDate, null, null),
                derivedLmp.plusWeeks(4));

        assertThat(fromEdd.eligible()).isTrue();
        assertThat(fromEdd.anchorDate()).isEqualTo(derivedLmp);
        assertThat(service.dueAt(ChecklistAnchorType.LMP,
                new ChecklistLifecycleDates(null, estimatedDueDate, null, null),
                1, ChecklistRangeUnit.WEEK, ZoneId.of("UTC")))
                .isEqualTo(derivedLmp.plusWeeks(1).atStartOfDay(ZoneId.of("UTC")).toInstant());

        var missingEdd = service.evaluate(ContentStage.PREGNANCY,
                substage("PREGNANCY_EDD", ContentStage.PREGNANCY,
                        ChecklistAnchorType.EDD, ChecklistRangeUnit.DAY, 0, 1),
                new ChecklistLifecycleDates(derivedLmp, null, null, null),
                derivedLmp.plusWeeks(4));
        assertThat(missingEdd.failureCode()).isEqualTo("LIFECYCLE_ANCHOR_MISSING");
        assertThatThrownBy(() -> service.dueAt(ChecklistAnchorType.EDD,
                new ChecklistLifecycleDates(derivedLmp, null, null, null),
                0, ChecklistRangeUnit.DAY, ZoneId.of("UTC")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anchor is missing");
    }

    @Test
    void postpartumUsesDeliveryDateWithMonthClampingAndDeterministicLocalStart() {
        var delivery = LocalDate.of(2026, 1, 31);
        var dates = new ChecklistLifecycleDates(null, null, delivery, null);
        var months = substage("POSTPARTUM_MONTH_1", ContentStage.POSTPARTUM,
                ChecklistAnchorType.DELIVERY_DATE, ChecklistRangeUnit.MONTH, 1, 1);

        var decision = service.evaluate(ContentStage.POSTPARTUM, months, dates, LocalDate.of(2026, 2, 28));
        assertThat(decision.eligible()).isTrue();
        assertThat(decision.windowStart()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(decision.windowEnd()).isEqualTo(LocalDate.of(2026, 2, 28));

        var zone = ZoneId.of("America/Havana");
        assertThat(service.dueAt(ChecklistAnchorType.DELIVERY_DATE, dates, 72, zone))
                .isEqualTo(LocalDate.of(2026, 4, 13).atStartOfDay(zone).toInstant());
    }

    @Test
    void dueAtSupportsWeekAndMonthOffsetsAtTheLocalStartOfTheConfiguredDay() {
        var dates = new ChecklistLifecycleDates(null, null, LocalDate.of(2026, 1, 31), null);
        assertThat(service.dueAt(ChecklistAnchorType.DELIVERY_DATE, dates, 1,
                ChecklistRangeUnit.WEEK, ZoneId.of("UTC")))
                .isEqualTo(Instant.parse("2026-02-07T00:00:00Z"));
        assertThat(service.dueAt(ChecklistAnchorType.DELIVERY_DATE, dates, 1,
                ChecklistRangeUnit.MONTH, ZoneId.of("UTC")))
                .isEqualTo(Instant.parse("2026-02-28T00:00:00Z"));
    }

    @Test
    void consolidatedPostpartumSupportsBirthDateAndRejectsMalformedRanges() {
        var birth = LocalDate.of(2026, 6, 1);
        var dates = new ChecklistLifecycleDates(LocalDate.of(2025, 9, 1), null, null, birth);
        var baby = substage("BABY_DAY_0_28", ContentStage.POSTPARTUM,
                ChecklistAnchorType.BIRTH_DATE, ChecklistRangeUnit.DAY, 0, 28);
        assertThat(service.evaluate(ContentStage.POSTPARTUM, baby, dates, birth.plusDays(28)).eligible()).isTrue();

        var negative = substage("BAD", ContentStage.POSTPARTUM,
                ChecklistAnchorType.BIRTH_DATE, ChecklistRangeUnit.DAY, -1, 2);
        assertThatThrownBy(() -> service.evaluate(ContentStage.POSTPARTUM, negative, dates, birth))
                .isInstanceOf(IllegalArgumentException.class);
        var reversed = substage("BAD2", ContentStage.POSTPARTUM,
                ChecklistAnchorType.BIRTH_DATE, ChecklistRangeUnit.DAY, 3, 2);
        assertThatThrownBy(() -> service.evaluate(ContentStage.POSTPARTUM, reversed, dates, birth))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ChecklistLifecycleEligibilityValue substage(
            String code,
            ContentStage stage,
            ChecklistAnchorType anchor,
            ChecklistRangeUnit unit,
            int start,
            int end) {
        return ChecklistLifecycleEligibilityValue.builder()
                .stage(stage.name())
                .anchorType(anchor)
                .rangeUnit(unit)
                .startInclusive(start)
                .endInclusive(end)
                .active(true)
                .build();
    }
}
