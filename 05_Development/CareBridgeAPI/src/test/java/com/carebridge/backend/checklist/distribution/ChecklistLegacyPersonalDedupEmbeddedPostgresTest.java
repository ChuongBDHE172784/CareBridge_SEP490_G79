package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class ChecklistLegacyPersonalDedupEmbeddedPostgresTest {

    @Test
    @Timeout(180)
    void migrationCancelsOnlyAllPendingDuplicatesAndPreservesEveryProgressedCopy() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-legacy")
                    .target("20260730235900")
                    .load()
                    .migrate();

            try (Connection connection = dataSource.getConnection()) {
                seedMultiGroupLegacyDuplicates(connection);
            }

            Flyway dedup = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-legacy")
                    .target("20260731010000")
                    .load();
            var first = migrateWhileConcurrentTaskWriterIsBlocked(dedup, dataSource);
            var replay = dedup.migrate();

            assertThat(first.success).isTrue();
            assertThat(first.migrationsExecuted).isOne();
            assertThat(replay.success).isTrue();
            assertThat(replay.migrationsExecuted).isZero();
            assertThat(dedup.validateWithResult().validationSuccessful).isTrue();

            try (Connection connection = dataSource.getConnection()) {
                assertAllPendingDuplicatesCollapseToPersonalCopy(connection);
                assertProgressedDuplicatesAreNeverOverwritten(connection);
                assertCancellationAudit(connection);
                assertThat(longValue(connection, """
                        SELECT count(*) FROM public.checklist_task_instances
                        WHERE checklist_task_instance_id =
                              '7f000000-0000-0000-0000-000000000008'
                        """)).isZero();
            }
        }
    }

    private static MigrateResult migrateWhileConcurrentTaskWriterIsBlocked(
            Flyway dedup,
            DataSource dataSource) throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (Connection auditBlocker = dataSource.getConnection()) {
            auditBlocker.setAutoCommit(false);
            try (var statement = auditBlocker.createStatement()) {
                statement.execute("LOCK TABLE public.audit_events IN ACCESS EXCLUSIVE MODE");
            }

            var migration = executor.submit(dedup::migrate);
            waitForDeduplicationLocks(dataSource);

            try (Connection writer = dataSource.getConnection();
                 var statement = writer.createStatement()) {
                statement.execute("SET statement_timeout = '1000ms'");
                SQLException blocked = catchThrowableOfType(() -> statement.execute("""
                        INSERT INTO public.checklist_task_instances
                            (checklist_task_instance_id, checklist_instance_id,
                             template_version_id, template_item_version_id,
                             task_key, key_version, title_snapshot, display_order,
                             is_required, target_subject, status, lock_version,
                             created_at, updated_at)
                        VALUES
                            ('7f000000-0000-0000-0000-000000000008',
                             '7e000000-0000-0000-0000-000000000001',
                             '7d000000-0000-0000-0000-000000000003',
                             '7d000000-0000-0000-0000-000000000101',
                             repeat('8', 64), 'v1', 'Concurrent writer sentinel', 2,
                             true, 'MOTHER', 'PENDING', 0, now(), now())
                        """), SQLException.class);
                if (blocked == null) {
                    throw new AssertionError("Concurrent task writer was not blocked by the migration locks");
                }
                assertThat(blocked.getSQLState()).isEqualTo("57014");
            }

            auditBlocker.rollback();
            return migration.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void waitForDeduplicationLocks(DataSource dataSource) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try (Connection observer = dataSource.getConnection()) {
            while (System.nanoTime() < deadline) {
                if (longValue(observer, """
                        SELECT count(*)
                        FROM pg_locks lock
                        JOIN pg_class relation ON relation.oid = lock.relation
                        WHERE relation.relname IN
                              ('checklist_instances', 'checklist_task_instances')
                          AND lock.mode = 'ShareRowExclusiveLock'
                          AND lock.granted
                        """) == 2L) {
                    return;
                }
                Thread.sleep(25);
            }
        }
        throw new AssertionError("Deduplication migration did not acquire both DML-blocking locks");
    }

    private static void seedMultiGroupLegacyDuplicates(Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO public.care_groups
                        (care_group_id, owner_user_id, group_name, linked_journey_id,
                         status, created_at, updated_at)
                    VALUES
                        ('5d000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'Legacy personal group one',
                         '40000000-0000-0000-0000-000000000001',
                         'ACTIVE', now(), now()),
                        ('5d000000-0000-0000-0000-000000000002',
                         '10000000-0000-0000-0000-000000000004',
                         'Legacy personal group two',
                         '40000000-0000-0000-0000-000000000001',
                         'ACTIVE', now(), now())
                    """);
            statement.execute("""
                    INSERT INTO public.care_item_templates
                        (template_id, entry_type, title, description, stage, is_active,
                         version, content_status, template_lineage_id, template_version_id,
                         migration_review_required, distribution_enabled, template_type,
                         created_at, updated_at)
                    VALUES
                        ('7d000000-0000-0000-0000-000000000001', 'TEMPLATE_ROOT',
                         'Legacy personal dedup template', 'fixture', 'PREGNANCY', true,
                         1, 'PENDING_REVIEW',
                         '7d000000-0000-0000-0000-000000000002',
                         '7d000000-0000-0000-0000-000000000003',
                         false, false, 'MANDATORY', now(), now())
                    """);
            statement.execute("""
                    INSERT INTO public.care_item_templates
                        (template_id, parent_template_id, entry_type, title, display_order,
                         stage, is_active, target_subject, is_required, created_at, updated_at)
                    VALUES
                        ('7d000000-0000-0000-0000-000000000101',
                         '7d000000-0000-0000-0000-000000000001',
                         'CHECKLIST_ENTRY', 'Legacy dedup item', 1,
                         'PREGNANCY', true, 'MOTHER', true, now(), now())
                    """);
            statement.execute("""
                    INSERT INTO public.checklist_template_recipient_roles
                        (template_version_id, recipient_role)
                    VALUES ('7d000000-0000-0000-0000-000000000003', 'MOTHER')
                    """);
            statement.execute("""
                    INSERT INTO public.checklist_instances
                        (checklist_instance_id, distribution_key, key_version,
                         template_lineage_id, template_version_id,
                         recipient_user_id, recipient_role, care_group_id,
                         care_context_type, care_context_id, context_owner_user_id,
                         origin, window_start, window_end, status, lock_version,
                         created_at, updated_at)
                    VALUES
                        ('7e000000-0000-0000-0000-000000000001', repeat('1', 64), 'v1',
                         '7d000000-0000-0000-0000-000000000002',
                         '7d000000-0000-0000-0000-000000000003',
                         '10000000-0000-0000-0000-000000000004', 'MOTHER', NULL,
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'SYSTEM_TEMPLATE', DATE '2026-08-01', DATE '2026-08-01',
                         'PENDING', 0, '2026-08-01T00:00:00Z', '2026-08-01T00:00:00Z'),
                        ('7e000000-0000-0000-0000-000000000002', repeat('2', 64), 'v1',
                         '7d000000-0000-0000-0000-000000000002',
                         '7d000000-0000-0000-0000-000000000003',
                         '10000000-0000-0000-0000-000000000004', 'MOTHER',
                         '5d000000-0000-0000-0000-000000000001',
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'SYSTEM_TEMPLATE', DATE '2026-08-01', DATE '2026-08-01',
                         'PENDING', 0, '2026-08-02T00:00:00Z', '2026-08-02T00:00:00Z'),
                        ('7e000000-0000-0000-0000-000000000003', repeat('3', 64), 'v1',
                         '7d000000-0000-0000-0000-000000000002',
                         '7d000000-0000-0000-0000-000000000003',
                         '10000000-0000-0000-0000-000000000004', 'MOTHER',
                         '5d000000-0000-0000-0000-000000000002',
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'SYSTEM_TEMPLATE', DATE '2026-08-01', DATE '2026-08-01',
                         'PENDING', 0, '2026-08-03T00:00:00Z', '2026-08-03T00:00:00Z'),
                        ('7e000000-0000-0000-0000-000000000004', repeat('4', 64), 'v1',
                         '7d000000-0000-0000-0000-000000000002',
                         '7d000000-0000-0000-0000-000000000003',
                         '10000000-0000-0000-0000-000000000004', 'MOTHER', NULL,
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'SYSTEM_TEMPLATE', DATE '2026-09-01', DATE '2026-09-01',
                         'PENDING', 0, '2026-09-01T00:00:00Z', '2026-09-01T00:00:00Z'),
                        ('7e000000-0000-0000-0000-000000000005', repeat('5', 64), 'v1',
                         '7d000000-0000-0000-0000-000000000002',
                         '7d000000-0000-0000-0000-000000000003',
                         '10000000-0000-0000-0000-000000000004', 'MOTHER',
                         '5d000000-0000-0000-0000-000000000001',
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'SYSTEM_TEMPLATE', DATE '2026-09-01', DATE '2026-09-01',
                         'PENDING', 0, '2026-09-02T00:00:00Z', '2026-09-02T00:00:00Z'),
                        ('7e000000-0000-0000-0000-000000000006', repeat('6', 64), 'v1',
                         '7d000000-0000-0000-0000-000000000002',
                         '7d000000-0000-0000-0000-000000000003',
                         '10000000-0000-0000-0000-000000000004', 'MOTHER',
                         '5d000000-0000-0000-0000-000000000002',
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'SYSTEM_TEMPLATE', DATE '2026-09-01', DATE '2026-09-01',
                         'IN_PROGRESS', 0, '2026-07-25T00:00:00Z', '2026-07-25T00:00:00Z'),
                        ('7e000000-0000-0000-0000-000000000007', repeat('7', 64), 'v1',
                         NULL, NULL,
                         '10000000-0000-0000-0000-000000000004', 'MOTHER', NULL,
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'USER_CREATED', NULL, NULL,
                         'PENDING', 0, '2026-07-26T00:00:00Z', '2026-07-26T00:00:00Z')
                    """);
            statement.execute("""
                    INSERT INTO public.checklist_task_instances
                        (checklist_task_instance_id, checklist_instance_id,
                         template_version_id, template_item_version_id,
                         task_key, key_version, title_snapshot, display_order,
                         is_required, target_subject, status, lock_version,
                         completed_at, created_at, updated_at)
                    VALUES
                        ('7f000000-0000-0000-0000-000000000001',
                         '7e000000-0000-0000-0000-000000000001',
                         '7d000000-0000-0000-0000-000000000003',
                         '7d000000-0000-0000-0000-000000000101',
                         repeat('a', 64), 'v1', 'Personal pending A', 1, true,
                         'MOTHER', 'PENDING', 0, NULL, now(), now()),
                        ('7f000000-0000-0000-0000-000000000002',
                         '7e000000-0000-0000-0000-000000000002',
                         '7d000000-0000-0000-0000-000000000003',
                         '7d000000-0000-0000-0000-000000000101',
                         repeat('b', 64), 'v1', 'Group pending A1', 1, true,
                         'MOTHER', 'PENDING', 0, NULL, now(), now()),
                        ('7f000000-0000-0000-0000-000000000003',
                         '7e000000-0000-0000-0000-000000000003',
                         '7d000000-0000-0000-0000-000000000003',
                         '7d000000-0000-0000-0000-000000000101',
                         repeat('c', 64), 'v1', 'Group pending A2', 1, true,
                         'MOTHER', 'PENDING', 0, NULL, now(), now()),
                        ('7f000000-0000-0000-0000-000000000004',
                         '7e000000-0000-0000-0000-000000000004',
                         '7d000000-0000-0000-0000-000000000003',
                         '7d000000-0000-0000-0000-000000000101',
                         repeat('d', 64), 'v1', 'Personal pending B', 1, true,
                         'MOTHER', 'PENDING', 0, NULL, now(), now()),
                        ('7f000000-0000-0000-0000-000000000005',
                         '7e000000-0000-0000-0000-000000000005',
                         '7d000000-0000-0000-0000-000000000003',
                         '7d000000-0000-0000-0000-000000000101',
                         repeat('e', 64), 'v1', 'Completed progress B', 1, true,
                         'MOTHER', 'COMPLETED', 0, '2026-09-04T03:00:00Z',
                         now(), '2026-09-04T03:00:00Z'),
                        ('7f000000-0000-0000-0000-000000000006',
                         '7e000000-0000-0000-0000-000000000006',
                         '7d000000-0000-0000-0000-000000000003',
                         '7d000000-0000-0000-0000-000000000101',
                         repeat('f', 64), 'v1', 'In-progress parent B', 1, true,
                         'MOTHER', 'PENDING', 0, NULL, now(), now()),
                        ('7f000000-0000-0000-0000-000000000007',
                         '7e000000-0000-0000-0000-000000000007',
                         NULL, NULL,
                         repeat('0', 64), 'v1', 'User-created sentinel', 1, false,
                         'MOTHER', 'PENDING', 0, NULL, now(), now())
                    """);
            // Model retained legacy rows created before terminal-shape guards existed.
            // Re-add the guards NOT VALID so new/changed rows remain protected while
            // these deliberately inconsistent PENDING sentinels can be rehearsed.
            statement.execute("""
                    ALTER TABLE public.checklist_instances
                        DROP CONSTRAINT checklist_instances_terminal_shape_ck;
                    UPDATE public.checklist_instances
                       SET status = 'PENDING', completed_at = '2026-09-05T03:00:00Z'
                     WHERE checklist_instance_id =
                           '7e000000-0000-0000-0000-000000000006';
                    ALTER TABLE public.checklist_instances
                        ADD CONSTRAINT checklist_instances_terminal_shape_ck CHECK (
                            (status = 'COMPLETED' AND completed_at IS NOT NULL
                                AND cancelled_at IS NULL) OR
                            (status = 'CANCELLED' AND cancelled_at IS NOT NULL
                                AND completed_at IS NULL) OR
                            (status IN ('PENDING','IN_PROGRESS')
                                AND completed_at IS NULL AND cancelled_at IS NULL)
                        ) NOT VALID
                    """);
            statement.execute("""
                    ALTER TABLE public.checklist_task_instances
                        DROP CONSTRAINT checklist_task_instances_terminal_shape_ck;
                    UPDATE public.checklist_task_instances
                       SET completed_at = '2026-09-05T04:00:00Z'
                     WHERE checklist_task_instance_id =
                           '7f000000-0000-0000-0000-000000000006';
                    ALTER TABLE public.checklist_task_instances
                        ADD CONSTRAINT checklist_task_instances_terminal_shape_ck CHECK (
                            (status = 'COMPLETED' AND completed_at IS NOT NULL
                                AND skipped_at IS NULL AND cancelled_at IS NULL) OR
                            (status = 'SKIPPED' AND skipped_at IS NOT NULL
                                AND completed_at IS NULL AND cancelled_at IS NULL) OR
                            (status = 'CANCELLED' AND cancelled_at IS NOT NULL
                                AND completed_at IS NULL AND skipped_at IS NULL) OR
                            (status IN ('PENDING','IN_PROGRESS')
                                AND completed_at IS NULL AND skipped_at IS NULL
                                AND cancelled_at IS NULL)
                        ) NOT VALID
                    """);
        }
    }

    private static void assertAllPendingDuplicatesCollapseToPersonalCopy(Connection connection) throws Exception {
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances
                WHERE checklist_instance_id = '7e000000-0000-0000-0000-000000000001'
                  AND distribution_key = repeat('1', 64)
                  AND care_group_id IS NULL AND status = 'PENDING' AND lock_version = 0
                """)).isOne();
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances
                WHERE checklist_instance_id IN
                      ('7e000000-0000-0000-0000-000000000002',
                       '7e000000-0000-0000-0000-000000000003')
                  AND status = 'CANCELLED'
                  AND cancellation_reason_code = 'LEGACY_PERSONAL_DUPLICATE'
                  AND cancelled_at IS NOT NULL AND lock_version = 1
                  AND ((checklist_instance_id = '7e000000-0000-0000-0000-000000000002'
                        AND distribution_key = repeat('2', 64))
                    OR (checklist_instance_id = '7e000000-0000-0000-0000-000000000003'
                        AND distribution_key = repeat('3', 64)))
                """)).isEqualTo(2L);
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_task_instances
                WHERE checklist_task_instance_id IN
                      ('7f000000-0000-0000-0000-000000000002',
                       '7f000000-0000-0000-0000-000000000003')
                  AND status = 'CANCELLED'
                  AND action_reason_code = 'LEGACY_PERSONAL_DUPLICATE'
                  AND cancelled_at IS NOT NULL AND lock_version = 1
                """)).isEqualTo(2L);
    }

    private static void assertProgressedDuplicatesAreNeverOverwritten(Connection connection) throws Exception {
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances
                WHERE checklist_instance_id = '7e000000-0000-0000-0000-000000000004'
                  AND status = 'CANCELLED'
                  AND cancellation_reason_code = 'LEGACY_PERSONAL_DUPLICATE'
                """)).isOne();
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances parent
                JOIN public.checklist_task_instances task
                  ON task.checklist_instance_id = parent.checklist_instance_id
                WHERE parent.checklist_instance_id = '7e000000-0000-0000-0000-000000000005'
                  AND parent.distribution_key = repeat('5', 64)
                  AND parent.status = 'PENDING' AND parent.lock_version = 0
                  AND parent.cancelled_at IS NULL AND parent.cancellation_reason_code IS NULL
                  AND task.status = 'COMPLETED'
                  AND task.completed_at = '2026-09-04T03:00:00Z'
                  AND task.cancelled_at IS NULL AND task.action_reason_code IS NULL
                  AND task.lock_version = 0
                """)).isOne();
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances parent
                JOIN public.checklist_task_instances task
                  ON task.checklist_instance_id = parent.checklist_instance_id
                WHERE parent.checklist_instance_id = '7e000000-0000-0000-0000-000000000006'
                  AND parent.distribution_key = repeat('6', 64)
                  AND parent.status = 'PENDING' AND parent.lock_version = 0
                  AND parent.completed_at = '2026-09-05T03:00:00Z'
                  AND parent.cancelled_at IS NULL AND parent.cancellation_reason_code IS NULL
                  AND task.status = 'PENDING' AND task.lock_version = 0
                  AND task.completed_at = '2026-09-05T04:00:00Z'
                  AND task.cancelled_at IS NULL AND task.action_reason_code IS NULL
                """)).isOne();
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances
                WHERE template_version_id = '7d000000-0000-0000-0000-000000000003'
                  AND window_start = DATE '2026-09-01' AND status <> 'CANCELLED'
                """)).isEqualTo(2L);
        assertThat(longValue(connection, """
                SELECT count(*)
                FROM public.checklist_instances parent
                JOIN public.checklist_task_instances task
                  ON task.checklist_instance_id = parent.checklist_instance_id
                WHERE parent.checklist_instance_id = '7e000000-0000-0000-0000-000000000007'
                  AND parent.origin = 'USER_CREATED'
                  AND parent.distribution_key = repeat('7', 64)
                  AND parent.status = 'PENDING' AND parent.lock_version = 0
                  AND task.status = 'PENDING' AND task.task_key = repeat('0', 64)
                  AND task.lock_version = 0
                """)).isOne();
    }

    private static void assertCancellationAudit(Connection connection) throws Exception {
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.audit_events
                WHERE event_category = 'CHECKLIST_CANCELLED'
                  AND reason_code = 'LEGACY_PERSONAL_DUPLICATE'
                  AND actor_service = 'CHECKLIST_LEGACY_PERSONAL_DEDUP'
                  AND resource_type = 'CHECKLIST_INSTANCE'
                """)).isEqualTo(3L);
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.audit_events
                WHERE event_category = 'CHECKLIST_CANCELLED'
                  AND reason_code = 'LEGACY_PERSONAL_DUPLICATE'
                  AND actor_service = 'CHECKLIST_LEGACY_PERSONAL_DEDUP'
                  AND resource_type = 'CHECKLIST_TASK_INSTANCE'
                  AND checklist_task_instance_id IS NOT NULL
                """)).isEqualTo(3L);
    }

    private static long longValue(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            return result.next() ? result.getLong(1) : 0L;
        }
    }
}
