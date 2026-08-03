package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Protects the forward-only schema contract for ordered PRE_PREGNANCY sets. */
class ChecklistSequentialPreconceptionMigrationContractTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/"
                    + "V20260803090000__add_preconception_sequence_contract.sql");

    @Test
    void allowsSequenceHistoryReasonAndDurableAdvanceIdentity() throws Exception {
        String sql = normalizedSql();

        assertThat(sql).contains(
                "HISTORY_REASON_CODE = 'SEQUENCE_STEP_COMPLETED'",
                "TASK_KIND IN ('CHECKLIST', 'CARE_TASK', 'REMINDER', 'CHECKLIST_SEQUENCE')",
                "ACTION_TYPE IN ('COMPLETE', 'SKIP', 'REOPEN', 'ADVANCE')",
                "NEW.TASK_KIND = 'CHECKLIST_SEQUENCE'",
                "CHECKLIST_INSTANCE_ID = NEW.TASK_ID");
    }

    @Test
    void indexesPositiveActiveMotherPreconceptionPositions() throws Exception {
        String sql = normalizedSql();

        assertThat(sql).contains(
                "CARE_ITEM_TEMPLATES_PRECONCEPTION_SEQUENCE_POSITION_UK",
                "DISPLAY_ORDER > 0",
                "STAGE = 'PRE_PREGNANCY'",
                "TEMPLATE_TYPE = 'MANDATORY'",
                "RECIPIENT_SCOPE = 'MOTHER'",
                "CONTENT_STATUS = 'APPROVED'");
    }

    @Test
    void remainsForwardOnly() throws Exception {
        String sql = normalizedSql();
        assertThat(sql).doesNotContain("DROP TABLE", "DROP COLUMN", "TRUNCATE", "DELETE FROM");
    }

    private static String normalizedSql() throws Exception {
        assertThat(MIGRATION).exists();
        return Files.readString(MIGRATION)
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
