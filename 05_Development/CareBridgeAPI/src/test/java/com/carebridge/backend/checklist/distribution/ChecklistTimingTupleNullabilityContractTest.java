package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ChecklistTimingTupleNullabilityContractTest {

    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/"
                    + "V20260729050000__add_checklist_distribution_timing_and_reconciliation_guards.sql");

    @Test
    void dueTimingConstraintUsesExplicitAllNullOrAllNotNullTupleBranches() throws IOException {
        String sql = Files.readString(MIGRATION).toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");

        assertThat(sql).contains(
                "DUE_ANCHOR_TYPE IS NULL",
                "DUE_OFFSET_START IS NULL",
                "DUE_OFFSET_END IS NULL",
                "DUE_OFFSET_UNIT IS NULL",
                "DUE_ANCHOR_TYPE IS NOT NULL",
                "DUE_OFFSET_START IS NOT NULL",
                "DUE_OFFSET_END IS NOT NULL",
                "DUE_OFFSET_UNIT IS NOT NULL");
    }
}
