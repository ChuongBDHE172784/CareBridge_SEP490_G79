package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReminderOccurrenceCommandIdentityMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/V20260729080000__support_reminder_occurrence_command_identity.sql");

    @Test
    void reminderOccurrenceCommandsRetainDefinitionReferenceForDatabaseTargetValidation() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase();

        assertThat(sql).contains("reminder_definition_id");
        assertThat(sql).contains("task_kind = 'reminder'");
        assertThat(sql).contains("coalesce");
        assertThat(sql).contains("checklist_validate_action_command_target");
    }
}
