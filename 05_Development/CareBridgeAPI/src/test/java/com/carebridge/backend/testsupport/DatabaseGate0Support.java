package com.carebridge.backend.testsupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import org.flywaydb.core.api.MigrationVersion;

/**
/**
 * Test-only support for database Gate 0 checks on the append-only Flyway
 * migration chain. The repository must contain valid, uniquely versioned SQL
 * migrations that can build the canonical schema from an empty database.
 */
final class DatabaseGate0Support {

    static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");

    static final String DUPLICATE_VERSION = "REPOSITORY_DUPLICATE_VERSION";
    static final String MALFORMED_FILENAME = "REPOSITORY_MALFORMED_FILENAME";
    static final String EMPTY_REPOSITORY = "REPOSITORY_MIGRATIONS_EMPTY";

    private static final Pattern MIGRATION_PATTERN =
            Pattern.compile("^([BV])(.+)__(.+)\\.sql$");

    private DatabaseGate0Support() {
    }

    static RepositoryManifest inspectRepository() {
        return inspectRepository(MIGRATION_DIRECTORY);
    }

    static RepositoryManifest inspectRepository(Path migrationDirectory) {
        List<MigrationFile> migrations = new ArrayList<>();
        List<String> malformed = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(migrationDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> inspectMigration(
                            migrationDirectory, path, migrations, malformed, failures));
        } catch (IOException exception) {
            failures.add("REPOSITORY_READ_ERROR:" + exception.getClass().getSimpleName());
        }

        if (migrations.isEmpty()) {
            failures.add(EMPTY_REPOSITORY);
        }

        migrations.sort(Comparator
                .comparing((MigrationFile migration) ->
                        MigrationVersion.fromVersion(migration.version()))
                .thenComparing(MigrationFile::type)
                .thenComparing(MigrationFile::script));

        Map<String, List<String>> pathsByTypeAndVersion = new TreeMap<>();
        for (MigrationFile migration : migrations) {
            pathsByTypeAndVersion.computeIfAbsent(
                            migration.type() + ":" + migration.version(),
                            ignored -> new ArrayList<>())
                    .add(migration.path());
        }
        Map<String, List<String>> duplicateVersions = new TreeMap<>();
        pathsByTypeAndVersion.forEach((typeAndVersion, scripts) -> {
            if (scripts.size() > 1) {
                List<String> sortedPaths = scripts.stream().sorted().toList();
                duplicateVersions.put(typeAndVersion, sortedPaths);
                failures.add(DUPLICATE_VERSION + ":" + typeAndVersion + ":"
                        + String.join(",", sortedPaths));
            }
        });
        malformed.stream().sorted()
                .forEach(path -> failures.add(MALFORMED_FILENAME + ":" + path));

        return new RepositoryManifest(
                List.copyOf(migrations),
                List.copyOf(malformed),
                Map.copyOf(duplicateVersions),
                List.copyOf(failures));
    }

    private static void inspectMigration(
            Path migrationDirectory,
            Path path,
            List<MigrationFile> migrations,
            List<String> malformed,
            List<String> failures) {
        String filename = path.getFileName().toString();
        String script = normalizePath(migrationDirectory.relativize(path));
        String relativePath = normalizePath(path);
        Matcher matcher = MIGRATION_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            malformed.add(relativePath);
            return;
        }
        try {
            migrations.add(new MigrationFile(
                    matcher.group(1),
                    canonicalVersion(matcher.group(2)),
                    matcher.group(3),
                    script,
                    relativePath,
                    flywayChecksum(path)));
        } catch (IllegalArgumentException exception) {
            malformed.add(relativePath);
        } catch (IOException exception) {
            failures.add("REPOSITORY_CHECKSUM_ERROR:" + relativePath + ":"
                    + exception.getClass().getSimpleName());
        }
    }

    static String canonicalVersion(String rawVersion) {
        return MigrationVersion.fromVersion(rawVersion).getVersion();
    }

    /** Mirrors Flyway's CRC32-of-lines checksum so history rows can be reconciled. */
    static int flywayChecksum(Path path) throws IOException {
        CRC32 crc32 = new CRC32();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            boolean firstLine = true;
            while (line != null) {
                if (firstLine && !line.isEmpty() && line.charAt(0) == '\ufeff') {
                    line = line.substring(1);
                }
                crc32.update(line.getBytes(StandardCharsets.UTF_8));
                firstLine = false;
                line = reader.readLine();
            }
        }
        return (int) crc32.getValue();
    }

    private static String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    record MigrationFile(
            String type,
            String version,
            String description,
            String script,
            String path,
            int flywayChecksum) {
    }

    record RepositoryManifest(
            List<MigrationFile> migrations,
            List<String> malformedFiles,
            Map<String, List<String>> duplicateVersions,
            List<String> gateFailures) {
    }
}
