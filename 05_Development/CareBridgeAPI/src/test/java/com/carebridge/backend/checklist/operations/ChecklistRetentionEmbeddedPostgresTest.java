package com.carebridge.backend.checklist.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import javax.sql.DataSource;
import org.assertj.core.api.SoftAssertions;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** CHK-039 retention barrier against a real Docker-free PostgreSQL 18 process. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistRetentionEmbeddedPostgresTest {

    private static final String OPERATIONS_ACTOR = "10000000-0000-0000-0000-000000000001";
    private static final String NORMAL_ACTOR = "10000000-0000-0000-0000-000000000004";
    private static final String OPERATIONS_DB_ROLE = "checklist_operations";
    private static final String APPLICATION_DB_ROLE = "carebridge_application";

    @Test
    @Timeout(180)
    void chk039_purgeHonorsCreatedAtLegalHoldAndActiveTaskBarrierAndAuditsAtomically() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .start()) {
            var dataSource = postgres.getPostgresDatabase();
            createDatabaseCallerRoles(dataSource);
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            runRetentionPostFinalizer(dataSource);

            try (Connection connection = dataSource.getConnection()) {
                seedRetentionRows(connection);
                assertRuntimeApplicationRolePath(postgres);
                assertDirectRetentionAttacksAreRejected(postgres, connection);
                try (Connection operationsConnection = postgres
                        .getDatabase(OPERATIONS_DB_ROLE, "postgres")
                        .getConnection()) {
                    assertUntrustedPurgeActorRejected(operationsConnection);
                    callPurge(operationsConnection);
                }

                assertThat(count(connection, """
                        SELECT count(*) FROM public.audit_events
                        WHERE audit_event_id = '92000000-0000-0000-0000-000000000001'
                        """)).isZero();
                assertThat(count(connection, """
                        SELECT count(*) FROM public.audit_events
                        WHERE audit_event_id = '92000000-0000-0000-0000-000000000002'
                        """)).isOne();
                assertThat(count(connection, """
                        SELECT count(*) FROM public.audit_events
                        WHERE audit_event_id = '92000000-0000-0000-0000-000000000003'
                        """)).isOne();
                assertThat(count(connection, """
                        SELECT count(*) FROM public.audit_events
                        WHERE audit_event_id = '92000000-0000-0000-0000-000000000004'
                        """)).isZero();
                assertThat(count(connection, """
                        SELECT count(*) FROM public.checklist_action_commands
                        WHERE checklist_action_command_id = '92000000-0000-0000-0000-000000000021'
                        """)).isZero();
                assertThat(count(connection, """
                        SELECT count(*) FROM public.checklist_action_commands
                        WHERE checklist_action_command_id IN
                            ('92000000-0000-0000-0000-000000000022',
                             '92000000-0000-0000-0000-000000000023',
                             '92000000-0000-0000-0000-000000000025')
                        """)).isEqualTo(3L);
                assertThat(count(connection, """
                        SELECT count(*) FROM public.checklist_action_commands
                        WHERE checklist_action_command_id = '92000000-0000-0000-0000-000000000024'
                        """)).isZero();
                assertThat(count(connection, """
                        SELECT count(*) FROM public.audit_events
                        WHERE event_category = 'CHECKLIST_RETENTION_PURGED'
                          AND actor_user_id = '10000000-0000-0000-0000-000000000001'
                          AND (after_payload_jsonb->>'auditEventsPurged')::bigint >= 1
                          AND (after_payload_jsonb->>'quarantinesPurged')::bigint = 0
                          AND (after_payload_jsonb->>'actionCommandsPurged')::bigint >= 1
                        """)).isOne();

                assertPurgeAuditFailureRollsBack(postgres, connection);
            }
        }
    }

    @Test
    @Timeout(180)
    void chk039_v15000RunsAsLeastPrivilegeMigratorWithPreprovisionedRoles() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .start()) {
            DataSource rootDataSource = postgres.getPostgresDatabase();
            createDatabaseCallerRoles(rootDataSource);
            createLeastPrivilegeMigrator(rootDataSource);

            Flyway.configure()
                    .dataSource(rootDataSource)
                    .locations("classpath:db/migration")
                    .locations("classpath:db/migration-legacy").target("20260729140000")
                    .load()
                    .migrate();

            prepareLeastPrivilegeMigrator(rootDataSource);
            DataSource migratorDataSource = postgres.getDatabase(
                    "checklist_retention_migrator", "postgres");
            Flyway.configure()
                    .dataSource(migratorDataSource)
                    .locations("classpath:db/migration")
                    .locations("classpath:db/migration-legacy").target("20260729150000")
                    .load()
                    .migrate();
            alterCurrentDatabaseOwner(rootDataSource, "checklist_retention_migrator");
            assertCurrentDatabaseOwner(
                    rootDataSource, "checklist_retention_migrator", true);

            try (Connection connection = migratorDataSource.getConnection()) {
                assertThat(count(connection, """
                        SELECT count(*)
                        FROM public.flyway_schema_history
                        WHERE version = '20260729150000' AND success = true
                        """)).isOne();
                assertThat(count(connection, """
                        SELECT count(*)
                        FROM pg_catalog.pg_roles
                        WHERE rolname = current_user
                          AND rolcreaterole = false
                          AND rolsuper = false
                        """)).isOne();
                assertThat(count(connection, """
                        SELECT count(*)
                        FROM pg_catalog.pg_proc routine
                        JOIN pg_catalog.pg_roles owner_role
                          ON owner_role.oid = routine.proowner
                        WHERE routine.proname = 'checklist_purge_retained_records'
                          AND owner_role.rolname = 'checklist_retention_migrator'
                        """)).isOne();
                assertThat(count(connection, """
                        SELECT count(*)
                        FROM pg_catalog.pg_auth_members membership
                        JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = membership.roleid
                        WHERE owner_role.rolname = 'carebridge_checklist_retention_owner'
                        """)).isZero();
                assertThat(count(connection, """
                        SELECT count(*)
                        WHERE has_schema_privilege(
                            'carebridge_checklist_retention_owner',
                            'public',
                            'CREATE') = false
                        """)).isOne();
            }

            assertThatThrownBy(() -> {
                try (Connection operationsConnection = postgres
                             .getDatabase(OPERATIONS_DB_ROLE, "postgres")
                             .getConnection();
                     var statement = operationsConnection.createStatement()) {
                    statement.executeQuery("""
                            SELECT * FROM public.checklist_purge_retained_records(
                                '10000000-0000-0000-0000-000000000001')
                            """);
                }
            }).isInstanceOf(SQLException.class)
                    .hasMessageContaining("permission denied");

        }
    }

    private static void createDatabaseCallerRoles(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE ROLE carebridge_checklist_retention_owner
                    NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS
                    """);
            statement.execute("""
                    CREATE ROLE carebridge_checklist_schema_owner
                    NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS
                    """);
            statement.execute("""
                    CREATE ROLE checklist_operations
                    LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS
                    """);
            statement.execute("""
                    CREATE ROLE carebridge_application
                    LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS
                    """);
        }
    }

    private static void createLeastPrivilegeMigrator(DataSource rootDataSource) throws Exception {
        try (Connection connection = rootDataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE ROLE checklist_retention_migrator
                    LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS
                    """);
        }
    }

    private static void prepareLeastPrivilegeMigrator(DataSource rootDataSource) throws Exception {
        try (Connection connection = rootDataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("ALTER SCHEMA public OWNER TO checklist_retention_migrator");
            statement.execute("ALTER TABLE public.audit_events OWNER TO checklist_retention_migrator");
            statement.execute("""
                    ALTER TABLE public.checklist_migration_quarantine
                    OWNER TO checklist_retention_migrator
                    """);
            statement.execute("""
                    ALTER TABLE public.checklist_action_commands
                    OWNER TO checklist_retention_migrator
                    """);
            statement.execute("""
                    ALTER TABLE public.flyway_schema_history
                    OWNER TO checklist_retention_migrator
                    """);
            statement.execute("""
                    ALTER FUNCTION public.carebridge_reject_mutation()
                    OWNER TO checklist_retention_migrator
                    """);
            statement.execute("""
                    GRANT SELECT ON public.users,
                                    public.care_tasks,
                                    public.checklist_task_instances
                    TO checklist_retention_migrator WITH GRANT OPTION
                    """);
        }
    }

    private static void alterCurrentDatabaseOwner(DataSource dataSource, String owner)
            throws Exception {
        if (!owner.equals("checklist_retention_migrator")
                && !owner.equals("carebridge_checklist_schema_owner")) {
            throw new IllegalArgumentException("Unexpected database owner fixture: " + owner);
        }
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("ALTER DATABASE postgres OWNER TO " + owner);
        }
    }

    private static void assertCurrentDatabaseOwner(
            DataSource dataSource,
            String expectedOwner,
            boolean canLogin) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(count(connection, """
                    SELECT count(*)
                    FROM pg_catalog.pg_database database_entry
                    JOIN pg_catalog.pg_roles owner_role
                      ON owner_role.oid = database_entry.datdba
                    WHERE database_entry.datname = current_database()
                      AND owner_role.rolname = '%s'
                      AND owner_role.rolcanlogin = %s
                    """.formatted(expectedOwner, canLogin))).isOne();
            assertThat(count(connection, """
                    SELECT count(*)
                    FROM pg_catalog.pg_auth_members membership
                    WHERE membership.roleid = to_regrole('%s')
                       OR membership.member = to_regrole('%s')
                    """.formatted(expectedOwner, expectedOwner))).isZero();
        }
    }

    private static void runRetentionPostFinalizer(DataSource rootDataSource) throws Exception {
        String finalizer = Files.readString(Path.of(
                "src", "main", "resources", "db", "migration",
                "checklist_retirement_post_finalizer.sql"));
        try (Connection connection = rootDataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(finalizer.substring(finalizer.indexOf('\n') + 1));
        }
    }

    private static void assertRuntimeApplicationRolePath(EmbeddedPostgres postgres) throws Exception {
        try (Connection runtimeConnection = postgres
                .getDatabase(APPLICATION_DB_ROLE, "postgres")
                .getConnection()) {
            assertThatThrownBy(() -> {
                try (var statement = runtimeConnection.createStatement()) {
                    statement.executeUpdate("DELETE FROM public.checklist_action_commands");
                }
            }).isInstanceOf(SQLException.class)
                    .hasMessageContaining("permission denied");
            assertThatThrownBy(() -> {
                try (var statement = runtimeConnection.createStatement()) {
                    statement.executeQuery("""
                            SELECT * FROM public.checklist_purge_retained_records(
                                '10000000-0000-0000-0000-000000000001')
                            """);
                }
            }).isInstanceOf(SQLException.class)
                    .hasMessageContaining("permission denied");
        }
    }

    private static void seedRetentionRows(Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO public.care_tasks
                        (task_id, owner_user_id, care_group_id, title, task_type, status,
                         origin, target_subject, created_at, updated_at)
                    VALUES
                        ('92000000-0000-0000-0000-000000000031',
                         '10000000-0000-0000-0000-000000000004',
                         '50000000-0000-0000-0000-000000000001',
                         'terminal retention fixture', 'MANUAL_TASK', 'DONE',
                         'USER_CREATED', 'MOTHER', now(), now()),
                        ('92000000-0000-0000-0000-000000000032',
                         '10000000-0000-0000-0000-000000000004',
                         '50000000-0000-0000-0000-000000000001',
                         'active retention fixture', 'MANUAL_TASK', 'OPEN',
                         'USER_CREATED', 'MOTHER', now(), now()),
                        ('92000000-0000-0000-0000-000000000099',
                         '10000000-0000-0000-0000-000000000004',
                         '50000000-0000-0000-0000-000000000001',
                         'orphan retention fixture', 'MANUAL_TASK', 'DONE',
                         'USER_CREATED', 'MOTHER', now(), now())
                    """);
            statement.execute("""
                    INSERT INTO public.audit_events
                        (audit_event_id, actor_user_id, event_category, event_origin,
                         occurred_at, created_at, legal_hold)
                    VALUES
                        ('92000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000004',
                         'CHECKLIST_QUARANTINE_VIEWED', 'AUDIT_LOG',
                         '2018-01-01T00:00:00Z', '2018-01-01T00:00:00Z', false),
                        ('92000000-0000-0000-0000-000000000002',
                         '10000000-0000-0000-0000-000000000004',
                         'CHECKLIST_QUARANTINE_VIEWED', 'AUDIT_LOG',
                         '2018-01-01T00:00:00Z', '2018-01-01T00:00:00Z', true),
                        ('92000000-0000-0000-0000-000000000003',
                         '10000000-0000-0000-0000-000000000004',
                         'CHECKLIST_QUARANTINE_VIEWED', 'AUDIT_LOG',
                         '2018-01-01T00:00:00Z', now(), false),
                        ('92000000-0000-0000-0000-000000000004',
                         '10000000-0000-0000-0000-000000000004',
                         'CHECKLIST_QUARANTINE_VIEWED', 'AUDIT_LOG',
                         now(), '2018-01-01T00:00:00Z', false),
                        ('92000000-0000-0000-0000-000000000005',
                         '10000000-0000-0000-0000-000000000004',
                         'CHECKLIST_QUARANTINE_VIEWED', 'AUDIT_LOG',
                         '2018-01-01T00:00:00Z', '2018-01-01T00:00:00Z', false)
                    """);
            statement.execute("""
                    INSERT INTO public.checklist_action_commands
                        (checklist_action_command_id, actor_user_id, task_kind, task_id,
                         client_request_id, payload_hash, action_type, result_status,
                         result_jsonb, applied_at, retain_until, legal_hold, created_at)
                    VALUES
                        ('92000000-0000-0000-0000-000000000021',
                         '10000000-0000-0000-0000-000000000004', 'CARE_TASK',
                         '92000000-0000-0000-0000-000000000031',
                         '92000000-0000-0000-0000-000000000051', repeat('c', 64),
                         'COMPLETE', 'APPLIED', '{}'::jsonb,
                         '2018-01-01T00:00:00Z', '2019-01-01T00:00:00Z', false,
                         '2018-01-01T00:00:00Z'),
                        ('92000000-0000-0000-0000-000000000022',
                         '10000000-0000-0000-0000-000000000004', 'CARE_TASK',
                         '92000000-0000-0000-0000-000000000032',
                         '92000000-0000-0000-0000-000000000052', repeat('d', 64),
                         'COMPLETE', 'APPLIED', '{}'::jsonb,
                         '2018-01-01T00:00:00Z', '2019-01-01T00:00:00Z', false,
                         '2018-01-01T00:00:00Z'),
                        ('92000000-0000-0000-0000-000000000023',
                         '10000000-0000-0000-0000-000000000004', 'CARE_TASK',
                         '92000000-0000-0000-0000-000000000031',
                         '92000000-0000-0000-0000-000000000053', repeat('e', 64),
                         'COMPLETE', 'APPLIED', '{}'::jsonb,
                         '2018-01-01T00:00:00Z', '2019-01-01T00:00:00Z', true,
                         '2018-01-01T00:00:00Z'),
                        ('92000000-0000-0000-0000-000000000024',
                         '10000000-0000-0000-0000-000000000004', 'CARE_TASK',
                         '92000000-0000-0000-0000-000000000099',
                         '92000000-0000-0000-0000-000000000054', repeat('f', 64),
                         'COMPLETE', 'APPLIED', '{}'::jsonb,
                         '2018-01-01T00:00:00Z', '2019-01-01T00:00:00Z', false,
                         '2018-01-01T00:00:00Z'),
                        ('92000000-0000-0000-0000-000000000025',
                         '10000000-0000-0000-0000-000000000004', 'CARE_TASK',
                         '92000000-0000-0000-0000-000000000031',
                         '92000000-0000-0000-0000-000000000055', repeat('1', 64),
                         'COMPLETE', 'APPLIED', '{}'::jsonb,
                         '2018-01-01T00:00:00Z', '2099-01-01T00:00:00Z', false,
                         '2018-01-01T00:00:00Z')
                    """);
            statement.execute("""
                    DELETE FROM public.care_tasks
                    WHERE task_id = '92000000-0000-0000-0000-000000000099'
                    """);
        }
    }

    private static void callPurge(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT audit_events_purged, quarantines_purged, action_commands_purged
                     FROM public.checklist_purge_retained_records(
                         '10000000-0000-0000-0000-000000000001')
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getLong(1)).isGreaterThanOrEqualTo(1L);
            assertThat(result.getLong(2)).isZero();
            assertThat(result.getLong(3)).isGreaterThanOrEqualTo(1L);
        }
    }

    private static void assertUntrustedPurgeActorRejected(Connection connection) {
        assertThatThrownBy(() -> {
            try (var statement = connection.createStatement()) {
                statement.executeQuery("""
                        SELECT * FROM public.checklist_purge_retained_records(
                            '10000000-0000-0000-0000-000000000004')
                        """);
            }
        }).isInstanceOf(java.sql.SQLException.class)
                .hasMessageContaining("PURGE_ACTOR_NOT_TRUSTED");
    }

    private static void assertDirectRetentionAttacksAreRejected(
            EmbeddedPostgres postgres,
            Connection privilegedConnection) throws Exception {
        SQLException forgedAuditDelete = captureSqlFailure(privilegedConnection, statement -> {
            statement.execute("SELECT set_config('carebridge.checklist_retention_purge', 'on', true)");
            statement.executeUpdate("""
                    DELETE FROM public.audit_events
                    WHERE audit_event_id = '92000000-0000-0000-0000-000000000005'
                    """);
        });
        SQLException forgedActorPurge = captureSqlFailure(privilegedConnection, statement ->
                statement.executeQuery("""
                        SELECT * FROM public.checklist_purge_retained_records(
                            '10000000-0000-0000-0000-000000000001')
                        """));
        SQLException forgedCommandDelete = captureSqlFailure(privilegedConnection, statement ->
                statement.executeUpdate("""
                        DELETE FROM public.checklist_action_commands
                        WHERE checklist_action_command_id = '92000000-0000-0000-0000-000000000025'
                        """));

        SQLException applicationCallerPurge;
        try (Connection applicationConnection = postgres
                .getDatabase(APPLICATION_DB_ROLE, "postgres")
                .getConnection()) {
            applicationCallerPurge = captureSqlFailure(applicationConnection, statement ->
                    statement.executeQuery("""
                            SELECT * FROM public.checklist_purge_retained_records(
                                '10000000-0000-0000-0000-000000000001')
                            """));
        }

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat((Throwable) forgedAuditDelete)
                .as("a custom GUC must not unlock direct immutable-audit DELETE")
                .isNotNull()
                .hasMessageContaining("IMMUTABLE_TABLE");
        softly.assertThat((Throwable) forgedActorPurge)
                .as("the migration/application database caller must not forge an operations UUID")
                .isNotNull()
                .hasMessageContaining("PURGE_DATABASE_CALLER_NOT_TRUSTED");
        softly.assertThat((Throwable) forgedCommandDelete)
                .as("a shared database role must not directly delete retained action commands")
                .isNotNull()
                .hasMessageContaining("RETENTION_DELETE_NOT_AUTHORIZED");
        softly.assertThat((Throwable) applicationCallerPurge)
                .as("the shared application database role must not execute the retention function")
                .isNotNull()
                .hasMessageContaining("permission denied");
        softly.assertAll();
    }

    private static SQLException captureSqlFailure(Connection connection, SqlAction action) throws Exception {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (var statement = connection.createStatement()) {
            action.execute(statement);
            return null;
        } catch (SQLException exception) {
            return exception;
        } finally {
            connection.rollback();
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private static void assertPurgeAuditFailureRollsBack(
            EmbeddedPostgres postgres,
            Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO public.audit_events
                        (audit_event_id, actor_user_id, event_category, event_origin,
                         occurred_at, created_at, legal_hold)
                    VALUES
                        ('92000000-0000-0000-0000-000000000007',
                         '10000000-0000-0000-0000-000000000004',
                         'CHECKLIST_QUARANTINE_VIEWED', 'AUDIT_LOG',
                         '2018-01-01T00:00:00Z', '2018-01-01T00:00:00Z', false)
                    """);
            statement.execute("""
                    INSERT INTO public.checklist_action_commands
                        (checklist_action_command_id, actor_user_id, task_kind, task_id,
                         client_request_id, payload_hash, action_type, result_status,
                         result_jsonb, applied_at, retain_until, legal_hold, created_at)
                    VALUES
                        ('92000000-0000-0000-0000-000000000026',
                         '10000000-0000-0000-0000-000000000004', 'CARE_TASK',
                         '92000000-0000-0000-0000-000000000031',
                         '92000000-0000-0000-0000-000000000056', repeat('2', 64),
                         'COMPLETE', 'APPLIED', '{}'::jsonb,
                         '2018-01-01T00:00:00Z', '2019-01-01T00:00:00Z', false,
                         '2018-01-01T00:00:00Z')
                    """);
            statement.execute("""
                    CREATE OR REPLACE FUNCTION public.test_reject_retention_audit()
                    RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN
                        IF NEW.event_category = 'CHECKLIST_RETENTION_PURGED' THEN
                            RAISE EXCEPTION 'INJECTED_RETENTION_AUDIT_FAILURE';
                        END IF;
                        RETURN NEW;
                    END
                    $$
                    """);
            statement.execute("""
                    CREATE TRIGGER test_reject_retention_audit_trg
                    BEFORE INSERT ON public.audit_events
                    FOR EACH ROW EXECUTE FUNCTION public.test_reject_retention_audit()
                    """);
        }

        assertThatThrownBy(() -> {
            try (Connection operationsConnection = postgres
                         .getDatabase(OPERATIONS_DB_ROLE, "postgres")
                         .getConnection();
                 var statement = operationsConnection.createStatement()) {
                statement.executeQuery("""
                        SELECT * FROM public.checklist_purge_retained_records(
                            '10000000-0000-0000-0000-000000000001')
                        """);
            }
        }).isInstanceOf(java.sql.SQLException.class)
                .hasMessageContaining("INJECTED_RETENTION_AUDIT_FAILURE");

        try (var statement = connection.createStatement()) {
            statement.execute("DROP TRIGGER test_reject_retention_audit_trg ON public.audit_events");
            statement.execute("DROP FUNCTION public.test_reject_retention_audit()");
        }

        assertThat(count(connection, """
                SELECT count(*) FROM public.audit_events
                WHERE audit_event_id = '92000000-0000-0000-0000-000000000007'
                """)).isOne();
        assertThat(count(connection, """
                SELECT count(*) FROM public.checklist_action_commands
                WHERE checklist_action_command_id = '92000000-0000-0000-0000-000000000026'
                """)).isOne();
        assertThat(count(connection, """
                SELECT count(*) FROM public.audit_events
                WHERE event_category = 'CHECKLIST_RETENTION_PURGED'
                """)).isOne();
    }

    private static long count(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            return result.next() ? result.getLong(1) : 0L;
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void execute(java.sql.Statement statement) throws SQLException;
    }
}
