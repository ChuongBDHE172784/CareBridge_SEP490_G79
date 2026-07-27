package com.carebridge.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Static repository contract for the one-migration convergence: the migration
 * directory contains exactly one versioned migration, the canonical
 * V20260727010000 convergence script, with a valid filename and no duplicates.
 */
class FlywayMigrationChainTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void repositoryContainsExactlyTheCanonicalConvergenceMigration() {
        var manifest = DatabaseGate0Support.inspectRepository();

        assertThat(manifest.gateFailures())
                .as("Gate 0 repository failures: %s", manifest.gateFailures())
                .isEmpty();
        assertThat(manifest.malformedFiles()).isEmpty();
        assertThat(manifest.duplicateVersions()).isEmpty();
        assertThat(manifest.migrations())
                .hasSize(1)
                .first()
                .satisfies(migration -> {
                    assertThat(migration.type()).isEqualTo("V");
                    assertThat(migration.version())
                            .isEqualTo(DatabaseGate0Support.canonicalVersion(
                                    DatabaseGate0Support.CANONICAL_VERSION));
                    assertThat(migration.script())
                            .isEqualTo(DatabaseGate0Support.CANONICAL_SCRIPT);
                });
    }

    @Test
    void duplicateVersionsAndMalformedFilenamesAreStillRejected() throws Exception {
        Files.writeString(
                temporaryDirectory.resolve("V20260727010000__canonical.sql"), "SELECT 1;");

        var single = DatabaseGate0Support.inspectRepository(temporaryDirectory);
        assertThat(single.gateFailures()).isEmpty();

        Files.writeString(
                temporaryDirectory.resolve("V20260727010000__duplicate.sql"), "SELECT 1;");
        var duplicate = DatabaseGate0Support.inspectRepository(temporaryDirectory);
        assertThat(duplicate.gateFailures())
                .anyMatch(failure -> failure.startsWith(
                        DatabaseGate0Support.DUPLICATE_VERSION + ":V:20260727010000:"));

        Files.writeString(temporaryDirectory.resolve("V_missing_version.sql"), "SELECT 1;");
        var malformed = DatabaseGate0Support.inspectRepository(temporaryDirectory);
        assertThat(malformed.gateFailures())
                .anyMatch(failure -> failure.startsWith(DatabaseGate0Support.MALFORMED_FILENAME + ":"));
    }

    @Test
    void emptyMigrationDirectoryIsRejected() throws Exception {
        Path empty = temporaryDirectory.resolve("empty");
        Files.createDirectories(empty);

        var manifest = DatabaseGate0Support.inspectRepository(empty);

        assertThat(manifest.gateFailures())
                .contains(DatabaseGate0Support.EMPTY_REPOSITORY);
    }
}
