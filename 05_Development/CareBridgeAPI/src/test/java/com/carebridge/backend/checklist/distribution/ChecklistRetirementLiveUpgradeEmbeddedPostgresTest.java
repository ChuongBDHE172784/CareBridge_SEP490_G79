package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.testsupport.CanonicalUserFixture;
import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledOnOs(OS.WINDOWS)
class ChecklistRetirementLiveUpgradeEmbeddedPostgresTest {

    private static final String PRE_RETIREMENT_VERSION = "20260731020000";
    private static final String OPERATOR_ATTESTATION =
            "REQUEST_100_PARITY_WRITERS_FROZEN_CATALOG_AND_LEDGER_CAPTURED_V1";
    private static final Path LEGACY_FINALIZER = Path.of(
            "..", "Deployment", "database", "finalizers",
            "V20260729150001__finalize_checklist_retention_security.sql");
    private static final Path PRE_FINALIZER = Path.of(
            "src", "main", "resources", "db", "migration",
            "checklist_retirement_pre_finalizer.sql");
    private static final Path POST_FINALIZER = Path.of(
            "src", "main", "resources", "db", "migration",
            "checklist_retirement_post_finalizer.sql");

    @Test
    @Timeout(240)
    void finalizedLiveUpgradePreservesCoreDataAndActionLedgerWhileRetiringSupportCatalog()
            throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);

            Flyway preRetirement = flyway(dataSource, PRE_RETIREMENT_VERSION);
            assertThat(preRetirement.migrate().success).isTrue();

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            RetainedFixture fixture = seedRetainedFixture(jdbc);
            seedApprovedQuarantineDisposalFixture(jdbc);

            List<UUID> parentIdsBefore = uuidList(
                    jdbc, "select checklist_instance_id from checklist_instances order by 1");
            List<UUID> taskIdsBefore = uuidList(
                    jdbc, "select checklist_task_instance_id from checklist_task_instances order by 1");
            String parentDigestBefore = digest(jdbc, """
                    select concat_ws('|', checklist_instance_id::text, distribution_key,
                               key_version, recipient_user_id::text, recipient_role,
                               coalesce(care_group_id::text, ''), care_context_type,
                               care_context_id::text, context_owner_user_id::text,
                               origin, status, lock_version::text,
                               coalesce(completed_at::text, ''), coalesce(cancelled_at::text, '')) as row_value
                      from checklist_instances order by checklist_instance_id
                    """);
            String taskDigestBefore = digest(jdbc, """
                    select concat_ws('|', checklist_task_instance_id::text,
                               checklist_instance_id::text, task_key, key_version,
                               title_snapshot, display_order::text, is_required::text,
                               target_subject, status, lock_version::text,
                               coalesce(completed_at::text, ''), coalesce(skipped_at::text, ''),
                               coalesce(cancelled_at::text, '')) as row_value
                      from checklist_task_instances order by checklist_task_instance_id
                    """);
            String ledgerDigestBefore = digest(jdbc, """
                    select concat_ws('|', checklist_action_command_id::text,
                               actor_user_id::text, task_kind, task_id::text,
                               client_request_id::text, payload_hash, action_type,
                               result_status, result_jsonb::text, applied_at::text,
                               retain_until::text, legal_hold::text, created_at::text) as row_value
                      from checklist_action_commands order by checklist_action_command_id
                    """);
            String quarantineDigestBefore = digest(jdbc, """
                    select concat_ws('|', quarantine_id::text, source_table,
                               coalesce(source_id::text, ''), reason_code, payload_hash,
                               encryption_key_version, correlation_id::text,
                               legal_hold::text, retain_until::text, created_at::text) as row_value
                      from checklist_migration_quarantine order by quarantine_id
                    """);
            assertThat(quarantineDigestBefore).hasSize(64);
            assertThat(jdbc.queryForObject(
                    "select count(*) from checklist_migration_quarantine", Integer.class))
                    .isEqualTo(6);

            runSqlScript(dataSource, LEGACY_FINALIZER, null, null);
            String migrationRole = jdbc.queryForObject("select current_user", String.class);
            assertThatThrownBy(() -> runSqlScript(
                    dataSource, PRE_FINALIZER, migrationRole, "WRONG_ATTESTATION"))
                    .isInstanceOf(java.sql.SQLException.class)
                    .hasMessageContaining("CHECKLIST_RETIREMENT_OPERATOR_ATTESTATION_REQUIRED");
            runSqlScript(dataSource, PRE_FINALIZER, migrationRole, OPERATOR_ATTESTATION);

            Flyway retired = flyway(dataSource, null);
            UUID overflowQuarantineId = UUID.randomUUID();
            jdbc.update("""
                    insert into checklist_migration_quarantine (
                        quarantine_id, source_table, source_id, reason_code,
                        payload_ciphertext, payload_hash, encryption_key_version,
                        correlation_id, legal_hold, retain_until, created_at)
                    values (?, 'synthetic_retirement_fixture', ?, 'LIMIT_EXCEEDED',
                            decode('01020304', 'hex'), ?, 'synthetic-v1', ?,
                            false, now() - interval '1 day', now() - interval '8 years')
                    """, overflowQuarantineId, UUID.randomUUID(), "f".repeat(64), UUID.randomUUID());
            assertThatThrownBy(retired::migrate)
                    .hasMessageContaining("CHECKLIST_RETIREMENT_QUARANTINE_LIMIT_EXCEEDED");
            deleteEligibleQuarantineAsRetentionOwner(dataSource, overflowQuarantineId);

            UUID outboxId = UUID.randomUUID();
            jdbc.update("""
                    insert into checklist_distribution_outbox (
                        outbox_event_id, event_type, aggregate_type, aggregate_id,
                        payload_jsonb, correlation_id, occurred_at)
                    values (?, 'RETIREMENT_REHEARSAL', 'CHECKLIST', ?, '{}'::jsonb, ?, now())
                    """, outboxId, UUID.randomUUID(), UUID.randomUUID());
            assertThatThrownBy(retired::migrate)
                    .hasMessageContaining("CHECKLIST_RETIREMENT_OUTBOX_NOT_DRAINED");
            jdbc.update("update checklist_distribution_outbox set processed_at=now() where outbox_event_id=?",
                    outboxId);

            UUID runId = UUID.randomUUID();
            jdbc.update("""
                    insert into checklist_reconciliation_runs (
                        reconciliation_run_id, correlation_id, trigger_type, status)
                    values (?, ?, 'REPLAY', 'RUNNING')
                    """, runId, UUID.randomUUID());
            assertThatThrownBy(retired::migrate)
                    .hasMessageContaining("CHECKLIST_RETIREMENT_RECONCILIATION_RUN_ACTIVE");
            jdbc.update("""
                    update checklist_reconciliation_runs
                       set status='SUCCEEDED', completed_at=now()
                     where reconciliation_run_id=?
                    """, runId);

            UUID candidateId = UUID.randomUUID();
            jdbc.update("""
                    insert into checklist_reconciliation_candidates (
                        reconciliation_candidate_id, reconciliation_run_id, outcome)
                    values (?, ?, 'PENDING')
                    """, candidateId, runId);
            assertThatThrownBy(retired::migrate)
                    .hasMessageContaining("CHECKLIST_RETIREMENT_RECONCILIATION_CANDIDATE_PENDING");
            jdbc.update("""
                    update checklist_reconciliation_candidates
                       set outcome='FAILED', completed_at=now(), failure_code='REHEARSAL_SETTLED'
                     where reconciliation_candidate_id=?
                    """, candidateId);

            jdbc.execute("""
                    alter table checklist_action_commands disable trigger
                        checklist_action_command_retention_guard_trg
                    """);
            assertThatThrownBy(retired::migrate)
                    .hasMessageContaining("CHECKLIST_RETIREMENT_ACTION_LEDGER_GUARD_MISSING");
            jdbc.execute("""
                    alter table checklist_action_commands enable trigger
                        checklist_action_command_retention_guard_trg
                    """);

            assertThat(retired.migrate().success).isTrue();
            runSqlScript(dataSource, POST_FINALIZER, null, null);

            assertThat(retired.validateWithResult().validationSuccessful).isTrue();
            assertThat(retired.migrate().migrationsExecuted).isZero();
            assertThat(uuidList(
                    jdbc, "select checklist_instance_id from checklist_instances order by 1"))
                    .containsExactlyElementsOf(parentIdsBefore)
                    .containsExactly(fixture.parentId());
            assertThat(uuidList(
                    jdbc, "select checklist_task_instance_id from checklist_task_instances order by 1"))
                    .containsExactlyElementsOf(taskIdsBefore)
                    .containsExactly(fixture.taskId());
            assertThat(digest(jdbc, """
                    select concat_ws('|', checklist_instance_id::text, distribution_key,
                               key_version, recipient_user_id::text, recipient_role,
                               coalesce(care_group_id::text, ''), care_context_type,
                               care_context_id::text, context_owner_user_id::text,
                               origin, status, lock_version::text,
                               coalesce(completed_at::text, ''), coalesce(cancelled_at::text, '')) as row_value
                      from checklist_instances order by checklist_instance_id
                    """)).isEqualTo(parentDigestBefore);
            assertThat(digest(jdbc, """
                    select concat_ws('|', checklist_task_instance_id::text,
                               checklist_instance_id::text, task_key, key_version,
                               title_snapshot, display_order::text, is_required::text,
                               target_subject, status, lock_version::text,
                               coalesce(completed_at::text, ''), coalesce(skipped_at::text, ''),
                               coalesce(cancelled_at::text, '')) as row_value
                      from checklist_task_instances order by checklist_task_instance_id
                    """)).isEqualTo(taskDigestBefore);
            assertThat(digest(jdbc, """
                    select concat_ws('|', checklist_action_command_id::text,
                               actor_user_id::text, task_kind, task_id::text,
                               client_request_id::text, payload_hash, action_type,
                               result_status, result_jsonb::text, applied_at::text,
                               retain_until::text, legal_hold::text, created_at::text) as row_value
                      from checklist_action_commands order by checklist_action_command_id
                    """)).isEqualTo(ledgerDigestBefore);

            assertThat(jdbc.queryForList("""
                    select table_name
                      from information_schema.tables
                     where table_schema='public'
                       and table_type='BASE TABLE'
                       and table_name like 'checklist_%'
                     order by table_name
                    """, String.class)).containsExactly(
                    "checklist_action_commands",
                    "checklist_instances",
                    "checklist_task_instances");
            assertThat(jdbc.queryForObject(
                    "select to_regprocedure('public.checklist_assert_retention_security()') is null",
                    Boolean.class)).isTrue();
            assertThat(jdbc.queryForObject("""
                    select count(*)
                      from pg_catalog.pg_proc routine
                     where routine.oid in (
                         to_regprocedure('public.checklist_action_command_retention_guard()'),
                         to_regprocedure('public.checklist_validate_action_command_target()'))
                    """, Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("""
                    select owner_role.rolname
                      from pg_catalog.pg_proc routine
                      join pg_catalog.pg_roles owner_role on owner_role.oid=routine.proowner
                     where routine.oid='public.checklist_purge_retained_records(uuid)'::regprocedure
                    """, String.class)).isEqualTo("carebridge_checklist_retention_owner");
        }
    }

    private static void deleteEligibleQuarantineAsRetentionOwner(
            DataSource dataSource,
            UUID quarantineId) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var role = connection.createStatement()) {
                role.execute("set local role carebridge_checklist_retention_owner");
            }
            try (var deletion = connection.prepareStatement(
                    "delete from checklist_migration_quarantine where quarantine_id=?")) {
                deletion.setObject(1, quarantineId);
                assertThat(deletion.executeUpdate()).isEqualTo(1);
            }
            connection.commit();
        }
    }

    private static RetainedFixture seedRetainedFixture(JdbcTemplate jdbc) {
        UUID actorId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(jdbc, actorId, "Retirement Mother", "0900000041", "MOTHER");
        jdbc.update("""
                insert into care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                values (?, ?, ?, 'MOTHER', 'Retirement Mother', 'ACTIVE', now(), now())
                """, subjectId, actorId, actorId);
        jdbc.update("""
                insert into mother_journeys (
                    journey_id, owner_user_id, care_subject_id, journey_type,
                    start_date, last_menstrual_date, estimated_due_date,
                    status, version, created_at, updated_at)
                values (?, ?, ?, 'PREGNANCY', current_date - 28, current_date - 28,
                        current_date + 252, 'ACTIVE', 0, now(), now())
                """, journeyId, actorId, subjectId);
        jdbc.update(
                "update care_subjects set mother_journey_id=? where care_subject_id=?",
                journeyId, subjectId);
        jdbc.update("""
                insert into checklist_instances (
                    checklist_instance_id, distribution_key, key_version,
                    recipient_user_id, recipient_role, care_group_id,
                    care_context_type, care_context_id, context_owner_user_id,
                    origin, status, lock_version, completed_at, created_at, updated_at)
                values (?, ?, 'v1', ?, 'MOTHER', null, 'JOURNEY', ?, ?,
                        'USER_CREATED', 'COMPLETED', 0, now(), now(), now())
                """, parentId, "a".repeat(64), actorId, journeyId, actorId);
        jdbc.update("""
                insert into checklist_task_instances (
                    checklist_task_instance_id, checklist_instance_id, task_key,
                    key_version, title_snapshot, display_order, is_required,
                    target_subject, status, lock_version, completed_at, created_at, updated_at)
                values (?, ?, ?, 'v1', 'Retained task', 0, true,
                        'MOTHER', 'COMPLETED', 0, now(), now(), now())
                """, taskId, parentId, "b".repeat(64));
        jdbc.update("""
                insert into checklist_action_commands (
                    checklist_action_command_id, actor_user_id, task_kind, task_id,
                    client_request_id, payload_hash, action_type, result_status,
                    result_jsonb, applied_at, retain_until, legal_hold, created_at)
                values (?, ?, 'CHECKLIST', ?, ?, ?, 'COMPLETE', 'APPLIED',
                        '{"status":"COMPLETED"}'::jsonb, now(), now() + interval '8 years',
                        false, now())
                """, UUID.randomUUID(), actorId, taskId, UUID.randomUUID(), "c".repeat(64));
        return new RetainedFixture(parentId, taskId);
    }

    private static void seedApprovedQuarantineDisposalFixture(JdbcTemplate jdbc) {
        Integer existing = jdbc.queryForObject(
                "select count(*) from checklist_migration_quarantine where not legal_hold",
                Integer.class);
        assertThat(existing).isNotNull().isBetween(0, 6);
        assertThat(jdbc.queryForObject(
                "select count(*) from checklist_migration_quarantine where legal_hold",
                Integer.class)).isZero();
        for (int index = existing; index < 6; index++) {
            jdbc.update("""
                    insert into checklist_migration_quarantine (
                        quarantine_id, source_table, source_id, reason_code,
                        payload_ciphertext, payload_hash, encryption_key_version,
                        correlation_id, legal_hold, retain_until, created_at)
                    values (?, 'synthetic_retirement_fixture', ?, 'APPROVED_DISPOSAL',
                            decode('01020304', 'hex'), ?, 'synthetic-v1', ?,
                            false, now() + interval '8 years', now())
                    """, UUID.randomUUID(), UUID.randomUUID(),
                    String.format("%064x", index + 1), UUID.randomUUID());
        }
    }

    private static String digest(JdbcTemplate jdbc, String orderedRowsSql) {
        return jdbc.queryForObject("""
                select encode(sha256(convert_to(coalesce(string_agg(row_value, E'\\n'), ''),
                                                'UTF8')), 'hex')
                  from (%s) ordered_rows
                """.formatted(orderedRowsSql), String.class);
    }

    private static List<UUID> uuidList(JdbcTemplate jdbc, String sql) {
        return jdbc.query(sql, (result, row) -> result.getObject(1, UUID.class));
    }

    private static void runSqlScript(DataSource dataSource, Path path, String flywayRole,
                                     String operatorAttestation)
            throws Exception {
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

    private static Flyway flyway(DataSource dataSource, String target) {
        var configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration-legacy")
                .cleanDisabled(true)
                .outOfOrder(false)
                .validateOnMigrate(true)
                .ignoreMigrationPatterns("*:future");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private record RetainedFixture(UUID parentId, UUID taskId) {
    }
}
