package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Contract checks for the forward-only checklist history marker migration.
 *
 * <p>The migration deliberately keeps the existing three-table checklist model and records
 * history on the parent instance. These checks protect the database contract used by the
 * reconciliation and history-read services without requiring a live PostgreSQL server.</p>
 */
class ChecklistStageScopedHistoryMigrationContractTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/"
                    + "V20260801120000__add_stage_scoped_checklist_history.sql");

    @Test
    void migrationAddsPairedHistoryMarkerColumnsAndChecks() throws Exception {
        String sql = normalizedSql();

        assertThat(sql).contains(
                "ALTER TABLE PUBLIC.CHECKLIST_INSTANCES ADD COLUMN HISTORICAL_AT",
                "ALTER TABLE PUBLIC.CHECKLIST_INSTANCES ADD COLUMN HISTORY_REASON_CODE",
                "CHECKLIST_INSTANCES_HISTORY_PAIR_CK",
                "HISTORICAL_AT IS NULL AND HISTORY_REASON_CODE IS NULL",
                "HISTORICAL_AT IS NOT NULL AND HISTORY_REASON_CODE IS NOT NULL",
                "CHECKLIST_INSTANCES_HISTORY_REASON_CK",
                "HISTORY_REASON_CODE LIKE 'LIFECYCLE_STAGE_OBSOLETE%'",
                "HISTORY_REASON_CODE IS NULL",
                "LENGTH(HISTORY_REASON_CODE) <= 80");
    }

    @Test
    void migrationCreatesOwnerScopedCurrentAndHistoryIndexes() throws Exception {
        String sql = normalizedSql();

        assertThat(sql).contains(
                "CREATE INDEX CHECKLIST_INSTANCES_OWNER_HISTORY_IX ON PUBLIC.CHECKLIST_INSTANCES",
                "ON PUBLIC.CHECKLIST_INSTANCES (CONTEXT_OWNER_USER_ID, HISTORICAL_AT DESC)",
                "WHERE HISTORICAL_AT IS NOT NULL",
                "CREATE INDEX CHECKLIST_INSTANCES_OWNER_CURRENT_IX ON PUBLIC.CHECKLIST_INSTANCES",
                "ON PUBLIC.CHECKLIST_INSTANCES (CONTEXT_OWNER_USER_ID, UPDATED_AT DESC)",
                "WHERE HISTORICAL_AT IS NULL");
    }

    @Test
    void migrationIsForwardOnlyAndPreservesChecklistTablesAndChildren() throws Exception {
        String sql = normalizedSql();

        assertThat(sql).doesNotContain(
                "DROP TABLE", "DROP COLUMN", "TRUNCATE", "DELETE FROM",
                "DROP CASCADE");
        assertThat(sql).contains("CHECKLIST_INSTANCES", "CHECKLIST_TASK_INSTANCES", "CHECKLIST_ACTION_COMMANDS");
    }

    private static String normalizedSql() throws Exception {
        assertThat(MIGRATION).exists();
        return Files.readString(MIGRATION)
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
