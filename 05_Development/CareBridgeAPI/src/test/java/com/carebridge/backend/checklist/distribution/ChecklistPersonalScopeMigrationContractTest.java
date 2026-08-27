package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ChecklistPersonalScopeMigrationContractTest {

    private static final Path BASE_MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/"
                    + "V20260730070000__allow_personal_checklist_instances_without_care_groups.sql");
    private static final Path FOLLOW_UP_MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/"
                    + "V20260730235900__allow_personal_checklist_instances_without_care_groups.sql");

    @Test
    void migrationMakesGroupOptionalButKeepsCanonicalContextAuthority() throws Exception {
        String sql = sql();

        assertThat(sql).contains(
                "ALTER COLUMN care_group_id DROP NOT NULL",
                "checklist_instances_personal_context_authority_fk",
                "FOREIGN KEY (care_context_type, care_context_id, context_owner_user_id)",
                "REFERENCES public.checklist_context_authorities",
                "(care_context_type, care_context_id, owner_user_id)",
                "ON DELETE RESTRICT",
                "DROP CONSTRAINT IF EXISTS checklist_reconciliation_candidates_context_ck",
                "recipient_user_id IS NOT NULL");
    }

    @Test
    void migrationRequiresFamilyScopeAndRevalidatesRecipientChanges() throws Exception {
        String sql = sql();

        assertThat(sql).contains(
                "checklist_instances_family_group_scope_ck",
                "CHECK (recipient_role <> 'FAMILY' OR care_group_id IS NOT NULL)",
                "DROP TRIGGER IF EXISTS checklist_validate_instance_recipient_trg",
                "BEFORE INSERT OR UPDATE OF recipient_role, recipient_user_id, care_group_id",
                "EXECUTE FUNCTION public.checklist_validate_instance_recipient()");
    }

    @Test
    void migrationIsForwardOnlyAndDoesNotRewriteHistoricalKeys() throws Exception {
        String sql = sql()
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        assertThat(sql).doesNotContain(
                "DROP TABLE",
                "DROP COLUMN",
                "TRUNCATE",
                "DELETE FROM",
                "UPDATE PUBLIC.CHECKLIST_INSTANCES",
                "UPDATE CHECKLIST_INSTANCES",
                "SET DISTRIBUTION_KEY");
    }

    private static String sql() throws Exception {
        return Files.readString(BASE_MIGRATION) + "\n" + Files.readString(FOLLOW_UP_MIGRATION);
    }
}
