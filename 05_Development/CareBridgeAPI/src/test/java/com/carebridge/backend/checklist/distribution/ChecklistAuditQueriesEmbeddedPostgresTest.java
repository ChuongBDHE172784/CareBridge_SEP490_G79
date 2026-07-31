package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** PostgreSQL syntax evidence for the read-only checklist retirement audit. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistAuditQueriesEmbeddedPostgresTest {

    private static final Path AUDIT_QUERIES = Path.of(
            "..",
            "..",
            "_bmad-output",
            "planning-artifacts",
            "architecture",
            "architecture-CareBridge_SEP490_G79-2026-07-31",
            "AUDIT-QUERIES.sql");

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void completeAuditExecutesAgainstCurrentPostgresSchema() throws Exception {
        assertThat(AUDIT_QUERIES).exists();
        String auditSql = Files.readString(AUDIT_QUERIES);

        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            try (var connection = dataSource.getConnection();
                    var statement = connection.createStatement()) {
                statement.execute(auditSql);
                assertThat(connection.getAutoCommit()).isTrue();
                assertThat(connection.isClosed()).isFalse();
            }
        }
    }
}
