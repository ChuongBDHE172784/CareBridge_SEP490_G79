package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistMaterializationMode;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Canonical period identities for cadence and exact V2 non-cadence occurrences. */
final class ChecklistPeriodIdentity {

    static final short V1_CONTRACT_VERSION = 1;
    static final short V2_CONTRACT_VERSION = 2;
    static final String V2_NON_CADENCE_PERIOD_KEY = "O:USER_CREATED";
    static final String V2_NON_CADENCE_ZONE_ID = "UTC";
    static final ChecklistMaterializationMode V2_NON_CADENCE_MODE =
            ChecklistMaterializationMode.INTERACTIVE;
    static final boolean V2_NON_CADENCE_WAS_ACTIONABLE = true;

    private ChecklistPeriodIdentity() {
    }

    static boolean isV1NonCadenceIdentity(
            Short contractVersion,
            String periodKey,
            String scheduleZoneId) {
        return (contractVersion == null
                || Short.valueOf(V1_CONTRACT_VERSION).equals(contractVersion))
                && periodKey == null
                && scheduleZoneId == null;
    }

    static boolean isV2NonCadenceIdentity(
            Short contractVersion,
            String periodKey,
            String scheduleZoneId,
            ChecklistMaterializationMode materializationMode,
            Boolean wasActionable) {
        return Short.valueOf(V2_CONTRACT_VERSION).equals(contractVersion)
                && V2_NON_CADENCE_PERIOD_KEY.equals(periodKey)
                && V2_NON_CADENCE_ZONE_ID.equals(scheduleZoneId)
                && materializationMode == V2_NON_CADENCE_MODE
                && Boolean.valueOf(V2_NON_CADENCE_WAS_ACTIONABLE).equals(wasActionable);
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
