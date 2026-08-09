package com.carebridge.backend.testsupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Resolves a Flyway migration by its description instead of its version prefix.
 *
 * <p>The migration chain gets re-versioned whenever the canonical baseline is rebuilt, but a
 * migration keeps its {@code __description.sql} suffix. Contract tests that assert on the content
 * of a specific migration should therefore look it up by description; pinning the full filename
 * turns every re-version into a false failure.
 */
public final class MigrationLocator {

    private static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");

    private MigrationLocator() {
    }

    /** @throws IllegalStateException when the description matches zero or several migrations. */
    public static Path byDescription(String description) {
        String suffix = "__" + description + ".sql";
        try (Stream<Path> paths = Files.list(MIGRATION_DIRECTORY)) {
            List<Path> matches = paths
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .sorted()
                    .toList();
            if (matches.size() != 1) {
                throw new IllegalStateException(
                        "Expected exactly one migration ending in '" + suffix + "' but found " + matches);
            }
            return matches.getFirst();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
