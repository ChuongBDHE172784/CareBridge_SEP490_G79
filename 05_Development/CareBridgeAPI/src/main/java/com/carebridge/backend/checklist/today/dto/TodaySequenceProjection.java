package com.carebridge.backend.checklist.today.dto;

import com.carebridge.backend.checklist.sequence.ChecklistSequenceState;
import java.util.UUID;

/** Additive Today projection for the ordered PRE_PREGNANCY checklist chain. */
public record TodaySequenceProjection(
        ChecklistSequenceState sequenceState,
        UUID currentInstanceId,
        UUID currentTemplateVersionId,
        String currentSetName,
        Integer currentPosition,
        Integer totalPositions,
        Integer qualifiedPositions,
        boolean advanceAvailable,
        TodaySequenceNextSet nextSet,
        boolean sequenceComplete,
        String blockedReasonCode) {

    public static TodaySequenceProjection outsideScope() {
        return null;
    }
}
