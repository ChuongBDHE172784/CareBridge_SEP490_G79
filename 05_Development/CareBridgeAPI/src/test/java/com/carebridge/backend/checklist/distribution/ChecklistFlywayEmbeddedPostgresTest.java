package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.time.Duration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Runs the complete migration chain on a real Docker-free PostgreSQL 18 process. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistFlywayEmbeddedPostgresTest {

    @Test
    @Timeout(180)
    void fullFlywayChainIsRepeatableAndChecklistSchemaIsOperational() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            EmbeddedPostgresRoleFixture.provision(postgres.getPostgresDatabase());
            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getPostgresDatabase())
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)
                    .load();

            var first = flyway.migrate();
            var second = flyway.migrate();

            assertThat(first.success).isTrue();
            assertThat(second.success).isTrue();
            assertThat(second.migrationsExecuted).isZero();
            assertThat(flyway.validateWithResult().validationSuccessful).isTrue();

            try (Connection connection = postgres.getPostgresDatabase().getConnection();
                 var statement = connection.createStatement()) {
                try (var version = statement.executeQuery("show server_version")) {
                    assertThat(version.next()).isTrue();
                    assertThat(version.getString(1)).startsWith("18.1");
                }
                try (var schema = statement.executeQuery("""
                        select
                          to_regclass('public.checklist_instances') is not null,
                          to_regclass('public.checklist_task_instances') is not null,
                          to_regclass('public.checklist_action_commands') is not null,
                          to_regclass('public.checklist_reconciliation_candidates') is null,
                          to_regclass('public.checklist_reconciliation_runs') is null,
                          to_regclass('public.checklist_distribution_outbox') is null,
                          to_regclass('public.checklist_migration_quarantine') is null,
                          to_regclass('public.checklist_care_group_contexts') is null,
                          to_regclass('public.checklist_context_authorities') is null,
                          to_regclass('public.checklist_template_version_items') is null,
                          to_regclass('public.checklist_template_recipient_roles') is null,
                          to_regclass('public.checklist_substages') is null
                        """)) {
                    assertThat(schema.next()).isTrue();
                    for (int column = 1; column <= 12; column++) {
                        assertThat(schema.getBoolean(column))
                                .as("retirement catalog assertion %s", column)
                                .isTrue();
                    }
                }
            }
        }
    }
}
