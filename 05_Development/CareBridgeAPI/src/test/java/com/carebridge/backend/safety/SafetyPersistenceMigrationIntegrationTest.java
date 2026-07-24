package com.carebridge.backend.safety;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@EnabledIfSystemProperty(named = "gate0.enabled", matches = "true")
class SafetyPersistenceMigrationIntegrationTest {

    private static final String PRE_BATCH4_TARGET = "20260722020500";

    @Test
    void emptyLegacyTablesAreDroppedAndCanonicalSafetySchemaIsCompleted() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_BATCH4_TARGET).migrate();

            flyway(postgres, null).migrate();

            try (var connection = connection(postgres);
                 var statement = connection.createStatement()) {
                assertThat(statement.executeUpdate("""
                        INSERT INTO audit_logs (audit_log_id, action, created_at)
                        VALUES (gen_random_uuid(), 'SAFETY_EVENT_RECORDED', now())
                        """)).isEqualTo(1);
                try (var result = statement.executeQuery("""
                         SELECT to_regclass('public.safety_alerts') AS safety_alerts,
                                to_regclass('public.emergency_events') AS emergency_events,
                                to_regclass('public.safety_events') AS safety_events,
                                to_regclass('public.safety_monitoring_settings') AS safety_settings,
                                to_regclass('public.safety_event_responses') AS responses,
                                to_regclass('public.emergency_alert_deliveries') AS deliveries,
                                EXISTS (SELECT 1 FROM information_schema.columns
                                         WHERE table_schema='public' AND table_name='imu_safety_events'
                                           AND column_name='countdown_deadline_at') AS countdown_exists,
                                EXISTS (SELECT 1 FROM information_schema.columns
                                         WHERE table_schema='public' AND table_name='safety_monitoring_config'
                                           AND column_name='sensor_permission_granted') AS permission_exists
                         """)) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString("safety_alerts")).isNull();
                    assertThat(result.getString("emergency_events")).isNull();
                    assertThat(result.getString("safety_events")).isNull();
                    assertThat(result.getString("safety_settings")).isNull();
                    assertThat(result.getString("responses")).isEqualTo("safety_event_responses");
                    assertThat(result.getString("deliveries")).isEqualTo("emergency_alert_deliveries");
                    assertThat(result.getBoolean("countdown_exists")).isTrue();
                    assertThat(result.getBoolean("permission_exists")).isTrue();
                }
            }
        }
    }

    @ParameterizedTest(name = "nonempty {0} rolls back")
    @MethodSource("legacyRows")
    void anyNonemptyCandidateRollsBackAllChanges(String table, String insertSql) throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_BATCH4_TARGET).migrate();
            try (var connection = connection(postgres); var statement = connection.createStatement()) {
                statement.execute(insertSql);
            }

            assertThatThrownBy(() -> flyway(postgres, null).migrate())
                    .hasMessageContaining("BLOCKED_PARTIAL_SAFETY_MIGRATION")
                    .hasMessageContaining(table);

            assertRollbackState(postgres, table);
        }
    }

    @Test
    void dependentViewRollsBackWithoutDroppingAnyCandidate() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_BATCH4_TARGET).migrate();
            try (var connection = connection(postgres); var statement = connection.createStatement()) {
                statement.execute("CREATE VIEW public.legacy_safety_event_view AS SELECT * FROM public.safety_events");
            }

            assertThatThrownBy(() -> flyway(postgres, null).migrate())
                    .hasMessageContaining("BLOCKED_PARTIAL_SAFETY_MIGRATION")
                    .hasMessageContaining("dependent view");
            assertRollbackState(postgres, "safety_events");
        }
    }

    static Stream<Arguments> legacyRows() {
        return Stream.of(
                Arguments.of("safety_alerts", """
                        SET session_replication_role = replica;
                        INSERT INTO safety_alerts (safety_event_id, recipient_user_id)
                        VALUES (gen_random_uuid(), gen_random_uuid());
                        SET session_replication_role = origin
                        """),
                Arguments.of("emergency_events", """
                        SET session_replication_role = replica;
                        INSERT INTO emergency_events (user_id) VALUES (gen_random_uuid());
                        SET session_replication_role = origin
                        """),
                Arguments.of("safety_events", """
                        SET session_replication_role = replica;
                        INSERT INTO safety_events (user_id, detected_at, event_type)
                        VALUES (gen_random_uuid(), now(), 'FALL');
                        SET session_replication_role = origin
                        """),
                Arguments.of("safety_monitoring_settings", """
                        SET session_replication_role = replica;
                        INSERT INTO safety_monitoring_settings (user_id) VALUES (gen_random_uuid());
                        SET session_replication_role = origin
                        """));
    }

    private static void assertRollbackState(PostgreSQLContainer postgres, String table) throws Exception {
        try (var connection = connection(postgres);
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT to_regclass('public.safety_alerts') AS safety_alerts,
                            to_regclass('public.emergency_events') AS emergency_events,
                            to_regclass('public.safety_events') AS safety_events,
                            to_regclass('public.safety_monitoring_settings') AS safety_settings,
                            to_regclass('public.safety_event_responses') AS responses,
                            (SELECT count(*) FROM %s) AS candidate_count
                     """.formatted(table))) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("safety_alerts")).isEqualTo("safety_alerts");
            assertThat(result.getString("emergency_events")).isEqualTo("emergency_events");
            assertThat(result.getString("safety_events")).isEqualTo("safety_events");
            assertThat(result.getString("safety_settings")).isEqualTo("safety_monitoring_settings");
            assertThat(result.getString("responses")).isNull();
            assertThat(result.getLong("candidate_count")).isGreaterThanOrEqualTo(0);
        }
    }

    private static java.sql.Connection connection(PostgreSQLContainer postgres) throws Exception {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static Flyway flyway(PostgreSQLContainer postgres, String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true);
        if (target != null) configuration.target(target);
        return configuration.load();
    }
}
