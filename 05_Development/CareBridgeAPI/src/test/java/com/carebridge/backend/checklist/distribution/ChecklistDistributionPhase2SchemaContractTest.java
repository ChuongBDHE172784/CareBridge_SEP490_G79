package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** RED contracts for the Phase 2 database enforcement layer. */
class ChecklistDistributionPhase2SchemaContractTest {

    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/V20260729040000__add_checklist_template_authoring_v2_guards.sql");

    @Test
    void phase2MigrationExistsAndFreezesApprovedVersionContent() throws Exception {
        String sql = Files.readString(MIGRATION);
        assertThat(sql).contains(
                "CREATE OR REPLACE FUNCTION public.checklist_guard_approved_template_mutation",
                "CREATE TRIGGER checklist_guard_approved_template_mutation_trg",
                "VERSION_IMMUTABLE",
                "checklist_guard_approved_item_mutation");
    }

    @Test
    void approvedMutationGuardsCannotBeBypassedByChangingReviewStatusOrParentKeys() throws Exception {
        String sql = Files.readString(MIGRATION);
        assertThat(sql)
                .doesNotContain("OLD.migration_review_required = false")
                .contains(
                        "NEW.entry_type IS DISTINCT FROM OLD.entry_type",
                        "NEW.migration_review_required IS DISTINCT FROM OLD.migration_review_required",
                        "NEW.content_status NOT IN ('APPROVED', 'ARCHIVED')",
                        "OLD.content_status = 'ARCHIVED'",
                        "OLD.content_status = 'APPROVED'",
                        "NEW.content_status = 'PENDING_REVIEW'",
                        "NEW.migration_review_required = false",
                        "OLD.parent_template_id",
                        "NEW.parent_template_id",
                        "OLD.template_version_id",
                        "NEW.template_version_id");
        assertThat(sql).contains(
                "TG_OP = 'DELETE'",
                "BEFORE UPDATE OR DELETE ON public.care_item_templates",
                "checklist_guard_referenced_substage_mutation",
                "BEFORE UPDATE OR DELETE ON public.checklist_substages",
                "checklist_guard_version_item_authority_mutation",
                "BEFORE INSERT OR UPDATE OR DELETE ON public.checklist_template_version_items");
    }

    @Test
    void productionCatalogSeedsLifecycleAnchorsInsteadOfOnlyLegacyNoneRows() throws Exception {
        String foundation = Files.readString(Path.of(
                "src/test/resources/db/migration-legacy/V20260729030000__add_checklist_distribution_v2_foundation.sql"));
        assertThat(foundation).contains(
                "'PREGNANCY_LMP_WEEK_0_12'",
                "'LMP'",
                "'POSTPARTUM_DAY_0_7'",
                "'DELIVERY_DATE'",
                "'BABY_CARE_MONTH_0_3'",
                "'BIRTH_DATE'");
    }

    @Test
    void phase2MigrationRequiresRolesAndTargetsBeforeApproval() throws Exception {
        String sql = Files.readString(MIGRATION);
        assertThat(sql).contains(
                "checklist_validate_template_approval",
                "TEMPLATE_ROLE_REQUIRED",
                "ITEM_TARGET_REQUIRED",
                "checklist_validate_template_approval_trg");
        assertThat(sql).contains(
                "substage_anchor = 'NONE'",
                "ELSIF NEW.stage IS NOT NULL",
                "NEW.stage = 'PREGNANCY' AND substage_anchor IN ('LMP', 'EDD')",
                "NEW.stage = 'POSTPARTUM' AND substage_anchor = 'DELIVERY_DATE'",
                "NEW.stage = 'BABY_CARE' AND substage_anchor = 'BIRTH_DATE'");
    }

    @Test
    void phase2MigrationProvidesExplicitImportedReviewAndActivationStateGuards() throws Exception {
        String sql = Files.readString(MIGRATION);
        assertThat(sql).contains(
                "migration_review_required",
                "distribution_enabled",
                "MIGRATION_REVIEW_REQUIRED",
                "checklist_template_version_items",
                "OLD.migration_review_required = true",
                "care_item_templates_distribution_gate_ck",
                "care_item_templates_approved_gate_ck",
                "care_item_templates_import_activation_gate_ck",
                "Normalize imported approved roots into the correctable review state",
                "content_status = 'APPROVED'",
                "AND is_active = true");
        assertThat(sql).contains(
                "OR NEW.content_status = 'APPROVED'",
                "OLD.migration_reviewed_at IS NOT NULL",
                "NEW.migration_review_required = true",
                "OLD.migration_review_required = true AND NEW.migration_review_required = false",
                "allowed_review_activation",
                "allowed_review_archive",
                "Activation and archive may change state only; reviewed content identity must remain exact",
                "NEW.title IS NOT DISTINCT FROM OLD.title");
    }

    @Test
    void consumerRepositoriesExposeOnlyReviewedDistributionEnabledVersions() throws Exception {
        String templateRepository = Files.readString(Path.of(
                "src/main/java/com/carebridge/backend/content/repository/ChecklistTemplateRepository.java"));
        String itemRepository = Files.readString(Path.of(
                "src/main/java/com/carebridge/backend/content/repository/ChecklistItemRepository.java"));
        assertThat(templateRepository).contains(
                "findAllDistributionEnabled",
                "t.distributionEnabled=true",
                "t.migrationReviewRequired=false");
        assertThat(itemRepository).contains(
                "t.distributionEnabled=true",
                "t.migrationReviewRequired=false");
    }

    @Test
    void lineageEndpointsResolvePublicVersionIdInsteadOfEntityPrimaryKey() throws Exception {
        String repository = Files.readString(Path.of(
                "src/main/java/com/carebridge/backend/content/repository/ChecklistTemplateRepository.java"));
        String authoringService = Files.readString(Path.of(
                "src/main/java/com/carebridge/backend/content/service/AdminChecklistTemplateServiceImpl.java"));
        String approvalService = Files.readString(Path.of(
                "src/main/java/com/carebridge/backend/content/service/ChecklistTemplateApprovalServiceImpl.java"));
        assertThat(repository).contains("findByTemplateVersionId");
        assertThat(authoringService).contains("findByTemplateVersionId");
        assertThat(authoringService).contains("acquireLineageLock");
        assertThat(approvalService).contains("findByTemplateVersionId");
    }
}
