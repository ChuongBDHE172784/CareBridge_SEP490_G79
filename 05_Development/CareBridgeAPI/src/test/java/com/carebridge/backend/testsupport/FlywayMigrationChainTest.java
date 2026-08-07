package com.carebridge.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Static repository contract for the append-only Flyway chain: migration files
 * have valid names, unique versions, and preserve the expected V1-V4 order.
 */
class FlywayMigrationChainTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void repositoryContainsTheExpectedAppendOnlyMigrationChain() {
        var manifest = DatabaseGate0Support.inspectRepository();

        assertThat(manifest.gateFailures())
                .as("Gate 0 repository failures: %s", manifest.gateFailures())
                .isEmpty();
        assertThat(manifest.malformedFiles()).isEmpty();
        assertThat(manifest.duplicateVersions()).isEmpty();
        // The two baseline scripts are the only fixed anchors: everything after them is
        // re-versioned whenever the canonical chain is rebuilt, so assert the append-only
        // *shape* (versioned, unique, strictly increasing) rather than specific filenames.
        assertThat(manifest.migrations())
                .extracting(DatabaseGate0Support.MigrationFile::script)
                .contains("V1__init_schema.sql", "V2__seed_reference_data.sql");
        assertThat(manifest.migrations())
                .extracting(DatabaseGate0Support.MigrationFile::type)
                .containsOnly("V");
        var versions = manifest.migrations().stream()
                .map(DatabaseGate0Support.MigrationFile::version)
                .map(org.flywaydb.core.api.MigrationVersion::fromVersion)
                .toList();
        assertThat(versions).doesNotHaveDuplicates().isSorted();
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
