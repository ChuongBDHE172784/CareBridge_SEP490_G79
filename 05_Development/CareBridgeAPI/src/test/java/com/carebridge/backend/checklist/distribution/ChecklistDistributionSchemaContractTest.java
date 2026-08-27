package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** RED contract for the additive Phase 1 schema migration. */
class ChecklistDistributionSchemaContractTest {

    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/V20260729030000__add_checklist_distribution_v2_foundation.sql");

    @Test
    void migrationExistsAndDeclaresAllV2Tables() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "CREATE TABLE public.checklist_substages",
                "CREATE TABLE public.checklist_template_recipient_roles",
                "CREATE TABLE public.checklist_care_group_contexts",
                "CREATE TABLE public.checklist_instances",
                "CREATE TABLE public.checklist_task_instances",
                "CREATE TABLE public.checklist_action_commands",
                "CREATE TABLE public.checklist_distribution_outbox",
                "CREATE TABLE public.checklist_reconciliation_runs",
                "CREATE TABLE public.checklist_reconciliation_candidates",
                "CREATE TABLE public.checklist_migration_quarantine");
    }

    @Test
    void migrationEnforcesParentChildTargetAndNonNullKeyInvariants() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "distribution_key char(64) NOT NULL",
                "task_key char(64) NOT NULL",
                "target_subject varchar(10) NOT NULL",
                "checklist_instances_template_pair_ck",
                "checklist_task_instances_target_ck",
                "checklist_instances_distribution_key_uk",
                "checklist_task_instances_task_key_uk");
        int parentStart = sql.indexOf("CREATE TABLE public.checklist_instances");
        int childStart = sql.indexOf("CREATE TABLE public.checklist_task_instances");
        assertThat(parentStart).isGreaterThanOrEqualTo(0);
        assertThat(childStart).isGreaterThan(parentStart);
        assertThat(sql.substring(parentStart, childStart)).doesNotContain("target_subject");
    }

    @Test
    void migrationSetsLegacyReviewAndDefaultDenyPermissions() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "migration_review_required = true",
                "distribution_enabled = false",
                "jsonb_typeof(base_permission->'CHECKLIST_VIEW') = 'boolean'",
                "jsonb_typeof(base_permission->'CHECKLIST_COMPLETE') = 'boolean'");
    }

    @Test
    void migrationAddsTypedChecklistAuditFieldsWithoutDroppingLegacyAuditColumns() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "ALTER TABLE public.audit_events",
                "ADD COLUMN actor_type varchar(20)",
                "ADD COLUMN actor_service varchar(80)",
                "ADD COLUMN reason_code varchar(80)",
                "ADD COLUMN care_context_type varchar(10)",
                "ADD COLUMN checklist_task_instance_id uuid");
        assertThat(sql).doesNotContain("DROP TABLE public.audit_events", "DROP COLUMN");
    }
}
