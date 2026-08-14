package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Canonical cadence period identity derived from the evaluated lifecycle window. */
final class ChecklistPeriodIdentity {

    private ChecklistPeriodIdentity() {
    }

    static String periodKey(
            ChecklistScheduleType scheduleType,
            ChecklistMaterializationPolicy policy,
            ChecklistEligibilityDecision decision,
            LocalDate effectiveDate) {
        if (scheduleType == null || policy == null || decision == null
                || !decision.eligible() || effectiveDate == null) {
            return null;
        }
        if (scheduleType == ChecklistScheduleType.DAILY
                && policy == ChecklistMaterializationPolicy.EACH_DAY) {
            return "D:" + effectiveDate;
        }
        if (scheduleType == ChecklistScheduleType.WEEKLY
                && policy == ChecklistMaterializationPolicy.EACH_WEEK) {
            if (decision.anchorDate() == null || decision.windowStart() == null
                    || effectiveDate.isBefore(decision.anchorDate())) {
                return null;
            }
            long completedWeek = ChronoUnit.DAYS.between(decision.anchorDate(), effectiveDate) / 7;
            return String.format(java.util.Locale.ROOT,
                    "W:G:%04d:%s", completedWeek, decision.windowStart());
        }
        if (policy == ChecklistMaterializationPolicy.ONCE_PER_WINDOW) {
            if (decision.windowStart() == null) {
                return null;
            }
            String end = decision.windowEnd() == null ? "EXIT" : decision.windowEnd().toString();
            return "O:" + decision.windowStart() + ":" + end;
        }
        return null;
    }
}
