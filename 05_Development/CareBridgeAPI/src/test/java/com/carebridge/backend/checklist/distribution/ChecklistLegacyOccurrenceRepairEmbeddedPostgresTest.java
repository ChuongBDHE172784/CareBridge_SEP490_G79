package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** CHK-022 acceptance against a real Docker-free PostgreSQL 18 process. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistLegacyOccurrenceRepairEmbeddedPostgresTest {

    private static final UUID OWNER = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID GROUP = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID JOURNEY = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ROOT = UUID.fromString("72000000-0000-0000-0000-000000000001");
    private static final UUID LINEAGE = UUID.fromString("72000000-0000-0000-0000-000000000002");
    private static final UUID VERSION = UUID.fromString("72000000-0000-0000-0000-000000000003");
    private static final UUID ITEM_ONE = UUID.fromString("72000000-0000-0000-0000-000000000101");
    private static final UUID ITEM_TWO = UUID.fromString("72000000-0000-0000-0000-000000000102");
    private static final UUID ITEM_THREE = UUID.fromString("72000000-0000-0000-0000-000000000103");
    private static final UUID ITEM_FOUR = UUID.fromString("72000000-0000-0000-0000-000000000104");

    private static final UUID OCCURRENCE_A_ITEM_ONE = source(1);
    private static final UUID OCCURRENCE_A_ITEM_TWO = source(2);
    private static final UUID OCCURRENCE_B_ITEM_ONE = source(3);
    private static final UUID OCCURRENCE_B_ITEM_TWO = source(4);
    private static final UUID CUSTOM_ONE = source(5);
    private static final UUID CUSTOM_TWO = source(6);
    private static final UUID COLLISION_ONE = source(7);
    private static final UUID COLLISION_TWO = source(8);
    private static final UUID MIXED_TERMINAL_COMPLETED = source(11);
    private static final UUID MIXED_TERMINAL_CANCELLED = source(12);
    private static final UUID MIXED_TERMINAL_SKIPPED = source(13);
    private static final String DRIFT_DISTRIBUTION_KEY = "a".repeat(64);
    private static final UUID DRIFT_PARENT = legacyParentId(
            ChecklistDistributionKeyFactory.instanceKey(
                    VERSION, OWNER, "MOTHER", GROUP, "JOURNEY", JOURNEY,
                    "2026-11-01", "2026-11-01"));
    private static final String TASK_DRIFT_DISTRIBUTION_KEY =
            ChecklistDistributionKeyFactory.instanceKey(
                    VERSION, OWNER, "MOTHER", GROUP, "JOURNEY", JOURNEY,
                    "2026-12-01", "2026-12-01");
    private static final UUID TASK_DRIFT_PARENT = legacyParentId(TASK_DRIFT_DISTRIBUTION_KEY);

    @Test
    @Timeout(180)
    void chk022_repairsOccurrenceGroupingWithoutDeletingLegacySources() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-legacy")
                    .target("20260729060000")
                    .load()
                    .migrate();

            try (Connection connection = dataSource.getConnection()) {
                seedLegacyOccurrences(connection);
            }

            var legacyBackfill = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-legacy")
                    .target("20260729070000")
                    .load()
                    .migrate();
            assertThat(legacyBackfill.success).isTrue();

            UUID progressedLegacyParent;
            try (Connection connection = dataSource.getConnection()) {
                progressedLegacyParent = seedPostV70000State(connection);
            }

            // This is a historical staged-chain repair test.  Keep it before the
            // retirement migration so the fixture can assert quarantine rows and
            // support-table state; retirement is covered by the canonical clean /
            // live-upgrade tests instead.
            Flyway latest = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-legacy")
                    .target("20260729140000")
                    .load();
            var result = latest.migrate();
            assertThat(result.success).isTrue();
            var replay = latest.migrate();
            assertThat(replay.success).isTrue();
            assertThat(replay.migrationsExecuted).isZero();
            assertThat(latest.validateWithResult().validationSuccessful).isTrue();

            try (Connection connection = dataSource.getConnection()) {
                assertSystemOccurrences(connection);
                assertStandaloneCustomRows(connection);
                assertCollisionQuarantineAndAudit(connection);
                assertAll(
                        () -> assertMixedTerminalOccurrence(connection),
                        () -> assertDriftAndHistorySafety(connection, progressedLegacyParent));
                assertSourceAndOrphanSafety(connection, progressedLegacyParent);
            }
        }
    }

    @Test
    @Timeout(180)
    void chk022_unreviewedContextIsQuarantinedAndNeverRegroupedAsAnOccurrence() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-legacy")
                    .target("20260729060000")
                    .load()
                    .migrate();

            try (Connection connection = dataSource.getConnection()) {
                seedLegacyOccurrences(connection);
                try (var statement = connection.createStatement()) {
                    statement.execute("""
                            UPDATE public.checklist_care_group_contexts
                            SET review_status = 'UNREVIEWED', distribution_blocked = true,
                                reviewed_at = NULL, reviewed_by = NULL
                            WHERE care_group_id = '50000000-0000-0000-0000-000000000001'
                              AND care_context_type = 'JOURNEY'
                              AND care_context_id = '40000000-0000-0000-0000-000000000001'
                            """);
                }
            }

            // The staged repair contract intentionally observes the support
            // catalog before retirement removes it.
            Flyway latest = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration-legacy")
                    .target("20260729140000")
                    .load();
            assertThat(latest.migrate().success).isTrue();

            try (Connection connection = dataSource.getConnection()) {
                assertThat(longValue(connection, """
                        SELECT count(*)
                        FROM public.checklist_instances
                        WHERE care_group_id = '50000000-0000-0000-0000-000000000001'
                          AND care_context_type = 'JOURNEY'
                          AND care_context_id = '40000000-0000-0000-0000-000000000001'
                          AND window_start IS NOT NULL
                        """)).isZero();
                assertThat(longValue(connection, """
                        SELECT count(*)
                        FROM public.checklist_migration_quarantine
                        WHERE source_id BETWEEN
                              '73000000-0000-0000-0000-000000000001'::uuid AND
                              '73000000-0000-0000-0000-000000000013'::uuid
                          AND reason_code = 'CONTEXT_OWNER_MISMATCH'
                        """)).isEqualTo(13L);
            }
        }
    }

    private static void seedLegacyOccurrences(Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            // The original V1 import-scope indexes reject repeated historical rows.
            // Drop them in this fixture to model a retained legacy database created
            // before those guards existed; V2 repair must remain safe for that data.
            statement.execute("DROP INDEX IF EXISTS public.uq_preparation_checklist_baby_import_scope");
            statement.execute("DROP INDEX IF EXISTS public.uq_preparation_checklist_journey_import_scope");
            statement.execute("DROP INDEX IF EXISTS public.uq_preparation_checklist_import_scope");
            statement.execute("""
                    INSERT INTO public.checklist_care_group_contexts
                        (context_mapping_id, care_group_id, owner_user_id,
                         care_context_type, care_context_id, review_status,
                         distribution_blocked, reviewed_at, reviewed_by)
                    VALUES
                        ('72000000-0000-0000-0000-000000000010',
                         '50000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         'REVIEWED', false, now(),
                         '10000000-0000-0000-0000-000000000004')
                    ON CONFLICT DO NOTHING
                    """);
            statement.execute("""
                    INSERT INTO public.care_item_templates
                        (template_id, entry_type, title, description, stage, is_active,
                         version, content_status, template_lineage_id, template_version_id,
                         substage_id, migration_review_required, distribution_enabled,
                         created_at, updated_at)
                    VALUES
                        ('72000000-0000-0000-0000-000000000001', 'TEMPLATE_ROOT',
                         'CHK-022 repeated template', 'fixture', 'PREGNANCY', true,
                         1, 'PENDING_REVIEW',
                         '72000000-0000-0000-0000-000000000002',
                         '72000000-0000-0000-0000-000000000003',
                         (SELECT substage_id FROM public.checklist_substages
                          WHERE code = 'LEGACY_PREGNANCY'),
                         true, false, '2026-07-01T00:00:00Z', '2026-07-01T00:00:00Z')
                    """);
            statement.execute("""
                    INSERT INTO public.care_item_templates
                        (template_id, parent_template_id, entry_type, title, display_order,
                         stage, is_active, target_subject, is_required, created_at, updated_at)
                    VALUES
                        ('72000000-0000-0000-0000-000000000101',
                         '72000000-0000-0000-0000-000000000001', 'CHECKLIST_ENTRY',
                         'Template item one', 1, 'PREGNANCY', true, 'MOTHER', false,
                         '2026-07-01T00:00:00Z', '2026-07-01T00:00:00Z'),
                        ('72000000-0000-0000-0000-000000000102',
                         '72000000-0000-0000-0000-000000000001', 'CHECKLIST_ENTRY',
                         'Template item two', 2, 'PREGNANCY', true, 'MOTHER', false,
                         '2026-07-01T00:00:00Z', '2026-07-01T00:00:00Z'),
                        ('72000000-0000-0000-0000-000000000103',
                         '72000000-0000-0000-0000-000000000001', 'CHECKLIST_ENTRY',
                         'Template item three', 3, 'PREGNANCY', true, 'MOTHER', false,
                         '2026-07-01T00:00:00Z', '2026-07-01T00:00:00Z'),
                        ('72000000-0000-0000-0000-000000000104',
                         '72000000-0000-0000-0000-000000000001', 'CHECKLIST_ENTRY',
                         'Template item four', 4, 'PREGNANCY', true, 'MOTHER', false,
                         '2026-07-01T00:00:00Z', '2026-07-01T00:00:00Z')
                    """);
            statement.execute("""
                    INSERT INTO public.checklist_template_recipient_roles
                        (template_version_id, recipient_role)
                    VALUES ('72000000-0000-0000-0000-000000000003', 'MOTHER')
                    """);
            statement.execute("""
                    INSERT INTO public.preparation_checklist_items
                        (checklist_item_id, owner_user_id, mother_journey_id,
                         template_entry_id, title, display_order, status, due_at,
                         created_at, updated_at)
                    VALUES
                        ('73000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000101',
                         'Occurrence A item one', 20, 'PENDING', '2026-08-01T02:00:00Z',
                         '2026-08-01T01:00:00Z', '2026-08-01T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000002',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000102',
                         'Occurrence A item two', 10, 'PENDING', '2026-08-02T03:00:00Z',
                         '2026-08-01T01:00:00Z', '2026-08-01T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000003',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000101',
                         'Occurrence B item one', 10, 'PENDING', '2026-09-01T02:00:00Z',
                         '2026-09-01T01:00:00Z', '2026-09-01T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000004',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000102',
                         'Occurrence B item two', 10, 'PENDING', '2026-09-01T03:00:00Z',
                         '2026-09-01T01:00:00Z', '2026-09-01T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000005',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001', NULL,
                         'Standalone custom one', 1, 'PENDING', '2026-08-15T02:00:00Z',
                         '2026-08-15T01:00:00Z', '2026-08-15T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000006',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001', NULL,
                         'Standalone custom two', 1, 'PENDING', '2026-08-15T02:00:00Z',
                         '2026-08-15T01:00:00Z', '2026-08-15T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000007',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000101',
                         'Collision one', 1, 'PENDING', '2026-10-01T02:00:00Z',
                         '2026-10-01T01:00:00Z', '2026-10-01T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000008',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000101',
                         'Collision two', 2, 'PENDING', '2026-10-01T03:00:00Z',
                         '2026-10-01T01:00:00Z', '2026-10-01T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000009',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000103',
                         'Parent drift sentinel', 1, 'PENDING', '2026-11-05T02:00:00Z',
                         '2026-11-01T01:00:00Z', '2026-11-01T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000010',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000104',
                         'Task drift sentinel', 1, 'CANCELLED', '2026-12-05T02:00:00Z',
                         '2026-12-01T01:00:00Z', '2026-12-01T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000011',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000101',
                         'Mixed terminal completed', 1, 'PENDING', '2027-01-02T02:00:00Z',
                         '2027-01-01T01:00:00Z', '2027-01-01T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000012',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000102',
                         'Mixed terminal cancelled', 2, 'PENDING', '2027-01-03T02:00:00Z',
                         '2027-01-01T01:00:00Z', '2027-01-01T01:00:00Z'),
                        ('73000000-0000-0000-0000-000000000013',
                         '10000000-0000-0000-0000-000000000004',
                         '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000103',
                         'Mixed terminal skipped', 3, 'PENDING', '2027-01-04T02:00:00Z',
                         '2027-01-01T01:00:00Z', '2027-01-01T01:00:00Z')
                    """);
            statement.execute("""
                    UPDATE public.preparation_checklist_items
                    SET status = 'COMPLETED', completed_at = '2027-01-01T03:00:00Z',
                        updated_at = '2027-01-01T03:00:00Z'
                    WHERE checklist_item_id =
                        '73000000-0000-0000-0000-000000000011'
                    """);
            statement.execute("""
                    UPDATE public.preparation_checklist_items
                    SET status = 'CANCELLED', updated_at = '2027-01-01T04:00:00Z'
                    WHERE checklist_item_id =
                        '73000000-0000-0000-0000-000000000012'
                    """);
            statement.execute("""
                    UPDATE public.preparation_checklist_items
                    SET status = 'SKIPPED', completed_at = '2027-01-01T05:00:00Z',
                        updated_at = '2027-01-01T05:00:00Z'
                    WHERE checklist_item_id =
                        '73000000-0000-0000-0000-000000000013'
                    """);
        }
    }

    private static UUID seedPostV70000State(Connection connection) throws Exception {
        UUID progressedParent = uuidValue(connection, """
                SELECT checklist_instance_id
                FROM public.checklist_task_instances
                WHERE checklist_task_instance_id =
                    '73000000-0000-0000-0000-000000000005'
                """);
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    UPDATE public.checklist_task_instances
                    SET status = 'IN_PROGRESS', updated_at = '2026-08-20T01:00:00Z'
                    WHERE checklist_task_instance_id =
                        '73000000-0000-0000-0000-000000000005'
                    """);
            statement.execute("""
                    UPDATE public.checklist_instances
                    SET status = 'IN_PROGRESS', updated_at = '2026-08-20T01:00:00Z'
                    WHERE checklist_instance_id = '%s'
                    """.formatted(progressedParent));
            statement.execute("""
                    INSERT INTO public.audit_events
                        (actor_user_id, event_category, resource_type, resource_id,
                         purpose, decision, event_origin, payload,
                         checklist_task_instance_id)
                    VALUES
                        ('10000000-0000-0000-0000-000000000004',
                         'LEGACY_PROGRESS_SENTINEL', 'CHECKLIST_INSTANCE', '%s',
                         'CHK_022_FIXTURE', 'RECORDED', 'AUDIT_LOG',
                         '{"metadata":"CONTROLLED"}'::jsonb,
                         '73000000-0000-0000-0000-000000000005')
                    """.formatted(progressedParent));
            statement.execute("""
                    UPDATE public.checklist_task_instances
                    SET title_snapshot = 'Tampered immutable title'
                    WHERE checklist_task_instance_id =
                        '73000000-0000-0000-0000-000000000010'
                    """);
            statement.execute("""
                    INSERT INTO public.checklist_instances
                        (checklist_instance_id, distribution_key, key_version,
                         recipient_user_id, recipient_role, care_group_id,
                         care_context_type, care_context_id, context_owner_user_id,
                         origin, status, created_at, updated_at)
                    VALUES
                        ('%s', '%s', 'v1',
                         '10000000-0000-0000-0000-000000000004', 'MOTHER',
                         '50000000-0000-0000-0000-000000000001',
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'USER_CREATED', 'PENDING',
                         '2026-11-01T00:00:00Z', '2026-11-01T00:00:00Z')
                    """.formatted(DRIFT_PARENT, DRIFT_DISTRIBUTION_KEY));
            statement.execute("""
                    INSERT INTO public.checklist_task_instances
                        (checklist_task_instance_id, checklist_instance_id,
                         task_key, title_snapshot, target_subject, status,
                         completed_at, created_at, updated_at)
                    VALUES
                        ('75000000-0000-0000-0000-000000000001', '%s',
                         '%s', 'Drift parent child sentinel', 'MOTHER', 'COMPLETED',
                         '2026-11-02T03:00:00Z',
                         '2026-11-02T01:00:00Z', '2026-11-02T03:00:00Z')
                    """.formatted(DRIFT_PARENT, "b".repeat(64)));
            statement.execute("""
                    INSERT INTO public.checklist_instances
                        (checklist_instance_id, distribution_key, key_version,
                         template_lineage_id, template_version_id,
                         recipient_user_id, recipient_role, care_group_id,
                         care_context_type, care_context_id, context_owner_user_id,
                         origin, window_start, window_end, status,
                         cancelled_at, cancellation_reason_code, created_at, updated_at)
                    VALUES
                        ('%s', '%s', 'v1',
                         '72000000-0000-0000-0000-000000000002',
                         '72000000-0000-0000-0000-000000000003',
                         '10000000-0000-0000-0000-000000000004', 'MOTHER',
                         '50000000-0000-0000-0000-000000000001',
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'SYSTEM_TEMPLATE', DATE '2026-12-01', DATE '2026-12-01',
                         'CANCELLED', '2026-12-01T01:00:00Z', 'LEGACY_CANCELLED',
                         '2026-12-01T01:00:00Z', '2026-12-01T01:00:00Z')
                    """.formatted(TASK_DRIFT_PARENT, TASK_DRIFT_DISTRIBUTION_KEY));
            statement.execute("""
                    INSERT INTO public.audit_events
                        (actor_user_id, event_category, subject_user_id,
                         event_origin, payload, correlation_id,
                         actor_type, actor_service, reason_code,
                         care_context_type, care_context_id, template_version_id)
                    VALUES
                        (NULL, 'CHECKLIST_CANCELLED',
                         '10000000-0000-0000-0000-000000000004',
                         'AUDIT_LOG', '{"metadata":"CONTROLLED"}'::jsonb,
                         '74000000-0000-0000-0000-000000000010',
                         'SERVICE', 'CHECKLIST_DISTRIBUTION', 'LEGACY_CANCELLED',
                         'JOURNEY', '40000000-0000-0000-0000-000000000001',
                         '72000000-0000-0000-0000-000000000003')
                    """);
        }
        return progressedParent;
    }

    private static void assertSystemOccurrences(Connection connection) throws Exception {
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances
                WHERE template_version_id = '72000000-0000-0000-0000-000000000003'
                  AND origin = 'SYSTEM_TEMPLATE'
                  AND window_start IN (DATE '2026-08-01', DATE '2026-09-01')
                """)).isEqualTo(2L);
        assertThat(textValues(connection, """
                SELECT window_start::text FROM public.checklist_instances
                WHERE template_version_id = '72000000-0000-0000-0000-000000000003'
                  AND window_start IS NOT NULL
                  AND window_start IN (DATE '2026-08-01', DATE '2026-09-01')
                ORDER BY window_start
                """)).containsExactly("2026-08-01", "2026-09-01");
        assertThat(textValues(connection, """
                SELECT trim(distribution_key)
                FROM public.checklist_instances
                WHERE template_version_id = '72000000-0000-0000-0000-000000000003'
                  AND window_start = DATE '2026-08-01'
                """))
                .containsExactly(ChecklistDistributionKeyFactory.instanceKey(
                        VERSION, OWNER, "MOTHER", GROUP, "JOURNEY", JOURNEY,
                        "2026-08-01", "2026-08-01"));
        assertThat(textValues(connection, """
                SELECT task.checklist_task_instance_id::text
                FROM public.checklist_task_instances task
                JOIN public.checklist_instances parent
                  ON parent.checklist_instance_id = task.checklist_instance_id
                WHERE parent.template_version_id = '72000000-0000-0000-0000-000000000003'
                  AND parent.window_start = DATE '2026-08-01'
                ORDER BY task.display_order
                """)).containsExactly(OCCURRENCE_A_ITEM_TWO.toString(), OCCURRENCE_A_ITEM_ONE.toString());
        assertThat(textValues(connection, """
                SELECT task.checklist_task_instance_id::text
                FROM public.checklist_task_instances task
                JOIN public.checklist_instances parent
                  ON parent.checklist_instance_id = task.checklist_instance_id
                WHERE parent.template_version_id = '72000000-0000-0000-0000-000000000003'
                  AND parent.window_start = DATE '2026-09-01'
                ORDER BY task.display_order
                """)).containsExactly(OCCURRENCE_B_ITEM_ONE.toString(), OCCURRENCE_B_ITEM_TWO.toString());
    }

    private static void assertStandaloneCustomRows(Connection connection) throws Exception {
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances parent
                JOIN public.checklist_task_instances task
                  ON task.checklist_instance_id = parent.checklist_instance_id
                WHERE parent.origin = 'USER_CREATED'
                  AND task.checklist_task_instance_id IN
                      ('73000000-0000-0000-0000-000000000005',
                       '73000000-0000-0000-0000-000000000006')
                """)).isEqualTo(2L);
        assertThat(longValue(connection, """
                SELECT count(DISTINCT task.checklist_instance_id)
                FROM public.checklist_task_instances task
                WHERE task.checklist_task_instance_id IN
                      ('73000000-0000-0000-0000-000000000005',
                       '73000000-0000-0000-0000-000000000006')
                """)).isEqualTo(2L);
        assertThat(longValue(connection, """
                SELECT count(*)
                FROM public.checklist_task_instances task
                JOIN public.checklist_instances parent
                  ON parent.checklist_instance_id = task.checklist_instance_id
                WHERE task.checklist_task_instance_id =
                      '73000000-0000-0000-0000-000000000005'
                  AND task.status = 'IN_PROGRESS'
                  AND task.updated_at = '2026-08-20T01:00:00Z'
                  AND parent.status = 'IN_PROGRESS'
                """)).isEqualTo(1L);
    }

    private static void assertMixedTerminalOccurrence(Connection connection) throws Exception {
        assertThat(longValue(connection, """
                SELECT count(*)
                FROM public.checklist_instances parent
                WHERE parent.template_version_id =
                      '72000000-0000-0000-0000-000000000003'
                  AND parent.window_start = DATE '2027-01-01'
                  AND parent.status = 'COMPLETED'
                  AND parent.completed_at = '2027-01-01T05:00:00Z'
                  AND parent.cancelled_at IS NULL
                  AND parent.cancellation_reason_code IS NULL
                """)).isEqualTo(1L);
        assertThat(longValue(connection, """
                SELECT count(*)
                FROM public.checklist_task_instances
                WHERE checklist_task_instance_id IN
                      ('73000000-0000-0000-0000-000000000011',
                       '73000000-0000-0000-0000-000000000012',
                       '73000000-0000-0000-0000-000000000013')
                  AND ((status = 'COMPLETED'
                        AND completed_at = '2027-01-01T03:00:00Z'
                        AND skipped_at IS NULL AND cancelled_at IS NULL
                        AND action_reason_code IS NULL)
                    OR (status = 'CANCELLED'
                        AND cancelled_at = '2027-01-01T04:00:00Z'
                        AND completed_at IS NULL AND skipped_at IS NULL
                        AND action_reason_code = 'LEGACY_CANCELLED')
                    OR (status = 'SKIPPED'
                        AND skipped_at = '2027-01-01T05:00:00Z'
                        AND completed_at IS NULL AND cancelled_at IS NULL
                        AND action_reason_code = 'LEGACY_SKIPPED'))
                """)).isEqualTo(3L);
    }

    private static void assertCollisionQuarantineAndAudit(Connection connection) throws Exception {
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_task_instances
                WHERE checklist_task_instance_id IN
                      ('73000000-0000-0000-0000-000000000007',
                       '73000000-0000-0000-0000-000000000008')
                """)).isZero();
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_migration_quarantine
                WHERE source_id IN
                      ('73000000-0000-0000-0000-000000000007',
                       '73000000-0000-0000-0000-000000000008')
                  AND reason_code = 'LEGACY_OCCURRENCE_COLLISION'
                """)).isEqualTo(2L);
        assertThat(longValue(connection, """
                SELECT count(*)
                FROM public.checklist_migration_quarantine quarantine
                JOIN public.audit_events audit
                  ON audit.correlation_id = quarantine.correlation_id
                 AND audit.event_category = 'CHECKLIST_MIGRATION_QUARANTINED'
                WHERE quarantine.source_id IN
                      ('73000000-0000-0000-0000-000000000007',
                       '73000000-0000-0000-0000-000000000008')
                  AND quarantine.reason_code = 'LEGACY_OCCURRENCE_COLLISION'
                """)).isEqualTo(2L);
    }

    private static void assertDriftAndHistorySafety(
            Connection connection, UUID progressedLegacyParent) throws Exception {
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances
                WHERE checklist_instance_id = '%s'
                  AND distribution_key = '%s'
                  AND key_version = 'v1'
                  AND template_lineage_id IS NULL
                  AND template_version_id IS NULL
                  AND recipient_user_id = '10000000-0000-0000-0000-000000000004'
                  AND recipient_role = 'MOTHER'
                  AND care_group_id = '50000000-0000-0000-0000-000000000001'
                  AND care_context_type = 'JOURNEY'
                  AND care_context_id = '40000000-0000-0000-0000-000000000001'
                  AND context_owner_user_id = '10000000-0000-0000-0000-000000000004'
                  AND origin = 'USER_CREATED'
                  AND window_start IS NULL
                  AND window_end IS NULL
                  AND status = 'PENDING'
                  AND completed_at IS NULL
                  AND cancelled_at IS NULL
                  AND cancellation_reason_code IS NULL
                  AND lock_version = 0
                  AND created_at = '2026-11-01T00:00:00Z'
                  AND updated_at = '2026-11-01T00:00:00Z'
                """.formatted(DRIFT_PARENT, DRIFT_DISTRIBUTION_KEY))).isEqualTo(1L);
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_task_instances
                WHERE checklist_task_instance_id =
                      '75000000-0000-0000-0000-000000000001'
                  AND checklist_instance_id = '%s'
                  AND template_version_id IS NULL
                  AND template_item_version_id IS NULL
                  AND task_key = '%s'
                  AND key_version = 'v1'
                  AND title_snapshot = 'Drift parent child sentinel'
                  AND display_order = 0
                  AND is_required = false
                  AND target_subject = 'MOTHER'
                  AND due_at IS NULL
                  AND status = 'COMPLETED'
                  AND lock_version = 0
                  AND completed_at = '2026-11-02T03:00:00Z'
                  AND skipped_at IS NULL
                  AND cancelled_at IS NULL
                  AND action_reason_code IS NULL
                  AND created_at = '2026-11-02T01:00:00Z'
                  AND updated_at = '2026-11-02T03:00:00Z'
                """.formatted(DRIFT_PARENT, "b".repeat(64)))).isEqualTo(1L);
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_migration_quarantine
                WHERE source_id = '73000000-0000-0000-0000-000000000009'
                  AND reason_code = 'LEGACY_PARENT_PAYLOAD_DRIFT'
                """)).isEqualTo(1L);
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances parent
                WHERE parent.checklist_instance_id = '%s'
                  AND parent.distribution_key = '%s'
                  AND parent.status = 'CANCELLED'
                  AND parent.cancellation_reason_code = 'LEGACY_CANCELLED'
                  AND NOT EXISTS (
                      SELECT 1 FROM public.checklist_task_instances child
                      WHERE child.checklist_instance_id = parent.checklist_instance_id)
                """.formatted(TASK_DRIFT_PARENT, TASK_DRIFT_DISTRIBUTION_KEY))).isEqualTo(1L);
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.audit_events
                WHERE event_category = 'CHECKLIST_CANCELLED'
                  AND correlation_id = '74000000-0000-0000-0000-000000000010'
                  AND checklist_task_instance_id IS NULL
                  AND resource_id IS NULL
                """)).isEqualTo(1L);
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances parent
                WHERE parent.checklist_instance_id = '%s'
                  AND parent.status = 'IN_PROGRESS'
                  AND NOT EXISTS (
                      SELECT 1 FROM public.checklist_task_instances child
                      WHERE child.checklist_instance_id = parent.checklist_instance_id)
                  AND EXISTS (
                      SELECT 1 FROM public.audit_events audit
                      WHERE audit.resource_type = 'CHECKLIST_INSTANCE'
                        AND audit.resource_id = parent.checklist_instance_id)
                """.formatted(progressedLegacyParent))).isEqualTo(1L);
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_task_instances
                WHERE checklist_task_instance_id =
                      '73000000-0000-0000-0000-000000000010'
                  AND title_snapshot = 'Tampered immutable title'
                """)).isEqualTo(1L);
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_migration_quarantine
                WHERE source_id = '73000000-0000-0000-0000-000000000010'
                  AND reason_code = 'LEGACY_TASK_PAYLOAD_DRIFT'
                """)).isEqualTo(1L);
    }

    private static void assertSourceAndOrphanSafety(
            Connection connection, UUID progressedLegacyParent) throws Exception {
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.preparation_checklist_items
                WHERE checklist_item_id BETWEEN
                      '73000000-0000-0000-0000-000000000001'::uuid AND
                      '73000000-0000-0000-0000-000000000013'::uuid
                """)).isEqualTo(13L);
        assertThat(longValue(connection, """
                SELECT count(*) FROM public.checklist_instances parent
                LEFT JOIN public.checklist_task_instances task
                  ON task.checklist_instance_id = parent.checklist_instance_id
                WHERE parent.recipient_user_id = '10000000-0000-0000-0000-000000000004'
                  AND parent.created_at >= '2026-08-01T00:00:00Z'
                  AND parent.status <> 'CANCELLED'
                  AND parent.checklist_instance_id NOT IN ('%s', '%s', '%s')
                GROUP BY parent.checklist_instance_id
                HAVING count(task.checklist_task_instance_id) = 0
                """.formatted(DRIFT_PARENT, TASK_DRIFT_PARENT, progressedLegacyParent))).isZero();
    }

    private static long longValue(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            return result.next() ? result.getLong(1) : 0L;
        }
    }

    private static List<String> textValues(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            var values = new java.util.ArrayList<String>();
            while (result.next()) {
                values.add(result.getString(1));
            }
            return List.copyOf(values);
        }
    }

    private static UUID uuidValue(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            if (!result.next()) {
                throw new AssertionError("Expected one UUID row");
            }
            return result.getObject(1, UUID.class);
        }
    }

    private static UUID legacyParentId(String distributionKey) {
        String hash = v1Hash("LEGACY_PARENT_ID", distributionKey);
        return UUID.fromString(hash.substring(0, 8) + "-" + hash.substring(8, 12) + "-"
                + hash.substring(12, 16) + "-" + hash.substring(16, 20) + "-"
                + hash.substring(20, 32));
    }

    private static String v1Hash(String... tokens) {
        StringBuilder canonical = new StringBuilder("v1");
        for (String token : tokens) {
            byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
            canonical.append(bytes.length).append(':').append(token);
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static UUID source(int suffix) {
        return UUID.fromString("73000000-0000-0000-0000-%012d".formatted(suffix));
    }
}
