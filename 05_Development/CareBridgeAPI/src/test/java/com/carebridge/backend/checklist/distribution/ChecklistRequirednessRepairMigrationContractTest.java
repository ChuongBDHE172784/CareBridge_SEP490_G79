package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChecklistRequirednessRepairMigrationContractTest {

    private static final Path MIGRATION = Path.of(
            "src", "main", "resources", "db", "migration",
            "V20260813140000__repair_preconception_sequence_task_requiredness.sql");

    @Test
    void repairIsLeafAuthoritativeAndScopedToCurrentSequenceSnapshots() throws IOException {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        assertThat(sql).contains("set is_required = item.is_required");
        assertThat(sql).contains("lock_version = task.lock_version + 1");
        assertThat(sql).contains("checklist_guard_preconception_requiredness");
        assertThat(sql).contains("task.template_item_version_id = item.template_id");
        assertThat(sql).contains("task.template_version_id = root.template_version_id");
        assertThat(sql).contains("parent.historical_at is null");
        assertThat(sql).contains("parent.origin = 'system_template'");
        assertThat(sql).contains("parent.recipient_role = 'mother'");
        assertThat(sql).contains("parent.care_context_type = 'journey'");
        assertThat(sql).contains("root.stage = 'pre_pregnancy'");
        assertThat(sql).contains("root.template_type = 'mandatory'");
        assertThat(sql).contains("root.recipient_scope = 'mother'");
        assertThat(sql).contains("root.display_order > 0");
        assertThat(sql).contains("task.checklist_quarantine_reason_code is null");
        assertThat(sql).contains("parent.checklist_quarantine_reason_code is null");
        assertThat(sql).contains("item.checklist_quarantine_reason_code is null");
        assertThat(sql).contains("root.checklist_quarantine_reason_code is null");
        assertThat(sql).contains("checklist_preconception_requiredness_drift_remains");
    }
}
