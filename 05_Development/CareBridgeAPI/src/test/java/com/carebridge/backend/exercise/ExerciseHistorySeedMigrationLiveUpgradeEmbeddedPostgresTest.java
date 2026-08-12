package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.time.Duration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class ExerciseHistorySeedMigrationLiveUpgradeEmbeddedPostgresTest {

    private static final String PRE_RECONCILIATION_VERSION = "20260810100000";
    private static final String RECONCILIATION_VERSION = "20260810110000";
    private static final String FAILURE_MARKER =
            "EXERCISE_TEMPLATE_ENUM_RECONCILE_FAILED";
    private static final String SEEDED_EXERCISE_ID =
            "60000000-0000-0000-0000-000000000003";

    @Test
    @Timeout(240)
    void upgradesLegacyExerciseEnumsAndSecondMigrationIsNoOp() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            EmbeddedPostgresRoleFixture.provision(postgres.getPostgresDatabase());

            Flyway beforeReconciliation = flyway(postgres, PRE_RECONCILIATION_VERSION);
            assertThat(beforeReconciliation.migrate().success).isTrue();
            assertSeedValues(postgres, "PREGNANCY", "ACTIVE");

            Flyway upgraded = flyway(postgres, RECONCILIATION_VERSION);
            MigrateResult firstUpgrade = upgraded.migrate();
            assertThat(firstUpgrade.success).isTrue();
            assertThat(firstUpgrade.migrationsExecuted).isOne();
            assertSeedValues(postgres, "ALL", "PUBLISHED");
            assertThat(upgraded.validateWithResult().validationSuccessful).isTrue();

            MigrateResult secondUpgrade = upgraded.migrate();
            assertThat(secondUpgrade.success).isTrue();
            assertThat(secondUpgrade.migrationsExecuted).isZero();
        }
    }

    @Test
    @Timeout(240)
    void unreadableDifficultyFailsGateAndRollsBackSeedRepair() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            EmbeddedPostgresRoleFixture.provision(postgres.getPostgresDatabase());

            Flyway beforeReconciliation = flyway(postgres, PRE_RECONCILIATION_VERSION);
            assertThat(beforeReconciliation.migrate().success).isTrue();
            updateSeedDifficulty(postgres, "UNREADABLE_DIFFICULTY");

            Flyway upgraded = flyway(postgres, RECONCILIATION_VERSION);
            assertThatThrownBy(upgraded::migrate)
                    .hasStackTraceContaining(FAILURE_MARKER)
                    .hasStackTraceContaining("difficulty_level=UNREADABLE_DIFFICULTY");

            assertSeedValues(postgres, "PREGNANCY", "ACTIVE");
            assertSeedDifficulty(postgres, "UNREADABLE_DIFFICULTY");
        }
    }

    private static void updateSeedDifficulty(EmbeddedPostgres postgres, String difficulty)
            throws Exception {
        try (Connection connection = postgres.getPostgresDatabase().getConnection();
                var statement = connection.prepareStatement("""
                        update public.care_item_templates
                           set difficulty_level = ?
                         where template_id = ?::uuid
                           and entry_type = 'EXERCISE_TEMPLATE'
                        """)) {
            statement.setString(1, difficulty);
            statement.setString(2, SEEDED_EXERCISE_ID);
            assertThat(statement.executeUpdate()).isOne();
        }
    }

    private static void assertSeedDifficulty(
            EmbeddedPostgres postgres, String expectedDifficulty) throws Exception {
        try (Connection connection = postgres.getPostgresDatabase().getConnection();
                var statement = connection.prepareStatement("""
                        select difficulty_level
                          from public.care_item_templates
                         where template_id = ?::uuid
                           and entry_type = 'EXERCISE_TEMPLATE'
                        """)) {
            statement.setString(1, SEEDED_EXERCISE_ID);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("difficulty_level"))
                        .isEqualTo(expectedDifficulty);
                assertThat(result.next()).isFalse();
            }
        }
    }

    private static void assertSeedValues(
            EmbeddedPostgres postgres, String expectedStage, String expectedStatus)
            throws Exception {
        try (Connection connection = postgres.getPostgresDatabase().getConnection();
                var statement = connection.prepareStatement("""
                        select stage, template_status
                          from public.care_item_templates
                         where template_id = ?::uuid
                           and entry_type = 'EXERCISE_TEMPLATE'
                        """)) {
            statement.setString(1, SEEDED_EXERCISE_ID);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("stage")).isEqualTo(expectedStage);
                assertThat(result.getString("template_status")).isEqualTo(expectedStatus);
                assertThat(result.next()).isFalse();
            }
        }
    }

    private static Flyway flyway(EmbeddedPostgres postgres, String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getPostgresDatabase())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .outOfOrder(true)
                .validateOnMigrate(true)
                .ignoreMigrationPatterns("*:future");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
