package com.carebridge.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfSystemProperty(named = "gate0.enabled", matches = "true")
class FlywayMigrationChainTest {

    private static final String BASELINE_SCRIPT =
            "B20260724111500__canonical_70_table_baseline.sql";

    @TempDir
    Path temporaryDirectory;

    @Test
    void repositoryMigrationChainHasNoGateFailures() throws Exception {
        var manifest = DatabaseGate0Support.inspectRepository();
        Path manifestPath = DatabaseGate0Support.writeManifest("repository-manifest.json", manifest);

        assertThat(manifestPath).exists();
        assertThat(manifest.gateFailures())
                .as("Gate 0 repository failures: %s", manifest.gateFailures())
                .isEmpty();
        assertThat(manifest.migrations())
                .extracting(DatabaseGate0Support.MigrationFile::script)
                .contains(BASELINE_SCRIPT, "V20260724111500__remove_legacy_expert_profile_columns.sql");
        assertThat(manifest.duplicateVersions()).isEmpty();
    }

    @Test
    void duplicateChecksRemainStrictWithinEachMigrationType() throws Exception {
        Files.writeString(temporaryDirectory.resolve("B1__baseline.sql"), "SELECT 1;");
        Files.writeString(temporaryDirectory.resolve("V1__versioned.sql"), "SELECT 1;");

        var validPair = DatabaseGate0Support.inspectRepository(temporaryDirectory);
        assertThat(validPair.gateFailures()).isEmpty();

        Files.writeString(temporaryDirectory.resolve("V1__duplicate.sql"), "SELECT 1;");
        var duplicateVersioned = DatabaseGate0Support.inspectRepository(temporaryDirectory);
        assertThat(duplicateVersioned.gateFailures())
                .anyMatch(failure -> failure.startsWith(
                        DatabaseGate0Support.DUPLICATE_VERSION + ":V:1:"));
    }
}
