package com.carebridge.backend.reminder.service;

import com.carebridge.backend.triage.event.IntakeSessionCompleted;

import java.util.Optional;
import java.util.UUID;

/**
 * CB-TYFU-IMP-001 §8.1 — schedules the automatic follow-up care item for a
 * completed YELLOW triage session (roadmap Part III.3).
 *
 * @version 1.0
 */
public interface ITriageFollowUpService {

    /**
     * Creates the follow-up scheduled care item for a completed YELLOW triage session.
     * Idempotent on (ReminderType.TRIAGE_FOLLOW_UP, event.sessionId()) — BR-TYFU-002.
     *
     * @return Optional with the created care_item_id, or Optional.empty() when skipped
     *         (duplicate or session not found — TYFU-001/TYFU-002).
     * @throws org.springframework.dao.DataAccessException on persistence failure (TYFU-003;
     *         contained by the handler, never reaches the user).
     */
    Optional<UUID> scheduleFollowUp(IntakeSessionCompleted event);
}
