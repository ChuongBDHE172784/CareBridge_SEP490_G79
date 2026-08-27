package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ChecklistSchemaSimplificationMigrationContractTest {

    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/"
                    + "V20260731020000__prepare_checklist_schema_simplification.sql");
    private static final Path STRICT_PROFILE =
            Path.of("src/main/resources/application-checklist-retirement.yaml");

    @Test
    void migrationAddsAndBackfillsInlineTemplateAssignmentMetadata() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "ADD COLUMN recipient_scope",
                "ADD COLUMN eligibility_anchor_type",
                "ADD COLUMN eligibility_range_unit",
                "ADD COLUMN eligibility_start_inclusive",
                "ADD COLUMN eligibility_end_inclusive",
                "checklist_template_recipient_roles",
                "checklist_substages",
                "UPDATE public.care_item_templates");
    }

    @Test
    void migrationKeepsLegacyRoleAndSubstageWritesCompatible() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "CREATE CONSTRAINT TRIGGER checklist_sync_inline_recipient_scope_trg",
                "DEFERRABLE INITIALLY DEFERRED",
                "EXECUTE FUNCTION public.checklist_sync_inline_recipient_scope()",
                "OLD.template_version_id IS DISTINCT FROM NEW.template_version_id",
                "WHERE template_version_id = OLD.template_version_id",
                "CREATE TRIGGER checklist_sync_inline_eligibility_trg",
                "EXECUTE FUNCTION public.checklist_sync_inline_eligibility()",
                "CREATE TRIGGER checklist_sync_inline_eligibility_from_substage_trg",
                "AFTER UPDATE OF stage, anchor_type, range_unit, start_inclusive, end_inclusive",
                "EXECUTE FUNCTION public.checklist_sync_inline_eligibility_from_substage()");
    }

    @Test
    void migrationValidatesTargetShapeWithoutRetiringTables() throws Exception {
        String normalized = Files.readString(MIGRATION)
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        assertThat(normalized).contains(
                "RECIPIENT_SCOPE IN ('MOTHER', 'FAMILY', 'BOTH')",
                "ELIGIBILITY_ANCHOR_TYPE IN ( 'NONE', 'LMP', 'EDD', 'DELIVERY_DATE', 'BIRTH_DATE' )",
                "ELIGIBILITY_RANGE_UNIT IN ('DAY', 'WEEK', 'MONTH')",
                "CREATE CONSTRAINT TRIGGER CHECKLIST_VALIDATE_INLINE_TEMPLATE_SHAPE_TRG",
                "DEFERRABLE INITIALLY DEFERRED",
                "EXECUTE FUNCTION PUBLIC.CHECKLIST_VALIDATE_INLINE_TEMPLATE_SHAPE()",
                "INLINE_TEMPLATE_METADATA_MISMATCH",
                "CURRENT_ROOT.MIGRATION_REVIEW_REQUIRED = TRUE",
                "CURRENT_ROOT.ELIGIBILITY_END_INCLUSIVE = 2147483647",
                "CONTENT_STATUS, DISTRIBUTION_ENABLED, MIGRATION_REVIEW_REQUIRED",
                "CURRENT_ROOT.ELIGIBILITY_START_INCLUSIVE IS DISTINCT FROM EXPECTED_START",
                "CURRENT_ROOT.ELIGIBILITY_END_INCLUSIVE IS DISTINCT FROM EXPECTED_END");
        assertThat(normalized).doesNotContain("DROP TABLE", " CASCADE", "TRUNCATE");
        assertThat(normalized).doesNotContain(
                "ADD CONSTRAINT CARE_ITEM_TEMPLATES_INLINE_SHAPE_CK",
                "VALIDATE CONSTRAINT CARE_ITEM_TEMPLATES_INLINE_SHAPE_CK");
    }

    @Test
    void retirementProfileEnforcesStrictFlywayValidation() throws Exception {
        String yaml = Files.readString(STRICT_PROFILE);

        assertThat(yaml).contains(
                "on-profile: checklist-retirement",
                "clean-disabled: true",
                "out-of-order: false",
                "validate-on-migrate: true",
                "ignore-migration-patterns: \"*:future\"");
        assertThat(yaml).doesNotContain("checksum-mismatch", "missing");
    }
}
