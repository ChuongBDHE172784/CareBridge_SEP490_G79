package com.carebridge.backend.checklist.sequence;

import com.carebridge.backend.checklist.today.dto.TodaySequenceProjection;
import java.time.Instant;
import java.util.UUID;

public record ChecklistSequenceAdvanceResponse(
        UUID predecessorInstanceId,
        UUID successorInstanceId,
        Instant advancedAt,
        TodaySequenceProjection sequence) {
    public ChecklistSequenceAdvanceResponse asReplay() {
        return this;
    }
}
