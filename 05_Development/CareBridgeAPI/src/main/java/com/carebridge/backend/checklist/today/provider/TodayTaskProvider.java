package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.today.dto.TodayTaskCandidate;
import com.carebridge.backend.checklist.today.model.TaskKind;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.ZoneId;

public interface TodayTaskProvider {
    TaskKind taskKind();

    List<TodayTaskCandidate> findAuthorizedTasks(UUID actorUserId);

    /** Optional date-aware read path used to hide stale lifecycle rows without writes. */
    default List<TodayTaskCandidate> findAuthorizedTasks(
            UUID actorUserId, LocalDate effectiveDate, ZoneId zone) {
        return findAuthorizedTasks(actorUserId);
    }

    /** Whether the provider implements the read-only date-aware lifecycle filter. */
    default boolean supportsDateAwareRead() {
        return false;
    }
}
