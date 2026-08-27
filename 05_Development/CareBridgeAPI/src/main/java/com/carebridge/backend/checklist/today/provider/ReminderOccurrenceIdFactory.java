package com.carebridge.backend.checklist.today.provider;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Creates the stable public identity for one reminder occurrence. */
public final class ReminderOccurrenceIdFactory {
    private ReminderOccurrenceIdFactory() {
    }

    public static UUID create(UUID reminderDefinitionId, Instant occurrenceAnchor) {
        Objects.requireNonNull(reminderDefinitionId, "Reminder definition id is required");
        if (occurrenceAnchor == null) {
            // Legacy rows without scheduled_at have no materialized occurrence anchor.
            // Keep the definition id as a stable compatibility identity for those rows.
            return reminderDefinitionId;
        }
        String canonical = "reminder-occurrence-v1|" + reminderDefinitionId + "|" + occurrenceAnchor;
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public static UUID create(
            UUID reminderDefinitionId,
            Instant occurrenceAnchor,
            long occurrenceGeneration) {
        if (occurrenceGeneration <= 0L) {
            return create(reminderDefinitionId, occurrenceAnchor);
        }
        Objects.requireNonNull(reminderDefinitionId, "Reminder definition id is required");
        Objects.requireNonNull(occurrenceAnchor, "Reminder occurrence anchor is required");
        String canonical = "reminder-occurrence-v2|" + reminderDefinitionId + "|"
                + occurrenceAnchor + "|" + occurrenceGeneration;
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }
}
