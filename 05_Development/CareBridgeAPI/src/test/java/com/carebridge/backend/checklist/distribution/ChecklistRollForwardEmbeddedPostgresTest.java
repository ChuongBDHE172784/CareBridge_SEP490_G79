package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Duration;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Verifies the opt-in roll-forward location against a database at 20260731020000. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistRollForwardEmbeddedPostgresTest {

    private static final String PRE_FINALIZER_ATTESTATION =
            "REQUEST_100_PARITY_WRITERS_FROZEN_CATALOG_AND_LEDGER_CAPTURED_V1";
    private static final Path DEPLOYMENT_FINALIZER = Path.of(
            "..", "Deployment", "database", "finalizers",
            "V20260729150001__finalize_checklist_retention_security.sql");
    private static final Path PRE_FINALIZER = Path.of(
            "src", "main", "resources", "db", "migration",
            "checklist_retirement_pre_finalizer.sql");

    @Test
    @Timeout(240)
    void appliesOnlyTheOptInRollForwardAfterTheExistingHistory() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);

            Flyway staged = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-legacy")
                    .cleanDisabled(true)
                    .outOfOrder(false)
                    .validateOnMigrate(true)
                    .ignoreMigrationPatterns("*:future")
                    .target("20260731020000")
                    .load();
            assertThat(staged.migrate().success).isTrue();

            runSqlScript(dataSource, DEPLOYMENT_FINALIZER, null, null);
            String flywayRole;
            try (Connection connection = dataSource.getConnection();
                    var statement = connection.createStatement();
                    var result = statement.executeQuery("select current_user")) {
                assertThat(result.next()).isTrue();
                flywayRole = result.getString(1);
            }
            runSqlScript(dataSource, PRE_FINALIZER, flywayRole, PRE_FINALIZER_ATTESTATION);

            Flyway rollForward = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-roll-forward")
                    .cleanDisabled(true)
                    .outOfOrder(false)
                    .validateOnMigrate(true)
                    .ignoreMigrationPatterns("*:missing")
                    .load();
            var first = rollForward.migrate();
            assertThat(first.success).isTrue();
            assertThat(first.migrationsExecuted).isOne();
            assertThat(rollForward.info().current().getVersion().getVersion())
                    .isEqualTo("20260731070000");

            try (Connection connection = dataSource.getConnection();
                    var statement = connection.createStatement();
                    var result = statement.executeQuery("""
                            select to_regclass('public.checklist_instances') is not null,
                                   to_regclass('public.checklist_task_instances') is not null,
                                   to_regclass('public.checklist_action_commands') is not null,
                                   to_regclass('public.checklist_substages') is null,
                                   to_regclass('public.checklist_migration_quarantine') is null,
                                   to_regclass('public.health_metric_definitions') is not null,
                                   to_regclass('public.health_observations') is not null
                            """)) {
                assertThat(result.next()).isTrue();
                for (int column = 1; column <= 7; column++) {
                    assertThat(result.getBoolean(column)).isTrue();
                }
            }

            var replay = rollForward.migrate();
            assertThat(replay.success).isTrue();
            assertThat(replay.migrationsExecuted).isZero();
            assertThat(rollForward.validateWithResult().validationSuccessful).isTrue();
        }
    }

    @Test
    @Timeout(120)
    void rejectsAHistoryThatIsNotExactlyTheSupportedRollForwardPoint() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .start()) {
            EmbeddedPostgresRoleFixture.provision(postgres.getPostgresDatabase());

            Flyway rollForward = Flyway.configure()
                    .dataSource(postgres.getPostgresDatabase())
                    .locations("classpath:db/migration-roll-forward")
                    .cleanDisabled(true)
                    .outOfOrder(false)
                    .validateOnMigrate(true)
                    .ignoreMigrationPatterns("*:missing")
                    .load();

            assertThatThrownBy(rollForward::migrate)
                    .isInstanceOf(FlywayException.class)
                    .hasMessageContaining("CHECKLIST_ROLL_FORWARD_HISTORY_REQUIRED");
        }
    }

    private static void runSqlScript(DataSource dataSource, Path path, String flywayRole,
                                     String operatorAttestation) throws Exception {
        String sql = Files.readString(path).replaceAll("(?m)^\\\\set[^\\r\\n]*(?:\\r?\\n)?", "");
        if (flywayRole != null) {
            sql = sql.replace(":'flyway_role'", "'" + flywayRole.replace("'", "''") + "'");
        }
        if (operatorAttestation != null) {
            sql = sql.replace(":'operator_attestation'",
                    "'" + operatorAttestation.replace("'", "''") + "'");
        }
        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
