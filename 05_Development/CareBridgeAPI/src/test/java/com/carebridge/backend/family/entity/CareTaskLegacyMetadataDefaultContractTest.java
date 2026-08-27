package com.carebridge.backend.family.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareTaskLegacyMetadataDefaultContractTest {

    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/V20260729060000__add_unified_task_origin_target.sql");

    @Test
    void legacyManualTaskHasJavaMetadataDefaultsOrDatabaseColumnDefaults() throws IOException {
        CareTask legacyTask = CareTask.builder()
                .taskType("MANUAL_TASK")
                .babyId(UUID.fromString("00000000-0000-0000-0000-000000000301"))
                .build();
        legacyTask.prepareUnifiedTaskMetadata();
        String sql = Files.readString(MIGRATION).toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");

        assertThat(legacyTask.getOrigin()).isEqualTo(ChecklistOrigin.USER_CREATED);
        assertThat(legacyTask.getTargetSubject()).isEqualTo(ChecklistTargetSubject.BABY);
        assertThat(sql).contains(
                "UPDATE PUBLIC.CARE_TASKS SET ORIGIN",
                "WITH RESOLVED_TARGETS AS",
                "UPDATE PUBLIC.CARE_TASKS TASK SET TARGET_SUBJECT",
                "ALTER COLUMN ORIGIN SET DEFAULT",
                "ALTER COLUMN TARGET_SUBJECT SET DEFAULT");
        assertThat(sql.indexOf("UPDATE PUBLIC.CARE_TASKS SET ORIGIN"))
                .isLessThan(sql.indexOf("ALTER COLUMN ORIGIN SET DEFAULT"));
        assertThat(sql.indexOf("UPDATE PUBLIC.CARE_TASKS TASK SET TARGET_SUBJECT"))
                .isLessThan(sql.indexOf("ALTER COLUMN TARGET_SUBJECT SET DEFAULT"));
    }
}
