package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/** Proves that the direct V1 schema and frozen V2 reference data bootstrap from empty PG18. */
@EnabledOnOs(OS.WINDOWS)
class ProductionBaselineBootstrapTest {

    private static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");
    private static final String REFERENCE_MANIFEST =
            "db/production-baseline-reference.properties";
    private static final String V2_SHA256 =
            "421b944a461aa9cacad8d5962a621818afd59b6d5db4115298dfe9620f57900d";
    private static final Set<String> REFERENCE_TABLE_ALLOWLIST = Set.of(
            "health_metric_definitions",
            "knowledge_sources",
            "red_flag_rules",
            "community_topics",
            "ai_moderation_policies",
            "vaccination_schedules",
            "care_item_templates");
    private static final Map<String, String> SEED_IDENTITY_COLUMNS = Map.of(
            "health_metric_definitions", "metric_definition_id",
            "knowledge_sources", "knowledge_source_id",
            "red_flag_rules", "id",
            "community_topics", "id",
            "ai_moderation_policies", "policy_id",
            "vaccination_schedules", "vaccination_schedule_id",
            "care_item_templates", "template_id");

    @Test
    @Timeout(240)
    void productionPairBootstrapsAndValidatesTheProductionContract() throws Exception {
        assertThat(Files.list(MIGRATION_DIRECTORY).map(path -> path.getFileName().toString()).sorted().toList())
                .containsExactly("V1__baseline_production_schema.sql");
        Path v1 = MIGRATION_DIRECTORY.resolve("V1__baseline_production_schema.sql");
        Path v2 = Path.of("src/main/resources/db/data_seed/production_reference_data.sql");
        assertThat(v1).isRegularFile();
        assertThat(v2).isRegularFile();

        String schemaSql = Files.readString(v1);
        String seedSql = Files.readString(v2);
        var schemaStatements = SqlTopLevelStatementParser.parse(schemaSql);
        assertThat(schemaStatements)
                .extracting(SqlTopLevelStatementParser.SqlStatement::type)
                .doesNotContain("INSERT", "UPDATE", "DELETE", "MERGE", "COPY", "TRUNCATE");
        assertThat(schemaStatements.stream()
                        .filter(statement -> statement.type().equals("DO"))
                        .filter(statement -> SqlTopLevelStatementParser.containsExecutableDataMutation(
                                statement.dollarQuotedBody()))
                        .toList())
                .as("V1 DO blocks must not mutate production data")
                .isEmpty();

        var seedStatements = SqlTopLevelStatementParser.parse(seedSql);
        assertThat(seedStatements)
                .extracting(SqlTopLevelStatementParser.SqlStatement::type)
                .containsOnly("INSERT");
        assertThat(seedStatements.stream()
                        .map(SqlTopLevelStatementParser.SqlStatement::insertTarget)
                        .collect(java.util.stream.Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(REFERENCE_TABLE_ALLOWLIST);
        assertThat(seedStatements)
                .allSatisfy(statement -> assertThat(REFERENCE_TABLE_ALLOWLIST)
                        .contains(statement.insertTarget()));
        assertThat(sha256(Files.readAllBytes(v2))).isEqualTo(V2_SHA256);

        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load();

            var first = flyway.migrate();
            var second = flyway.migrate();
            assertThat(first.success).isTrue();
            assertThat(first.migrationsExecuted).isEqualTo(1);
            assertThat(second.success).isTrue();
            assertThat(second.migrationsExecuted).isZero();
            assertThat(flyway.validateWithResult().validationSuccessful).isTrue();

            try (Connection connection = dataSource.getConnection()) {
                for (var stmt : seedStatements) {
                    try (var s = connection.createStatement()) {
                        s.execute(stmt.sql());
                    }
                }
                assertThat(historyVersions(connection)).containsExactly("1");
                assertThat(count(connection, """
                        SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                         WHERE n.nspname='public' AND c.relkind IN ('r','p')
                           AND c.relname<>'flyway_schema_history'
                        """)).isEqualTo(63);
                assertThat(count(connection, """
                        SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                         WHERE n.nspname='public' AND c.relkind='v'
                        """)).isEqualTo(3);
                assertThat(count(connection, """
                        SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
                         WHERE n.nspname='public'
                        """)).isEqualTo(35);
                assertThat(count(connection, """
                        SELECT count(*) FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid
                         JOIN pg_namespace n ON n.oid=c.relnamespace
                         WHERE n.nspname='public' AND NOT t.tgisinternal
                        """)).isEqualTo(36);

                assertThat(count(connection, "SELECT count(*) FROM health_metric_definitions")).isEqualTo(16);
                assertThat(count(connection, "SELECT count(*) FROM knowledge_sources")).isEqualTo(3);
                assertThat(count(connection, "SELECT count(*) FROM red_flag_rules")).isEqualTo(8);
                assertThat(count(connection, "SELECT count(*) FROM ai_moderation_policies")).isEqualTo(11);
                assertThat(count(connection, "SELECT count(*) FROM community_topics")).isEqualTo(107);
                assertThat(count(connection, "SELECT count(*) FROM community_topics WHERE slug LIKE 'rec-%'"))
                        .isEqualTo(107);
                assertThat(count(connection, "SELECT count(*) FROM vaccination_schedules")).isEqualTo(30);
                assertThat(count(connection, """
                        SELECT count(*) FROM vaccination_schedules
                         WHERE schedule_version='vn-2026'
                        """)).isEqualTo(30);
                assertThat(count(connection, """
                        SELECT count(*) FROM care_item_templates
                         WHERE entry_type='TEMPLATE_ROOT' AND stage='PREGNANCY'
                           AND checklist_contract_version=2 AND content_status='DRAFT'
                           AND distribution_enabled=false
                        """)).isEqualTo(16);
                assertThat(count(connection, """
                        SELECT count(*) FROM care_item_templates
                         WHERE entry_type='CHECKLIST_ENTRY' AND stage='PREGNANCY'
                           AND checklist_contract_version=2
                        """)).isEqualTo(62);
                assertThat(count(connection, "SELECT count(*) FROM care_item_templates")).isEqualTo(86);
                assertThat(count(connection, """
                        SELECT count(*) FROM care_item_templates WHERE entry_type='EXERCISE_TEMPLATE'
                        """)).isEqualTo(4);
                assertThat(count(connection, """
                        SELECT count(*) FROM care_item_templates WHERE entry_type='POSTURE_CONFIG'
                        """)).isEqualTo(4);

                assertThat(count(connection, "SELECT count(*) FROM users")).isZero();
                assertThat(count(connection, "SELECT count(*) FROM community_content")).isZero();
                assertThat(count(connection, "SELECT count(*) FROM community_interactions")).isZero();
                assertThat(count(connection, "SELECT count(*) FROM maternal_exercise_sessions")).isZero();

                assertWhoChecklistHierarchy(connection);
                assertReferenceHashesAndCatalogManifest(connection);
                assertRetentionFinalizerState(connection);
                assertApplicationRuntimePrivilegeContract(connection);
                assertFinalStateSecurityBehavior(postgres, connection);
            }
            validateJpaMappings(postgres.getJdbcUrl("postgres", "postgres"));
        }
    }

    @Test
    @Timeout(240)
    void productionPairRejectsANonPrivilegedFlywayRunnerBeforeCreatingApplicationObjects()
            throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            DataSource adminDataSource = postgres.getPostgresDatabase();
            try (Connection admin = adminDataSource.getConnection();
                 var statement = admin.createStatement()) {
                statement.execute("""
                        CREATE ROLE carebridge_baseline_flyway
                        LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT
                        NOREPLICATION NOBYPASSRLS
                        """);
                EmbeddedPostgresRoleFixture.provisionForFlywayRunner(
                        admin, "carebridge_baseline_flyway");
                statement.execute("ALTER DATABASE postgres OWNER TO carebridge_baseline_flyway");
            }

            DataSource runnerDataSource = postgres.getDatabase(
                    "carebridge_baseline_flyway", "postgres");
            Flyway flyway = Flyway.configure()
                    .dataSource(runnerDataSource)
                    .locations("classpath:db/migration")
                    .validateOnMigrate(true)
                    .outOfOrder(false)
                    .load();
            assertThatThrownBy(flyway::migrate)
                    .rootCause()
                    .hasMessageContaining("CAREBRIDGE_PRIVILEGED_FINALIZER_REQUIRED");

            try (Connection connection = adminDataSource.getConnection()) {
                assertThat(count(connection, """
                        SELECT count(*)
                          FROM pg_catalog.pg_class relation
                          JOIN pg_catalog.pg_namespace namespace_entry
                            ON namespace_entry.oid = relation.relnamespace
                         WHERE namespace_entry.nspname = 'public'
                           AND relation.relkind IN ('r', 'p')
                           AND relation.relname <> 'flyway_schema_history'
                        """)).as("failed V1 must roll back all application tables").isZero();
                assertThat(count(connection, """
                        SELECT count(*) FROM pg_catalog.pg_auth_members
                         WHERE roleid = to_regrole('carebridge_checklist_retention_owner')
                           AND (inherit_option OR set_option)
                        """)).as("failed V1 must not leave retention-owner membership").isZero();
            }
        }
    }

    private java.util.List<String> historyVersions(Connection connection) throws Exception {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("""
                SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank
                """)) {
            var versions = new java.util.ArrayList<String>();
            while (rows.next()) {
                versions.add(rows.getString(1));
            }
            return versions;
        }
    }

    static long count(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertThat(rows.next()).isTrue();
            return rows.getLong(1);
        }
    }

    private void assertWhoChecklistHierarchy(Connection connection) throws Exception {
        assertThat(count(connection, """
                SELECT count(*)
                  FROM care_item_templates child
             LEFT JOIN care_item_templates parent ON parent.template_id = child.parent_template_id
                 WHERE child.entry_type = 'CHECKLIST_ENTRY'
                   AND child.stage = 'PREGNANCY'
                   AND child.checklist_contract_version = 2
                   AND (parent.template_id IS NULL OR parent.entry_type <> 'TEMPLATE_ROOT')
                """)).as("WHO child rows must have an existing root parent").isZero();
        assertThat(count(connection, """
                SELECT count(*)
                  FROM care_item_templates root
                 WHERE root.entry_type = 'TEMPLATE_ROOT'
                   AND root.stage = 'PREGNANCY'
                   AND root.checklist_contract_version = 2
                   AND (root.parent_template_id IS NOT NULL
                        OR root.content_status <> 'DRAFT'
                        OR root.distribution_enabled IS DISTINCT FROM false)
                """)).as("WHO roots stay parentless, DRAFT, and non-distributing").isZero();
        assertThat(count(connection, """
                SELECT count(*)
                  FROM care_item_templates child
                 WHERE child.entry_type = 'CHECKLIST_ENTRY'
                   AND child.stage = 'PREGNANCY'
                   AND child.checklist_contract_version = 2
                   AND (child.content_status <> 'DRAFT'
                        OR child.distribution_enabled IS DISTINCT FROM false)
                """)).as("WHO children stay DRAFT and non-distributing").isZero();
        assertThat(count(connection, """
                SELECT count(*)
                  FROM care_item_templates root
                 WHERE root.entry_type = 'TEMPLATE_ROOT'
                   AND root.stage = 'PREGNANCY'
                   AND root.checklist_contract_version = 2
                   AND NOT EXISTS (
                       SELECT 1 FROM care_item_templates child
                        WHERE child.parent_template_id = root.template_id
                          AND child.entry_type = 'CHECKLIST_ENTRY')
                """)).as("every WHO root must have at least one child").isZero();
    }

    private void assertReferenceHashesAndCatalogManifest(Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("SET TIME ZONE 'Asia/Bangkok'");
        }
        Properties expected = loadReferenceManifest();
        Map<String, String> actual = new LinkedHashMap<>();
        PostgresCatalogManifest.capture(connection).forEach((name, section) -> {
            actual.put("catalog." + name + ".count", Long.toString(section.count()));
            actual.put("catalog." + name + ".sha256", section.sha256());
        });
        seedContentHashes(connection).forEach((table, hash) ->
                actual.put("seed." + table + ".sha256", hash));
        actual.put("seed.sql.sha256", V2_SHA256);

        Map<String, String> expectedContract = new LinkedHashMap<>();
        expected.stringPropertyNames().stream()
                .filter(name -> name.startsWith("catalog.") || name.startsWith("seed."))
                .sorted()
                .forEach(name -> expectedContract.put(name, expected.getProperty(name)));
        assertThat(actual)
                .as("Catalog/seed reference mismatch. Actual manifest:%n%s", renderProperties(actual))
                .containsExactlyInAnyOrderEntriesOf(expectedContract);
    }

    private Map<String, String> seedContentHashes(Connection connection) throws Exception {
        Map<String, String> hashes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : SEED_IDENTITY_COLUMNS.entrySet()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String sql = "SELECT row_to_json(seed_row)::text FROM (SELECT * FROM public.\""
                    + entry.getKey() + "\" ORDER BY \"" + entry.getValue() + "\") seed_row";
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
                while (rows.next()) {
                    digest.update(rows.getString(1).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) '\n');
                }
            }
            hashes.put(entry.getKey(), HexFormat.of().formatHex(digest.digest()));
        }
        return hashes;
    }

    static void assertRetentionFinalizerState(Connection connection) throws Exception {
        assertThat(count(connection, """
                SELECT count(*)
                  FROM pg_catalog.pg_proc routine
                  JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = routine.proowner
                 WHERE routine.oid = 'public.checklist_purge_retained_records(uuid)'::regprocedure
                   AND owner_role.rolname = 'carebridge_checklist_retention_owner'
                   AND NOT EXISTS (
                       SELECT 1
                         FROM pg_catalog.aclexplode(
                             COALESCE(routine.proacl, pg_catalog.acldefault('f', routine.proowner))) acl
                        WHERE acl.grantee = 0
                          AND acl.privilege_type = 'EXECUTE')
                   AND has_function_privilege(
                       'checklist_operations', routine.oid, 'EXECUTE')
                """)).isOne();
        assertThat(count(connection, """
                SELECT count(*)
                  FROM pg_catalog.pg_auth_members
                 WHERE roleid = to_regrole('carebridge_checklist_retention_owner')
                   AND (inherit_option OR set_option)
                """)).as("retention owner must be unreachable after V1").isZero();
    }

    static void assertApplicationRuntimePrivilegeContract(Connection connection) throws Exception {
        assertThat(count(connection, """
                SELECT count(*)
                  FROM (VALUES
                      ('system_configurations'),
                      ('expert_consultation_requests'),
                      ('conversation_calls'),
                      ('safety_events'),
                      ('notification_records'),
                      ('notification_jobs')
                  ) expected(table_name)
                 WHERE NOT has_table_privilege(
                     'carebridge_application',
                     'public.' || expected.table_name,
                     'SELECT,INSERT,UPDATE,DELETE')
                """)).as("representative runtime tables require full CRUD").isZero();
        assertThat(count(connection, """
                SELECT count(*)
                 WHERE has_schema_privilege('carebridge_application', 'public', 'USAGE')
                   AND NOT has_schema_privilege('carebridge_application', 'public', 'CREATE')
                   AND has_table_privilege(
                       'carebridge_application', 'public.audit_events', 'SELECT,INSERT')
                   AND NOT has_table_privilege(
                       'carebridge_application', 'public.audit_events', 'UPDATE')
                   AND NOT has_table_privilege(
                       'carebridge_application', 'public.audit_events', 'DELETE')
                   AND has_table_privilege(
                       'carebridge_application', 'public.checklist_action_commands',
                       'SELECT,INSERT,UPDATE')
                   AND NOT has_table_privilege(
                       'carebridge_application', 'public.checklist_action_commands', 'DELETE')
                   AND has_table_privilege(
                       'carebridge_application', 'public.reminder_occurrence_aliases', 'SELECT')
                   AND NOT has_table_privilege(
                       'carebridge_application', 'public.reminder_occurrence_aliases', 'INSERT')
                   AND NOT has_table_privilege(
                       'carebridge_application', 'public.reminder_occurrence_aliases', 'UPDATE')
                   AND NOT has_table_privilege(
                       'carebridge_application', 'public.reminder_occurrence_aliases', 'DELETE')
                   AND NOT has_table_privilege(
                       'carebridge_application', 'public.flyway_schema_history', 'SELECT')
                   AND NOT has_function_privilege(
                       'carebridge_application',
                       'public.checklist_purge_retained_records(uuid)', 'EXECUTE')
                """)).as("runtime role must retain narrow security boundaries").isOne();
        assertThat(count(connection, """
                SELECT count(*)
                 WHERE to_regclass('public.checklist_migration_quarantine') IS NULL
                    OR (
                        has_table_privilege(
                            'carebridge_application',
                            'public.checklist_migration_quarantine', 'SELECT,UPDATE')
                        AND NOT has_table_privilege(
                            'carebridge_application',
                            'public.checklist_migration_quarantine', 'INSERT')
                        AND NOT has_table_privilege(
                            'carebridge_application',
                            'public.checklist_migration_quarantine', 'DELETE'))
                """)).as("retired quarantine stays absent or preserves SELECT+UPDATE only").isOne();
        assertThat(count(connection, """
                SELECT count(*)
                  FROM pg_catalog.pg_sequence sequence_metadata
                  JOIN pg_catalog.pg_class sequence_entry
                    ON sequence_entry.oid = sequence_metadata.seqrelid
                  JOIN pg_catalog.pg_namespace namespace_entry
                    ON namespace_entry.oid = sequence_entry.relnamespace
                 WHERE namespace_entry.nspname = 'public'
                   AND NOT has_sequence_privilege(
                       'carebridge_application', sequence_metadata.seqrelid, 'USAGE')
                """)).as("runtime role must be able to consume every application sequence").isZero();
    }

    private void assertFinalStateSecurityBehavior(
            EmbeddedPostgres postgres, Connection privilegedConnection) throws Exception {
        String operationsActor = "10000000-0000-0000-0000-000000000001";
        String retainedAudit = "92000000-0000-0000-0000-000000000001";
        try (var statement = privilegedConnection.createStatement()) {
            statement.execute("""
                    INSERT INTO public.users (
                        user_id, person_id, created_at, updated_at, enabled, locked,
                        role, failed_login_count, email_verified, phone_verified,
                        account_status, must_change_password)
                    VALUES (
                        '10000000-0000-0000-0000-000000000001',
                        '10000000-0000-0000-0000-000000000001',
                        now(), now(), true, false, 'SYSTEM_ADMIN', 0, true, true,
                        'ACTIVE', false)
                    """);
            statement.execute("""
                    INSERT INTO public.audit_events (
                        audit_event_id, actor_user_id, event_category, occurred_at,
                        created_at, event_origin, legal_hold, correlation_id)
                    VALUES (
                        '92000000-0000-0000-0000-000000000001',
                        '10000000-0000-0000-0000-000000000001',
                        'CHECKLIST_QUARANTINE_VIEWED',
                        '2018-01-01T00:00:00Z', '2018-01-01T00:00:00Z',
                        'AUDIT_LOG', false, '92000000-0000-0000-0000-000000000091')
                    """);
        }

        assertThatThrownBy(() -> {
            try (var statement = privilegedConnection.createStatement()) {
                statement.executeUpdate("DELETE FROM public.audit_events WHERE audit_event_id = '"
                        + retainedAudit + "'");
            }
        }).isInstanceOf(SQLException.class).hasMessageContaining("IMMUTABLE_TABLE");

        assertThatThrownBy(() -> {
            try (var statement = privilegedConnection.createStatement()) {
                statement.executeQuery("SELECT * FROM public.checklist_purge_retained_records('"
                        + operationsActor + "')");
            }
        }).isInstanceOf(SQLException.class)
                .hasMessageContaining("PURGE_DATABASE_CALLER_NOT_TRUSTED");

        assertThatThrownBy(() -> {
            try (Connection application = postgres
                         .getDatabase("carebridge_application", "postgres")
                         .getConnection();
                 var statement = application.createStatement()) {
                statement.executeQuery("SELECT * FROM public.checklist_purge_retained_records('"
                        + operationsActor + "')");
            }
        }).isInstanceOf(SQLException.class).hasMessageContaining("permission denied");

        try (Connection application = postgres
                     .getDatabase("carebridge_application", "postgres")
                     .getConnection();
             var statement = application.createStatement()) {
            for (String table : java.util.List.of(
                    "system_configurations",
                    "expert_consultation_requests",
                    "conversation_calls",
                    "safety_events",
                    "notification_records",
                    "notification_jobs")) {
                try (var result = statement.executeQuery(
                        "SELECT count(*) FROM public." + table)) {
                    assertThat(result.next()).isTrue();
                }
            }
            statement.execute("""
                    INSERT INTO public.system_configurations (
                        system_configuration_id, api_rate_limit, connection_timeout_ms,
                        max_upload_size_mb, administrator_email, updated_by)
                    VALUES (
                        '93000000-0000-0000-0000-000000000001', 100, 1000, 10,
                        'runtime-acl@carebridge.test',
                        '10000000-0000-0000-0000-000000000001')
                    """);
            assertThat(statement.executeUpdate("""
                    UPDATE public.system_configurations SET api_rate_limit = 101
                     WHERE system_configuration_id = '93000000-0000-0000-0000-000000000001'
                    """)).isOne();
            assertThat(statement.executeUpdate("""
                    DELETE FROM public.system_configurations
                     WHERE system_configuration_id = '93000000-0000-0000-0000-000000000001'
                    """)).isOne();
        }

        assertThatThrownBy(() -> {
            try (Connection application = postgres
                         .getDatabase("carebridge_application", "postgres")
                         .getConnection();
                 var statement = application.createStatement()) {
                statement.executeQuery("SELECT * FROM public.flyway_schema_history");
            }
        }).isInstanceOf(SQLException.class).hasMessageContaining("permission denied");
        assertThatThrownBy(() -> {
            try (Connection application = postgres
                         .getDatabase("carebridge_application", "postgres")
                         .getConnection();
                 var statement = application.createStatement()) {
                statement.executeUpdate("UPDATE public.audit_events SET status = 'CLOSED'");
            }
        }).isInstanceOf(SQLException.class).hasMessageContaining("permission denied");

        assertThatThrownBy(() -> {
            try (var statement = privilegedConnection.createStatement()) {
                statement.execute("""
                        INSERT INTO public.checklist_action_commands (
                            checklist_action_command_id, actor_user_id, task_kind, task_id,
                            client_request_id, payload_hash, action_type, result_status,
                            result_jsonb, applied_at, retain_until, legal_hold, created_at)
                        VALUES (
                            '92000000-0000-0000-0000-000000000021',
                            '10000000-0000-0000-0000-000000000001', 'CARE_TASK',
                            '92000000-0000-0000-0000-000000000099',
                            '92000000-0000-0000-0000-000000000051', repeat('c', 64),
                            'COMPLETE', 'APPLIED', '{}'::jsonb, now(), now(), false, now())
                        """);
            }
        }).isInstanceOf(SQLException.class)
                .hasMessageContaining("CHECKLIST_ACTION_TARGET_NOT_FOUND");

        try (Connection operations = postgres
                     .getDatabase("checklist_operations", "postgres")
                     .getConnection()) {
            assertThatThrownBy(() -> {
                try (var statement = operations.createStatement()) {
                    statement.executeQuery("""
                            SELECT * FROM public.checklist_purge_retained_records(
                                '10000000-0000-0000-0000-000000000002')
                            """);
                }
            }).isInstanceOf(SQLException.class).hasMessageContaining("PURGE_ACTOR_NOT_TRUSTED");

            try (var statement = operations.createStatement();
                 var result = statement.executeQuery("""
                         SELECT audit_events_purged, quarantines_purged, action_commands_purged
                           FROM public.checklist_purge_retained_records(
                               '10000000-0000-0000-0000-000000000001')
                         """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong("audit_events_purged")).isOne();
                assertThat(result.getLong("quarantines_purged")).isZero();
                assertThat(result.getLong("action_commands_purged")).isZero();
            }
        }
        assertThat(count(privilegedConnection,
                "SELECT count(*) FROM audit_events WHERE audit_event_id = '" + retainedAudit + "'"))
                .isZero();
        assertThat(count(privilegedConnection, """
                SELECT count(*) FROM audit_events
                 WHERE event_category = 'CHECKLIST_RETENTION_PURGED'
                   AND actor_user_id = '10000000-0000-0000-0000-000000000001'
                """)).isOne();
    }

    private Properties loadReferenceManifest() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = ProductionBaselineBootstrapTest.class
                .getClassLoader()
                .getResourceAsStream(REFERENCE_MANIFEST)) {
            assertThat(input).as("reference manifest resource").isNotNull();
            properties.load(input);
        }
        return properties;
    }

    private static String renderProperties(Map<String, String> properties) {
        StringBuilder rendered = new StringBuilder();
        properties.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                rendered.append(entry.getKey()).append('=').append(entry.getValue()).append('\n'));
        return rendered.toString();
    }

    private static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private void validateJpaMappings(String jdbcUrl) {
        var dataSource = new DriverManagerDataSource(jdbcUrl, "postgres", "");
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.carebridge.backend");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        var properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        factory.setJpaProperties(properties);
        try {
            factory.afterPropertiesSet();
            assertThat(factory.getObject()).isNotNull();
            assertThat(factory.getObject().isOpen()).isTrue();
        } finally {
            factory.destroy();
        }
    }
}
