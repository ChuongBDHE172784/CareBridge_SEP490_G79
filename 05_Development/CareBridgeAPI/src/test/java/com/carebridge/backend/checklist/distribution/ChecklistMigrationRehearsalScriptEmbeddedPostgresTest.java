package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Real PostgreSQL syntax/RLS evidence for the external read-only rehearsal queries. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistMigrationRehearsalScriptEmbeddedPostgresTest {

    private static final Path SCRIPT = Path.of(System.getProperty(
            "checklist.rehearsal.script",
            Path.of("..", "Deployment", "database", "Invoke-ChecklistMigrationRehearsal.ps1").toString()));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void preAndPostQueriesExecuteUnderTheOperationsRlsRole() throws Exception {
        String script = Files.readString(SCRIPT);
        String preSql = extractHereString(script, "preSql");
        String postSql = extractHereString(script, "postSql");

        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            // The external rehearsal SQL is a historical staged-chain verifier.
            // CapturePre stops before backfill, then the staged chain advances
            // only through the last pre-retirement version so its quarantine and
            // context visibility assertions remain meaningful. The new canonical
            // migration retires those support tables atomically and is covered by
            // the clean-chain PostgreSQL 18 tests.
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-legacy")
                    .target("20260729060000")
                    .load()
                    .migrate();
            grantVerifierReads(dataSource.getConnection());

            JsonNode pre = executeAsOperations(dataSource.getConnection(), preSql);
            assertThat(pre.path("flywayVersion").asText()).isEqualTo("20260729060000");
            assertVisibilityAttestation(pre);

            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-legacy")
                    .target("20260730050000")
                    .load()
                    .migrate();

            JsonNode post = executeAsOperations(dataSource.getConnection(), postSql);
            assertThat(post.path("flywayVersion").asText()).isEqualTo("20260730050000");
            assertThat(post.path("expectedTargetSha256").asText())
                    .isEqualTo(post.path("actualTargetSha256").asText());
            assertThat(post.path("legacyQuarantineRateDefined").asBoolean()).isTrue();
            assertVisibilityAttestation(post);
        }
    }

    private static String extractHereString(String script, String variable) {
        String marker = "$" + variable + " = @'";
        int start = script.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        start += marker.length();
        int end = script.indexOf("\n'@", start);
        assertThat(end).isGreaterThan(start);
        return script.substring(start, end).trim();
    }

    private static void grantVerifierReads(Connection connection) throws Exception {
        try (connection; var statement = connection.createStatement()) {
            statement.execute("""
                    GRANT SELECT ON public.flyway_schema_history,
                        public.preparation_checklist_items,
                        public.care_item_templates,
                        public.checklist_care_group_contexts,
                        public.checklist_instances,
                        public.checklist_task_instances,
                        public.checklist_migration_quarantine
                    TO checklist_operations
                    """);
        }
    }

    private JsonNode executeAsOperations(Connection connection, String sql) throws Exception {
        String json = null;
        try (connection; var statement = connection.createStatement()) {
            statement.execute("SET SESSION AUTHORIZATION checklist_operations");
            for (String segment : sql.split(";\\s*")) {
                if (segment.isBlank()) {
                    continue;
                }
                boolean hasResult = statement.execute(segment);
                if (hasResult) {
                    try (var result = statement.getResultSet()) {
                        if (result.next()) {
                            json = result.getString(1);
                        }
                    }
                }
            }
        }
        assertThat(json).isNotBlank();
        return objectMapper.readTree(json);
    }

    private static void assertVisibilityAttestation(JsonNode result) {
        assertThat(result.path("quarantineVisibilityRoleAuthorized").asBoolean()).isTrue();
        assertThat(result.path("quarantineVisibilitySessionRoleAuthorized").asBoolean()).isTrue();
        assertThat(result.path("quarantineRoleNonBypassVerified").asBoolean()).isTrue();
        assertThat(result.path("quarantineSelectGranted").asBoolean()).isTrue();
        assertThat(result.path("quarantineRlsPolicyVerified").asBoolean()).isTrue();
        assertThat(result.path("quarantineForceRlsVerified").asBoolean()).isTrue();
    }
}
