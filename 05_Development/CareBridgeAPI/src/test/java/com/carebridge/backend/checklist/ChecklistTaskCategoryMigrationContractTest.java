package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChecklistTaskCategoryMigrationContractTest {
    @Test
    void forwardOnlyMigrationPersistsCanonicalTaskCategory() throws Exception {
        String sql = Files.readString(Path.of(
                "src/test/resources/db/migration-legacy/V20260730050000__persist_checklist_task_category.sql"));
        assertThat(sql).contains("ADD COLUMN category", "DEFAULT 'GENERAL'", "NOT NULL");
        assertThat(sql).contains("task.category IS DISTINCT FROM legacy.category");
        assertThat(sql).contains("DELIVERY", "PAPERWORK", "BABY_CARE", "GENERAL");
        assertThat(sql).contains("UPDATE public.checklist_task_instances")
                .contains("preparation_checklist_items")
                .contains("task.checklist_task_instance_id = legacy.checklist_item_id");
    }
}
