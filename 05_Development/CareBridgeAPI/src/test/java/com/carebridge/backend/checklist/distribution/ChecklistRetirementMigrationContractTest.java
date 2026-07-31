package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ChecklistRetirementMigrationContractTest {

    private static final Path MIGRATION_DIRECTORY =
            Path.of("src/main/resources/db/migration");
    private static final Path RETIREMENT_MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/"
                    + "V20260731030000__retire_checklist_support_tables.sql");
    private static final Path PRE_FINALIZER =
            MIGRATION_DIRECTORY.resolve("checklist_retirement_pre_finalizer.sql");
    private static final Path POST_FINALIZER =
            MIGRATION_DIRECTORY.resolve("checklist_retirement_post_finalizer.sql");
    private static final Pattern DROP_TABLE = Pattern.compile(
            "(?im)^\\s*DROP\\s+TABLE\\s+public\\.([a-z0-9_]+)\\s*;");
    private static final Pattern LOCK_TABLE = Pattern.compile(
            "(?im)^\\s*LOCK\\s+TABLE\\s+public\\.([a-z0-9_]+)\\s+IN\\s+ACCESS\\s+EXCLUSIVE\\s+MODE\\s*;");

    private static final List<String> RETIRED_TABLES = List.of(
            "checklist_reconciliation_candidates",
            "checklist_reconciliation_runs",
            "checklist_distribution_outbox",
            "checklist_migration_quarantine",
            "checklist_care_group_contexts",
            "checklist_context_authorities",
            "checklist_template_version_items",
            "checklist_template_recipient_roles",
            "checklist_substages");
    private static final List<String> RETIREMENT_LOCK_ORDER = List.of(
            "checklist_reconciliation_runs",
            "checklist_reconciliation_candidates",
            "checklist_distribution_outbox",
            "checklist_migration_quarantine",
            "checklist_care_group_contexts",
            "checklist_context_authorities",
            "checklist_template_version_items",
            "checklist_template_recipient_roles",
            "checklist_substages",
            "care_item_templates",
            "mother_journeys",
            "care_subjects",
            "care_groups",
            "checklist_instances",
            "checklist_task_instances",
            "checklist_action_commands");

    @Test
    void retirementDropsExactlyTheApprovedTablesInDependencyOrderWithoutCascade() throws Exception {
        String sql = Files.readString(RETIREMENT_MIGRATION);
        var matcher = DROP_TABLE.matcher(sql);
        List<String> droppedTables = new ArrayList<>();
        while (matcher.find()) {
            droppedTables.add(matcher.group(1));
        }

        assertThat(droppedTables).containsExactlyElementsOf(RETIRED_TABLES);
        var lockMatcher = LOCK_TABLE.matcher(sql);
        List<String> lockedTables = new ArrayList<>();
        while (lockMatcher.find()) {
            lockedTables.add(lockMatcher.group(1));
        }
        assertThat(lockedTables).containsExactlyElementsOf(RETIREMENT_LOCK_ORDER);
        assertThat(sql.indexOf("SET LOCAL lock_timeout = '30s'"))
                .isLessThan(sql.indexOf("LOCK TABLE public.checklist_reconciliation_runs"));
        assertThat(sql.indexOf("LOCK TABLE public.checklist_reconciliation_runs"))
                .isLessThan(sql.indexOf("LOCK TABLE public.checklist_reconciliation_candidates"));
        assertThat(sql.indexOf("LOCK TABLE public.checklist_reconciliation_runs"))
                .isLessThan(sql.indexOf("CHECKLIST_RETIREMENT_LEGAL_HOLD_PRESENT"));
        assertThat(sql.lastIndexOf("LOCK TABLE public.checklist_action_commands"))
                .isLessThan(sql.indexOf("CHECKLIST_RETIREMENT_OUTBOX_NOT_DRAINED"));
        String executableSql = sql.replaceAll("(?m)--.*$", "");
        assertThat(executableSql.toUpperCase(Locale.ROOT)).doesNotContain("CASCADE");
    }

    @Test
    void retirementRequiresDrainedMutableWorkAndUsesSafePermissionBooleanChecks() throws Exception {
        String sql = Files.readString(RETIREMENT_MIGRATION).replaceAll("\\s+", " ");

        assertThat(sql).contains(
                "WHERE processed_at IS NULL",
                "CHECKLIST_RETIREMENT_OUTBOX_NOT_DRAINED",
                "WHERE status = 'RUNNING'",
                "CHECKLIST_RETIREMENT_RECONCILIATION_RUN_ACTIVE",
                "WHERE outcome = 'PENDING'",
                "CHECKLIST_RETIREMENT_RECONCILIATION_CANDIDATE_PENDING",
                "WHERE NOT legal_hold) > 6",
                "CHECKLIST_RETIREMENT_QUARANTINE_LIMIT_EXCEEDED",
                "jsonb_typeof(member.permission_json->'CHECKLIST_VIEW') = 'boolean'",
                "member.permission_json->>'CHECKLIST_VIEW' = 'true'");
        assertThat(sql).doesNotContain("(member.permission_json->>'CHECKLIST_VIEW')::boolean");
    }

    @Test
    void retirementPreservesActionTargetRetentionLegalHoldDeleteAndPurgeGuards() throws Exception {
        String sql = Files.readString(RETIREMENT_MIGRATION);
        String normalized = sql.replaceAll("\\s+", " ");

        assertThat(normalized).contains(
                "CREATE OR REPLACE FUNCTION public.checklist_purge_retained_records",
                "DELETE FROM public.checklist_action_commands",
                "command.retain_until <= clock_timestamp()",
                "command.legal_hold = false",
                "CHECKLIST_RETIREMENT_ACTION_LEDGER_ONLY_V1",
                "DROP FUNCTION IF EXISTS public.checklist_assert_retention_security()",
                "trigger_entry.tgqual IS NULL");
        assertThat(sql.indexOf("CHECKLIST_RETIREMENT_ACTION_LEDGER_GUARD_MISSING"))
                .isLessThan(sql.indexOf("DROP CONSTRAINT checklist_instances_template_recipient_role_fk"));
        assertThat(normalized).doesNotContain(
                "DROP TRIGGER checklist_validate_action_command_target_trg",
                "DROP FUNCTION public.checklist_validate_action_command_target()",
                "DROP TRIGGER checklist_action_command_retention_guard_trg",
                "DROP FUNCTION public.checklist_action_command_retention_guard()",
                "DROP INDEX public.checklist_action_commands_retention_ix");
    }

    @Test
    void privilegedFinalizersLiveBesideMigrationsButCannotBeDiscoveredAsFlywayMigrations()
            throws Exception {
        assertThat(PRE_FINALIZER).exists().hasParent(MIGRATION_DIRECTORY);
        assertThat(POST_FINALIZER).exists().hasParent(MIGRATION_DIRECTORY);
        assertThat(PRE_FINALIZER.getFileName().toString())
                .doesNotStartWith("V")
                .doesNotStartWith("R");
        assertThat(POST_FINALIZER.getFileName().toString())
                .doesNotStartWith("V")
                .doesNotStartWith("R");

        String preFinalizer = Files.readString(PRE_FINALIZER);
        String postFinalizer = Files.readString(POST_FINALIZER);
        assertThat(preFinalizer).contains(
                "BEGIN;",
                "COMMIT;",
                "CHECKLIST_RETIREMENT_PRE_FINALIZER_COMPLETE",
                "CHECKLIST_RETIREMENT_ACTION_LEDGER_ONLY_V1",
                "REQUEST_100_PARITY_WRITERS_FROZEN_CATALOG_AND_LEDGER_CAPTURED_V1",
                "CHECKLIST_RETIREMENT_OPERATOR_ATTESTATION_REQUIRED",
                "ALTER TABLE public.checklist_migration_quarantine OWNER TO");
        assertThat(postFinalizer).contains(
                "BEGIN;",
                "COMMIT;",
                "CHECKLIST_RETIREMENT_POST_FINALIZER_COMPLETE",
                "CHECKLIST_RETIREMENT_ACTION_LEDGER_GUARD_MISSING",
                "checklist_action_command_retention_guard_trg",
                "public.checklist_action_command_retention_guard()",
                "checklist_validate_action_command_target_trg",
                "public.checklist_validate_action_command_target()",
                "trigger_entry.tgenabled = 'O'",
                "trigger_entry.tgqual IS NULL",
                "OWNER TO carebridge_checklist_retention_owner");
    }
}
