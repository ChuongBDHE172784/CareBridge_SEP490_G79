package com.carebridge.backend.reminder.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.today.provider.ReminderOccurrenceIdFactory;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Decides the appointment identity of the consolidated notification_jobs table.
 *
 * <p>V3 §3.8 keeps {@code occurrence_generation} out of the unique identity on one
 * stated assumption: that occurrence-ID v2 already folds the generation into
 * {@code occurrence_id}. It also says that if the ID factory stops guaranteeing
 * this, the migration must stop and add {@code occurrence_generation} to the
 * identity rather than silently change semantics.
 *
 * <p>This test is that check. If it ever fails, the partial unique index in
 * V20260806160000 is wrong and two generations of the same occurrence can collide
 * on one identity — meaning a regenerated appointment would silently drop its
 * notifications.
 */
class ReminderOccurrenceIdGenerationContractTest {

    private static final UUID REMINDER_ID =
            UUID.fromString("7f000000-0000-0000-0000-0000000000a1");
    private static final Instant ANCHOR = Instant.parse("2026-08-10T02:00:00Z");

    @Test
    @DisplayName("Successive generations of the same occurrence get different ids")
    void distinctGenerationsProduceDistinctOccurrenceIds() {
        Set<UUID> ids = new HashSet<>();
        for (long generation = 1L; generation <= 50L; generation++) {
            ids.add(ReminderOccurrenceIdFactory.create(REMINDER_ID, ANCHOR, generation));
        }

        assertThat(ids)
                .as("50 generations must yield 50 distinct occurrence ids")
                .hasSize(50);
    }

    @Test
    @DisplayName("A generated occurrence id never collides with the ungenerated one")
    void generatedIdsNeverCollideWithTheLegacyIdentity() {
        UUID legacy = ReminderOccurrenceIdFactory.create(REMINDER_ID, ANCHOR);

        for (long generation = 1L; generation <= 50L; generation++) {
            assertThat(ReminderOccurrenceIdFactory.create(REMINDER_ID, ANCHOR, generation))
                    .as("generation %s must differ from the v1 identity", generation)
                    .isNotEqualTo(legacy);
        }
    }

    @Test
    @DisplayName("Generation 0 and no generation are the same occurrence, deliberately")
    void generationZeroIsTheUngeneratedIdentity() {
        // Not a collision: zero means "never regenerated", so it must keep resolving
        // to the same occurrence as the two-argument call, or existing rows would
        // change identity the moment the column was populated.
        assertThat(ReminderOccurrenceIdFactory.create(REMINDER_ID, ANCHOR, 0L))
                .isEqualTo(ReminderOccurrenceIdFactory.create(REMINDER_ID, ANCHOR));
        assertThat(ReminderOccurrenceIdFactory.create(REMINDER_ID, ANCHOR, -1L))
                .isEqualTo(ReminderOccurrenceIdFactory.create(REMINDER_ID, ANCHOR));
    }

    @Test
    @DisplayName("Occurrence ids are stable across calls, so identity is reproducible")
    void occurrenceIdsAreDeterministic() {
        assertThat(ReminderOccurrenceIdFactory.create(REMINDER_ID, ANCHOR, 3L))
                .isEqualTo(ReminderOccurrenceIdFactory.create(REMINDER_ID, ANCHOR, 3L));
    }

    @Test
    @DisplayName("Different anchors and different reminders stay distinct within a generation")
    void anchorAndReminderStillDiscriminate() {
        UUID otherReminder = UUID.fromString("7f000000-0000-0000-0000-0000000000a2");
        Instant otherAnchor = ANCHOR.plusSeconds(3600);

        UUID base = ReminderOccurrenceIdFactory.create(REMINDER_ID, ANCHOR, 2L);

        assertThat(ReminderOccurrenceIdFactory.create(otherReminder, ANCHOR, 2L)).isNotEqualTo(base);
        assertThat(ReminderOccurrenceIdFactory.create(REMINDER_ID, otherAnchor, 2L)).isNotEqualTo(base);
    }
}
