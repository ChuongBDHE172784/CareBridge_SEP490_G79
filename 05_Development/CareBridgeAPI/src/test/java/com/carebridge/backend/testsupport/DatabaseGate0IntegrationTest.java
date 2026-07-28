package com.carebridge.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Clean-bootstrap contract for the current forward-only Flyway chain.
 */
@Testcontainers(disabledWithoutDocker = true)
class DatabaseGate0IntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE);

    @Test
    void cleanBootstrapAppliesCurrentMigrationChainOnce() throws Exception {
        var repository = DatabaseGate0Support.inspectRepository();
        assertThat(repository.gateFailures())
                .as("Gate 0 repository failure codes")
                .isEmpty();
        assertThat(repository.migrations()).hasSize(3);

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        var firstRun = flyway.migrate();
        assertThat(firstRun.success).isTrue();
        assertThat(firstRun.migrationsExecuted).isEqualTo(repository.migrations().size());

        var secondRun = flyway.migrate();
        assertThat(secondRun.success).isTrue();
        assertThat(secondRun.migrationsExecuted).isZero();
        assertThat(flyway.info().pending()).isEmpty();

        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            try (var statement = connection.createStatement();
                 var history = statement.executeQuery("""
                         SELECT version, script, checksum, success
                           FROM flyway_schema_history
                          WHERE version IS NOT NULL AND type = 'SQL'
                          ORDER BY installed_rank
                         """)) {
                for (var expected : repository.migrations()) {
                    assertThat(history.next()).isTrue();
                    assertThat(DatabaseGate0Support.canonicalVersion(history.getString("version")))
                            .isEqualTo(expected.version());
                    assertThat(history.getString("script")).isEqualTo(expected.script());
                    assertThat((Integer) history.getObject("checksum"))
                            .isEqualTo(expected.flywayChecksum());
                    assertThat(history.getBoolean("success")).isTrue();
                }
                assertThat(history.next()).isFalse();
            }

            try (var statement = connection.createStatement();
                 var structure = statement.executeQuery("""
                         SELECT to_regclass('public.users') IS NOT NULL
                                AND to_regclass('public.roles') IS NULL
                                AND to_regclass('public.user_roles') IS NULL,
                                to_regclass('public.triage_sessions') IS NOT NULL
                                AND to_regclass('public.triage_session_evidence') IS NOT NULL
                                AND to_regclass('public.intake_sessions') IS NULL
                                AND to_regclass('public.structured_intake_data') IS NULL
                                AND to_regclass('public.triage_answers') IS NULL
                                AND to_regclass('public.triage_assessments') IS NULL
                         """)) {
                assertThat(structure.next()).isTrue();
                assertThat(structure.getBoolean(1))
                        .as("Clean bootstrap must retain only users.role as role persistence")
                        .isTrue();
                assertThat(structure.getBoolean(2))
                        .as("Clean bootstrap must retain only canonical triage persistence")
                        .isTrue();
            }
        }
    }
}
