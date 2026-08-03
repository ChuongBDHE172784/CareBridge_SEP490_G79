package com.carebridge.backend.recommendation;

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

/** Minimum academic gate: exercise the recommendation migration on real PostgreSQL without Docker. */
@EnabledOnOs(OS.WINDOWS)
class RecommendationMigrationEmbeddedPostgresTest {

    @Test
    @Timeout(240)
    void recommendationSchemaAndCatalogSurviveFreshAndRepeatMigration() throws Exception {
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
                try (var columns = statement.executeQuery("""
                        select
                          to_regclass('public.mother_journeys') is not null,
                          to_regclass('public.content_items') is not null,
                          to_regclass('public.community_topics') is not null,
                          to_regclass('public.content_item_topics') is not null,
                          to_regclass('public.data_permissions') is not null,
                          to_regclass('public.recommendation_profiles') is null,
                          (select count(*) from information_schema.columns
                             where table_schema='public' and table_name='mother_journeys'
                               and column_name in ('recommendation_profile_jsonb', 'recommendation_profile_version',
                                                   'recommendation_profile_completed_at', 'recommendation_profile_status')) = 4,
                          (select count(*) from information_schema.columns
                             where table_schema='public' and table_name='content_items'
                               and column_name in ('eligible_from_week', 'eligible_to_week', 'recommendation_priority')) = 3,
                          (select count(*) from public.community_topics where slug like 'rec-%'
                               and type = 'TAG' and parent_id is null and is_hidden = false) = 107
                        """)) {
                    assertThat(columns.next()).isTrue();
                    for (int column = 1; column <= 9; column++) {
                        assertThat(columns.getBoolean(column))
                                .as("recommendation migration assertion %s", column)
                                .isTrue();
                    }
                }
            }
        }
    }
}
