package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.checklist.entity.ChecklistActionCommand;
import jakarta.persistence.Column;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** RED contracts derived from the independent Phase 1 acceptance and edge-case reviews. */
class ChecklistDistributionHardeningContractTest {

    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/V20260729030000__add_checklist_distribution_v2_foundation.sql");

    @Test
    void migrationQuarantinesMalformedLegacyEntriesBeforeStrictTargetConstraint() throws Exception {
        String sql = Files.readString(MIGRATION);

        int quarantineTable = sql.indexOf("CREATE TABLE public.checklist_migration_quarantine");
        int quarantineInsert = sql.indexOf("INSERT INTO public.checklist_migration_quarantine");
        int targetBackfill = sql.indexOf("SET target_subject = CASE");
        assertThat(quarantineTable).isGreaterThanOrEqualTo(0);
        assertThat(quarantineInsert).isGreaterThan(quarantineTable).isLessThan(targetBackfill);
        assertThat(sql).contains(
                "DROP CONSTRAINT care_item_templates_type_ck",
                "'QUARANTINED_CHECKLIST_ENTRY'",
                "root.entry_type <> 'TEMPLATE_ROOT'",
                "root.stage NOT IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE')",
                "entry_type = 'QUARANTINED_CHECKLIST_ENTRY'",
                "target_subject IS NOT NULL AND target_subject IN ('MOTHER','BABY')");
    }

    @Test
    void migrationSeedsCatchAllSubstagesAndPreservesExistingPermissionGrants() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "LEGACY_PRE_PREGNANCY",
                "LEGACY_PREGNANCY",
                "LEGACY_POSTPARTUM",
                "LEGACY_BABY_CARE",
                "SET substage_id = substage.substage_id",
                "jsonb_typeof(base_permission->'CHECKLIST_VIEW') = 'boolean'",
                "jsonb_typeof(base_permission->'CHECKLIST_COMPLETE') = 'boolean'");
    }

    @Test
    void migrationAddsCanonicalContextRecipientLineageAndItemAuthorities() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "CREATE TABLE public.checklist_context_authorities",
                "REFERENCES public.mother_journeys(journey_id, owner_user_id)",
                "REFERENCES public.care_subjects(care_subject_id, owner_user_id, subject_type)",
                "care_group_members_checklist_auth_ix",
                "checklist_instances_mother_recipient_ck",
                "checklist_instances_template_recipient_role_fk",
                "checklist_validate_instance_recipient",
                "checklist_sync_context_authority",
                "checklist_sync_template_version_item",
                "care_item_templates_lineage_version_uk",
                "checklist_instances_lineage_version_fk",
                "CREATE TABLE public.checklist_template_version_items",
                "REFERENCES public.care_item_templates(parent_template_id, template_id, entry_type)",
                "checklist_task_instances_parent_version_fk",
                "checklist_task_instances_version_item_fk");
    }

    @Test
    void migrationUsesNonUniqueCorrelationIndexesAndDefaultDenyQuarantineAccess() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).doesNotContain(
                "checklist_distribution_outbox_correlation_uk UNIQUE",
                "checklist_migration_quarantine_correlation_uk UNIQUE");
        assertThat(sql).contains(
                "CREATE INDEX checklist_distribution_outbox_correlation_ix",
                "CREATE INDEX checklist_migration_quarantine_correlation_ix",
                "ALTER TABLE public.checklist_migration_quarantine ENABLE ROW LEVEL SECURITY",
                "ALTER TABLE public.checklist_migration_quarantine FORCE ROW LEVEL SECURITY");
        assertThat(sql).contains(
                "checklist_migration_quarantine_operations_select",
                "checklist_migration_quarantine_operations_update");
    }

    @Test
    void migrationConstrainsCommandsAndTypedChecklistAuditColumns() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "checklist_action_commands_task_kind_ck",
                "checklist_action_commands_action_ck",
                "checklist_action_commands_result_ck",
                "audit_events_checklist_actor_type_ck",
                "audit_events_checklist_actor_shape_ck",
                "audit_events_checklist_correlation_ck",
                "audit_events_checklist_context_type_ck",
                "audit_events_checklist_context_pair_ck",
                "audit_events_checklist_subject_ck",
                "audit_events_checklist_task_ck",
                "audit_events_checklist_reason_code_ck");
        assertThat(sql).contains("event_category NOT IN (").doesNotContain("event_category NOT LIKE 'CHECKLIST_%' OR actor_type");
    }

    @Test
    void retiredPersistenceSurfaceIsAbsentWhileCoreTaskMappingRemains() throws Exception {
        assertFields("ChecklistTaskInstance", "templateVersionId");
        assertThatThrownBy(() -> Class.forName(
                "com.carebridge.backend.checklist.entity.ChecklistTemplateRecipientRole"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                "com.carebridge.backend.checklist.repository.ChecklistDistributionOutboxRepository"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void entityNullabilityAndBuilderDefaultsMatchDatabaseContract() throws Exception {
        Field targetSubject = Class.forName("com.carebridge.backend.content.entity.ChecklistItem")
                .getDeclaredField("targetSubject");
        assertThat(targetSubject.getAnnotation(Column.class).nullable()).isFalse();

        ChecklistActionCommand command = ChecklistActionCommand.builder().build();
        assertThat(command.getResultJson()).isEqualTo("{}");
        assertThat(command.getAppliedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
        assertThatThrownBy(() -> Class.forName(
                "com.carebridge.backend.checklist.entity.ChecklistMigrationQuarantine"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void normativeEnumsAndChecklistAuditActionsAreEnforced() throws Exception {
        for (String enumName : List.of(
                "ChecklistTemplateReviewStatus",
                "ChecklistAnchorType",
                "ChecklistRangeUnit")) {
            assertThat(Class.forName("com.carebridge.backend.checklist.model." + enumName).isEnum()).isTrue();
        }

        AuditEligibilityPolicy policy = new AuditEligibilityPolicy();
        for (String actionName : List.of(
                "CHECKLIST_DISTRIBUTED",
                "CHECKLIST_ASSIGNED",
                "CHECKLIST_COMPLETED",
                "CHECKLIST_SKIPPED",
                "CHECKLIST_CANCELLED",
                "CHECKLIST_RECONCILIATION_FAILED",
                "CHECKLIST_MIGRATION_QUARANTINED")) {
            AuditAction action = AuditAction.valueOf(actionName);
            assertThat(policy.shouldAudit(action)).as(actionName).isTrue();
        }
        assertThat(Class.forName("com.carebridge.backend.checklist.audit.ChecklistAuditWriter")).isNotNull();

        Class<?> request = Class.forName("com.carebridge.backend.family.dto.UpdateFamilyPermissionRequest");
        assertThat(request.getDeclaredField("checklistView")).isNotNull();
        assertThat(request.getDeclaredField("checklistComplete")).isNotNull();
        assertThat(Class.forName("com.carebridge.backend.content.dto.request.ChecklistItemRequest")
                .getDeclaredField("targetSubject")).isNotNull();
    }

    private static void assertFields(String simpleName, String... fieldNames) throws Exception {
        Class<?> type = Class.forName("com.carebridge.backend.checklist.entity." + simpleName);
        for (String fieldName : fieldNames) {
            assertThat(type.getDeclaredField(fieldName)).as(simpleName + "." + fieldName).isNotNull();
        }
    }
}
