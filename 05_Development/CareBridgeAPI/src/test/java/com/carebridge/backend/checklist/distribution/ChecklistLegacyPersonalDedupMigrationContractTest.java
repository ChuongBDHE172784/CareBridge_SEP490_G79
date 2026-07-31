package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ChecklistLegacyPersonalDedupMigrationContractTest {

    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/"
                    + "V20260731010000__deduplicate_legacy_personal_mother_checklists.sql");

    @Test
    void migrationUsesGroupNeutralLogicalIdentityAndTargetsOnlyLegacyMotherTemplates() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "instance.recipient_role = 'MOTHER'",
                "instance.origin = 'SYSTEM_TEMPLATE'",
                "instance.status <> 'CANCELLED'",
                "PARTITION BY template_version_id, recipient_user_id",
                "care_context_type, care_context_id, context_owner_user_id",
                "window_start, window_end");
        assertThat(sql.substring(
                sql.indexOf("PARTITION BY template_version_id"),
                sql.indexOf("window_start, window_end") + "window_start, window_end".length()))
                .doesNotContain("care_group_id");
    }

    @Test
    void migrationCancelsOnlyAllPendingParentsAndChildrenWhileKeepingOneSafeCopy() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "LOCK TABLE public.checklist_instances IN SHARE ROW EXCLUSIVE MODE",
                "LOCK TABLE public.checklist_task_instances IN SHARE ROW EXCLUSIVE MODE",
                "instance.status = 'PENDING'",
                "instance.completed_at IS NULL",
                "instance.cancelled_at IS NULL",
                "instance.cancellation_reason_code IS NULL",
                "AND EXISTS (",
                "FROM public.checklist_task_instances task",
                "task.status <> 'PENDING'",
                "task.completed_at IS NOT NULL",
                "task.skipped_at IS NOT NULL",
                "task.cancelled_at IS NOT NULL",
                "task.action_reason_code IS NOT NULL",
                "count(*) FILTER (WHERE NOT safely_cancellable)",
                "CASE WHEN care_group_id IS NULL THEN 0 ELSE 1 END",
                "preserved_count > 0 OR safe_rank > 1",
                "task.status = 'PENDING'",
                "sibling.status <> 'PENDING'",
                "instance.status = 'PENDING'",
                "LEGACY_PERSONAL_DUPLICATE");
    }

    @Test
    void migrationIsForwardOnlyAndNeverRekeysOrDeletesHistory() throws Exception {
        String normalized = Files.readString(MIGRATION)
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        assertThat(normalized).doesNotContain(
                "DELETE FROM",
                "DROP TABLE",
                "DROP COLUMN",
                "SET DISTRIBUTION_KEY",
                "SET CARE_GROUP_ID");
        assertThat(normalized).contains(
                "UPDATE PUBLIC.CHECKLIST_TASK_INSTANCES",
                "UPDATE PUBLIC.CHECKLIST_INSTANCES",
                "INSERT INTO PUBLIC.AUDIT_EVENTS");
    }
}
