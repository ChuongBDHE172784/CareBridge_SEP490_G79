package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.HealthMemoryContextItem;
import com.carebridge.backend.triage.entity.HealthMemoryEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Health-context memory contract (CB-TRIAGE-THMC-IMP-001 §8.1).
 *
 * @version 2.0 (additive — existing list/delete signatures unchanged)
 */
public interface HealthMemoryService {
    List<HealthMemoryEntry> list(UUID userId, TriageStage stage, UUID profileId);
    void delete(UUID userId, UUID entryId);

    /**
     * WRITE path (BR-THMC-001/003/005). Loads the session owned by userId; when it is
     * COMPLETED with a persistable riskLevel and no active memory exists for it yet,
     * inserts one health_context_memories row with expires_at = completedAt + ttlDays.
     * Returns Optional.empty() (and writes nothing) for NEED_MORE_INFO/FAILED/PROCESSING
     * sessions, for replayed events (idempotency), or when the session is not found.
     * MUST NOT throw for business no-op cases; repository exceptions propagate to the
     * caller (the AFTER_COMMIT handler catches and logs them).
     */
    Optional<HealthMemoryEntry> writeFromCompletedSession(UUID sessionId, UUID userId);

    /**
     * READ path (BR-THMC-002/004/006). Returns the newest-first active (non-expired,
     * non-deleted) memories of THIS user for the given stage + subject profile, mapped
     * to bounded HealthMemoryContextItem values (maxContextEntries / maxSummaryChars).
     * Returns an empty list when profileId is null (legacy sessions without a profile)
     * — it never throws TRIAGE-014, unlike list(), because intake must not be blocked.
     */
    List<HealthMemoryContextItem> loadContextForIntake(UUID userId, TriageStage stage, UUID profileId);
}
