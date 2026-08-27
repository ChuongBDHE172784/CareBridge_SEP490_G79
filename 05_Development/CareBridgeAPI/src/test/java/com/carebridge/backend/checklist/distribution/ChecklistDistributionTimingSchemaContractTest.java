package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChecklistDistributionTimingSchemaContractTest {

    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/"
                    + "V20260729050000__add_checklist_distribution_timing_and_reconciliation_guards.sql");

    @Test
    void migrationAddsVersionedItemTimingAndDurableRetryGuards() throws IOException {
        assertThat(MIGRATION).exists();
        String sql = Files.readString(MIGRATION);
        assertThat(sql).contains("due_anchor_type", "due_offset_start", "due_offset_end", "due_offset_unit");
        assertThat(sql).contains("checklist_distribution_outbox_event_uk");
        assertThat(sql).contains("checklist_distribution_outbox_retry_ck");
        assertThat(sql).contains("checklist_reconciliation_success_watermark_ix");
    }
}
