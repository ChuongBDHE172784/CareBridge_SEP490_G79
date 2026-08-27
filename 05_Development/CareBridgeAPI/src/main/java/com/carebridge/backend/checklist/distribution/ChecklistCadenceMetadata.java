package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistMaterializationMode;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import java.time.ZoneId;

/** Server-resolved cadence identity carried into the transactional materializer. */
public record ChecklistCadenceMetadata(
        ChecklistScheduleType scheduleType,
        ChecklistMaterializationPolicy materializationPolicy,
        String periodKey,
        ZoneId scheduleZone,
        ChecklistMaterializationMode materializationMode,
        Boolean wasActionable) {

    public ChecklistCadenceMetadata {
        if (scheduleType == null || materializationPolicy == null) {
            throw new IllegalArgumentException("Cadence type and policy are required");
        }
        if (periodKey == null || periodKey.isBlank()) {
            throw new IllegalArgumentException("Cadence period key is required");
        }
        if (scheduleZone == null) {
            throw new IllegalArgumentException("Cadence schedule zone is required");
        }
        if (materializationMode == null || wasActionable == null) {
            throw new IllegalArgumentException("Cadence materialization state is required");
        }
    }

    public static ChecklistCadenceMetadata interactive(
            ChecklistScheduleType scheduleType,
            ChecklistMaterializationPolicy policy,
            String periodKey,
            ZoneId scheduleZone) {
        return new ChecklistCadenceMetadata(scheduleType, policy, periodKey, scheduleZone,
                ChecklistMaterializationMode.INTERACTIVE, Boolean.TRUE);
    }

    public ChecklistCadenceMetadata catchUp() {
        return new ChecklistCadenceMetadata(scheduleType, materializationPolicy, periodKey,
                scheduleZone, ChecklistMaterializationMode.CATCH_UP, Boolean.FALSE);
    }
}
