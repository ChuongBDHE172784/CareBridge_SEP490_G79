package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReminderOccurrenceAliasMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/"
                    + "V20260729160000__persist_reminder_occurrence_aliases.sql");
    private static final Path GENERATION_MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/"
                    + "V20260730010000__add_reminder_occurrence_generation.sql");

    @Test
    void durableAliasIsIndependentOfActionCommandRetentionAndMaintainedByCareTaskWrites()
            throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "CREATE TABLE public.reminder_occurrence_aliases",
                "PRIMARY KEY (occurrence_id)",
                "reminder_definition_id uuid NOT NULL",
                "owner_user_id uuid NOT NULL",
                "scheduled_at timestamptz NOT NULL",
                "CREATE TRIGGER care_tasks_reminder_occurrence_alias_trg",
                "AFTER INSERT OR UPDATE OF scheduled_at, owner_user_id ON public.care_tasks",
                "ON CONFLICT (occurrence_id) DO NOTHING");
        assertThat(sql).doesNotContain("checklist_action_commands");
    }

    @Test
    void reenableGenerationPreservesLegacyV1AndCreatesFreshSameScheduleOccurrence()
            throws Exception {
        String sql = Files.readString(GENERATION_MIGRATION);

        assertThat(sql).contains(
                "reminder_occurrence_generation bigint NOT NULL DEFAULT 0",
                "occurrence_generation bigint NOT NULL DEFAULT 0",
                "UNIQUE (reminder_definition_id, occurrence_generation, scheduled_at)",
                "CREATE OR REPLACE FUNCTION public.reminder_occurrence_id_v2",
                "IF p_occurrence_generation = 0 THEN",
                "RETURN public.reminder_occurrence_id_v1",
                "AFTER INSERT OR UPDATE OF scheduled_at, owner_user_id, reminder_occurrence_generation");
    }
}
