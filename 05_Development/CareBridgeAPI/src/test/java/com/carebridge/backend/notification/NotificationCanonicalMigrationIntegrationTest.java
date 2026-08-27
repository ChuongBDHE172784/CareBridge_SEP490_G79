package com.carebridge.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.postgresql.PostgreSQLContainer;

@EnabledIfSystemProperty(named = "gate0.enabled", matches = "true")
class NotificationCanonicalMigrationIntegrationTest {

    private static final String PRE_CONSOLIDATION_TARGET = "20260722020200";

    @Test
    void liveShapeWithZeroLegacyAndThirtyCanonicalRowsKeepsTheExactCount() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_CONSOLIDATION_TARGET).migrate();

            UUID userId = UUID.randomUUID();
            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO users (user_id, email, created_at, updated_at)
                        VALUES ('%s', 'migration-live-shape@carebridge.dev', now(), now())
                        """.formatted(userId));
                statement.executeUpdate("""
                        INSERT INTO notification_records (id, user_id, type, title, body, status, created_at)
                        SELECT gen_random_uuid(), '%s', 'MESSAGE', 'Message ' || n, 'Body ' || n,
                               'FAILED', now() - (n || ' minutes')::interval
                        FROM generate_series(1, 30) AS n
                        """.formatted(userId));
            }

            flyway(postgres, null).migrate();

            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT count(*) AS canonical_count,
                                max(to_regclass('public.notifications')::text) AS legacy_table,
                                count(*) FILTER (WHERE updated_at = created_at) AS preserved_timestamps
                         FROM notification_records
                         """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong("canonical_count")).isEqualTo(30);
                assertThat(result.getString("legacy_table")).isNull();
                assertThat(result.getLong("preserved_timestamps")).isEqualTo(30);
            }
        }
    }

    @Test
    void validLegacyRowIsPreservedBeforeLegacyTableIsDropped() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            Flyway flyway = flyway(postgres, PRE_CONSOLIDATION_TARGET);
            flyway.migrate();

            UUID userId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();
            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO users (user_id, email, created_at, updated_at)
                        VALUES ('%s', 'migration-test@carebridge.dev', now(), now())
                        """.formatted(userId));
                statement.executeUpdate("""
                        INSERT INTO notifications (
                            notification_id, recipient_user_id, notification_type, title, body,
                            delivery_status, channel, attempt_count, created_at, updated_at,
                            is_read, read_at, metadata
                        ) VALUES (
                            '%s', '%s', 'REMINDER', 'Nhắc lịch', 'Nội dung',
                            'DELIVERED', 'IN_APP', 2, now(), now(), true, now(),
                            '{"source":"legacy"}'::jsonb
                        )
                        """.formatted(notificationId, userId));
            }

            flyway(postgres, null).migrate();

            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT id, user_id, type, status, channel, attempt_count, is_read,
                                metadata ->> 'source', to_regclass('public.notifications')
                           FROM notification_records
                          WHERE id = '%s'
                         """.formatted(notificationId))) {
                assertThat(result.next()).isTrue();
                assertThat(result.getObject("id", UUID.class)).isEqualTo(notificationId);
                assertThat(result.getObject("user_id", UUID.class)).isEqualTo(userId);
                assertThat(result.getString("type")).isEqualTo("REMINDER");
                assertThat(result.getString("status")).isEqualTo("DELIVERED");
                assertThat(result.getString("channel")).isEqualTo("IN_APP");
                assertThat(result.getInt("attempt_count")).isEqualTo(2);
                assertThat(result.getBoolean("is_read")).isTrue();
                assertThat(result.getString(8)).isEqualTo("legacy");
                assertThat(result.getString(9)).isNull();
            }
        }
    }

    @Test
    void invalidLegacyRowFailsBeforeDropOrBackfill() throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            flyway(postgres, PRE_CONSOLIDATION_TARGET).migrate();

            UUID userId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();
            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO users (user_id, email, created_at, updated_at)
                        VALUES ('%s', 'migration-invalid@carebridge.dev', now(), now())
                        """.formatted(userId));
                statement.executeUpdate("""
                        INSERT INTO notifications (
                            notification_id, recipient_user_id, notification_type, title, body,
                            delivery_status, channel, attempt_count, created_at, updated_at, is_read
                        ) VALUES (
                            '%s', '%s', 'REMINDER', 'Nhắc lịch', 'Nội dung',
                            'SENT', 'PUSH', 0, now(), now(), NULL
                        )
                        """.formatted(notificationId, userId));
            }

            assertThatThrownBy(() -> flyway(postgres, null).migrate())
                    .hasMessageContaining("Legacy notifications preflight failed");

            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT to_regclass('public.notifications'),
                                (SELECT count(*) FROM notifications),
                                (SELECT count(*) FROM notification_records WHERE id = '%s')
                         """.formatted(notificationId))) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("notifications");
                assertThat(result.getLong(2)).isEqualTo(1);
                assertThat(result.getLong(3)).isZero();
            }
        }
    }

    private static Flyway flyway(PostgreSQLContainer postgres, String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(true);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
