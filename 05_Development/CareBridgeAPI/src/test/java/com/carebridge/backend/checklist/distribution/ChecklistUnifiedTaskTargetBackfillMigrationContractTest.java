package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChecklistUnifiedTaskTargetBackfillMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/V20260729060000__add_unified_task_origin_target.sql");

    @Test
    void canonicalCareSubjectTypeWinsBeforeBabyIdCompatibilityFallback() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase();

        assertThat(sql).contains("left join public.care_subjects");
        assertThat(sql).contains("subject.subject_type = 'baby'");
        assertThat(sql).contains("subject.subject_type = 'mother'");
        assertThat(sql).contains("task.baby_id is not null");
    }
}
