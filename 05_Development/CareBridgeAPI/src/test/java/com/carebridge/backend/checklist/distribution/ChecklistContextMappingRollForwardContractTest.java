package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChecklistContextMappingRollForwardContractTest {

    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/"
                    + "V20260729120000__sync_reviewed_care_group_contexts.sql");

    @Test
    void explicitLinksMaterializeOnlyCanonicalOwnerVerifiedContexts() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "CREATE OR REPLACE FUNCTION public.checklist_sync_reviewed_care_group_contexts",
                "CREATE TRIGGER checklist_sync_reviewed_care_group_contexts_trg",
                "authority.owner_user_id = NEW.owner_user_id",
                "authority.owner_user_id = group_row.owner_user_id",
                "'REVIEWED', false",
                "ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING");
        assertThat(sql).doesNotContain("UPDATE public.checklist_care_group_contexts");
    }
}
