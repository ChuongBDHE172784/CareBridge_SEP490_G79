package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Opt-in CHK-041 Flyway/backfill measurements on PostgreSQL 18.1. */
@EnabledOnOs(OS.WINDOWS)
@EnabledIfSystemProperty(named = "checklist.performance.enabled", matches = "true")
class ChecklistMigrationPerformanceEmbeddedPostgresTest {

    private static final int BACKFILL_ROWS = 10_000;
    private static final String OWNER = "10000000-0000-0000-0000-000000000004";
    private static final String GROUP = "50000000-0000-0000-0000-000000000001";
    private static final String JOURNEY = "40000000-0000-0000-0000-000000000001";

    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    void backfillThroughputFullMigrationAndControlledFlywayLockAreMeasured() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            long baseStarted = System.nanoTime();
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration-legacy")
                    .target("20260729060000").load().migrate();
            long baseElapsed = System.nanoTime() - baseStarted;
            try (Connection connection = dataSource.getConnection()) {
                seedLegacyRows(connection);
            }

            long backfillStarted = System.nanoTime();
            var backfill = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration-legacy")
                    .target("20260729070000").load().migrate();
            long backfillElapsed = System.nanoTime() - backfillStarted;
            assertThat(backfill.success).isTrue();
            long migratedRows = countRows(dataSource, """
                    select count(*) from checklist_task_instances
                    where title_snapshot like 'CHK-041 load row %'
                    """);
            double backfillRowsPerSecond = migratedRows / (backfillElapsed / 1_000_000_000.0d);

            long intermediateStarted = System.nanoTime();
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration-legacy")
                    .target("20260729130000").load().migrate();
            long intermediateElapsed = System.nanoTime() - intermediateStarted;
            long repairStarted = System.nanoTime();
            var repair = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration-legacy")
                    .target("20260729140000").load().migrate();
            long repairElapsed = System.nanoTime() - repairStarted;
            double repairRowsPerSecond = migratedRows / (repairElapsed / 1_000_000_000.0d);

            long latestStarted = System.nanoTime();
            Flyway latest = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration-legacy")
                    .target("20260730050000").load();
            var remainder = latest.migrate();
            long latestElapsed = System.nanoTime() - latestStarted;
            long fullElapsed = baseElapsed + backfillElapsed + intermediateElapsed + repairElapsed + latestElapsed;
            assertThat(repair.success).isTrue();
            assertThat(remainder.success).isTrue();
            assertThat(latest.validateWithResult().validationSuccessful).isTrue();

            System.out.printf(
                    "CHK-041 migration: backfillRows=%d backfillSeconds=%.2f backfillRowsPerSecond=%.2f "
                            + "intermediateSeconds=%.2f repairSeconds=%.2f repairRowsPerSecond=%.2f "
                            + "fullMigrationSeconds=%.2f%n",
                    migratedRows, backfillElapsed / 1_000_000_000.0d, backfillRowsPerSecond,
                    intermediateElapsed / 1_000_000_000.0d, repairElapsed / 1_000_000_000.0d, repairRowsPerSecond,
                    fullElapsed / 1_000_000_000.0d);

            assertThat(migratedRows).isEqualTo(BACKFILL_ROWS);
            assertThat(backfillRowsPerSecond)
                    .as("legacy backfill throughput")
                    .isGreaterThanOrEqualTo(500.0d);
            assertThat(repairRowsPerSecond)
                    .as("legacy occurrence repair throughput")
                    .isGreaterThanOrEqualTo(500.0d);
            assertThat(fullElapsed)
                    .as("full migration including the seeded backfill")
                    .isLessThanOrEqualTo(TimeUnit.MINUTES.toNanos(30));
            assertThat(countRows(dataSource, "select count(*) from checklist_task_instances")).isGreaterThanOrEqualTo(BACKFILL_ROWS);
            assertThat(countRows(dataSource, """
                    select count(*) from audit_events
                    where event_category = 'CHECKLIST_CANCELLED'
                      and actor_service = 'CHECKLIST_LEGACY_OCCURRENCE_REPAIR'
                      and reason_code = 'LEGACY_OCCURRENCE_REPAIRED'
                      and before_payload_jsonb->>'status' = 'PENDING'
                      and after_payload_jsonb->>'status' = 'CANCELLED'
                      and after_payload_jsonb->>'reasonCode' = 'LEGACY_OCCURRENCE_REPAIRED'
                    """)).isEqualTo(1L);
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void flywayHistoryLockContentionResolvesWithinFiveSeconds() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration-legacy")
                    .target("20260729140000").load().migrate();
            Flyway latest = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration-legacy")
                    .target("20260730050000").load();
            try (Connection blocker = dataSource.getConnection()) {
                blocker.setAutoCommit(false);
                blocker.createStatement().execute("LOCK TABLE flyway_schema_history IN ACCESS EXCLUSIVE MODE");
                var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
                long started = System.nanoTime();
                var future = executor.submit(latest::migrate);
                try {
                    Thread.sleep(1_000L);
                    assertThat(future.isDone()).as("Flyway must be blocked by the held history lock").isFalse();
                    blocker.rollback();
                    var result = future.get(20, TimeUnit.SECONDS);
                    long elapsed = System.nanoTime() - started;
                    assertThat(result.success).isTrue();
                    System.out.printf("CHK-041 Flyway lock: heldSeconds=1.00 completionSeconds=%.2f%n",
                            elapsed / 1_000_000_000.0d);
                    assertThat(elapsed).as("controlled Flyway history lock contention")
                            .isLessThanOrEqualTo(TimeUnit.SECONDS.toNanos(5));
                } finally {
                    executor.shutdownNow();
                }
            }
        }
    }

    private static void seedLegacyRows(Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    insert into checklist_care_group_contexts
                        (context_mapping_id, care_group_id, owner_user_id,
                         care_context_type, care_context_id, review_status,
                         distribution_blocked, reviewed_at, reviewed_by)
                    values ('72000000-0000-0000-0000-000000000010', '%s', '%s', 'JOURNEY', '%s',
                            'REVIEWED', false, now(), '%s')
                    on conflict do nothing
                    """.formatted(GROUP, OWNER, JOURNEY, OWNER));
            statement.execute("""
                    insert into preparation_checklist_items
                        (checklist_item_id, owner_user_id, mother_journey_id,
                         title, display_order, status, created_at, updated_at)
                    select gen_random_uuid(), '%s', '%s', 'CHK-041 load row ' || n,
                           n, 'PENDING', timestamp '2026-08-01', timestamp '2026-08-01'
                    from generate_series(1, %d) n
                    """.formatted(OWNER, JOURNEY, BACKFILL_ROWS));
        }
    }

    private static long countRows(javax.sql.DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }
}
