package com.carebridge.backend.reminder.service;

import java.util.UUID;

/** Forensic identity supplied by the unified task-action boundary. */
public record ReminderActionAuditContext(
        UUID occurrenceId,
        String reasonCode,
        UUID correlationId,
        UUID actorUserId,
        UUID careGroupId) {
    public ReminderActionAuditContext(UUID occurrenceId, String reasonCode, UUID correlationId) {
        this(occurrenceId, reasonCode, correlationId, null, null);
    }
}
