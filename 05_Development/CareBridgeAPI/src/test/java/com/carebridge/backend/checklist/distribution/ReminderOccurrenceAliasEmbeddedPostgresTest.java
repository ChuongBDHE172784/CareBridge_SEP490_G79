package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.checklist.today.provider.ReminderOccurrenceIdFactory;
import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class ReminderOccurrenceAliasEmbeddedPostgresTest {
    private static final UUID DEFINITION_ID =
            UUID.fromString("83000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID CARE_SUBJECT_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Test
    @Timeout(180)
    void postgresIdentityMatchesJavaAndRescheduleRetainsBothAliases() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            EmbeddedPostgresRoleFixture.provision(postgres.getPostgresDatabase());
            Flyway.configure()
                    .dataSource(postgres.getPostgresDatabase())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            try (Connection connection = postgres.getPostgresDatabase().getConnection()) {
                assertGoldenVectors(connection);
                assertRescheduleAliases(connection);
            }
            assertApplicationRoleBoundary(postgres.getPostgresDatabase());
        }
    }

    private static void assertGoldenVectors(Connection connection) throws Exception {
        List<Instant> anchors = List.of(
                Instant.parse("2026-08-03T01:00:00Z"),
                Instant.parse("2026-08-03T01:00:00.123Z"),
                Instant.parse("2026-08-03T01:00:00.123456Z"));
        try (var statement = connection.prepareStatement("""
                SELECT public.reminder_occurrence_id_v1(?::uuid, ?::timestamptz)
                """)) {
            for (Instant anchor : anchors) {
                statement.setString(1, DEFINITION_ID.toString());
                statement.setString(2, anchor.toString());
                try (var result = statement.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject(1, UUID.class))
                            .isEqualTo(ReminderOccurrenceIdFactory.create(DEFINITION_ID, anchor));
                }
            }
        }
        try (var statement = connection.prepareStatement("""
                SELECT public.reminder_occurrence_id_v2(?::uuid, ?::timestamptz, ?)
                """)) {
            for (Instant anchor : anchors) {
                statement.setString(1, DEFINITION_ID.toString());
                statement.setString(2, anchor.toString());
                statement.setLong(3, 7L);
                try (var result = statement.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject(1, UUID.class))
                            .isEqualTo(ReminderOccurrenceIdFactory.create(
                                    DEFINITION_ID, anchor, 7L));
                }
            }
        }
    }

    private static void assertRescheduleAliases(Connection connection) throws Exception {
        Instant original = Instant.parse("2026-08-03T01:00:00.123Z");
        Instant rescheduled = Instant.parse("2026-08-05T01:00:00.123456Z");
        boolean originalAutoCommit = connection.getAutoCommit();
        boolean committed = false;
        connection.setAutoCommit(false);
        try {
            try (var insert = connection.prepareStatement("""
                    INSERT INTO public.care_tasks (
                        task_id, task_type, owner_user_id, care_subject_id,
                        title, status, scheduled_at, created_at, updated_at
                    ) VALUES (?::uuid, 'SCHEDULED_REMINDER', ?::uuid, ?::uuid,
                              'Occurrence alias fixture', 'PENDING', ?::timestamptz,
                              clock_timestamp(), clock_timestamp())
                    """)) {
                insert.setString(1, DEFINITION_ID.toString());
                insert.setString(2, OWNER_ID.toString());
                insert.setString(3, CARE_SUBJECT_ID.toString());
                insert.setString(4, original.toString());
                assertThat(insert.executeUpdate()).isOne();
            }
            try (var update = connection.prepareStatement("""
                    UPDATE public.care_tasks
                    SET scheduled_at = ?::timestamptz, updated_at = clock_timestamp()
                    WHERE task_id = ?::uuid
                    """)) {
                update.setString(1, rescheduled.toString());
                update.setString(2, DEFINITION_ID.toString());
                assertThat(update.executeUpdate()).isOne();
            }

            try (var aliases = connection.prepareStatement("""
                    SELECT occurrence_id, reminder_definition_id, owner_user_id, scheduled_at
                    FROM public.reminder_occurrence_aliases
                    WHERE reminder_definition_id = ?::uuid
                    ORDER BY scheduled_at
                    """)) {
                aliases.setString(1, DEFINITION_ID.toString());
                try (var result = aliases.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject("occurrence_id", UUID.class))
                            .isEqualTo(ReminderOccurrenceIdFactory.create(DEFINITION_ID, original));
                    assertThat(result.getObject("reminder_definition_id", UUID.class))
                            .isEqualTo(DEFINITION_ID);
                    assertThat(result.getObject("owner_user_id", UUID.class)).isEqualTo(OWNER_ID);

                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject("occurrence_id", UUID.class))
                            .isEqualTo(ReminderOccurrenceIdFactory.create(DEFINITION_ID, rescheduled));
                    assertThat(result.getObject("reminder_definition_id", UUID.class))
                            .isEqualTo(DEFINITION_ID);
                    assertThat(result.getObject("owner_user_id", UUID.class)).isEqualTo(OWNER_ID);
                    assertThat(result.next()).isFalse();
                }
            }
            connection.commit();
            committed = true;
        } finally {
            if (!committed) {
                connection.rollback();
            }
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private static void assertApplicationRoleBoundary(javax.sql.DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("SET SESSION AUTHORIZATION carebridge_application");
            try (var result = statement.executeQuery("""
                    SELECT count(*) FROM public.reminder_occurrence_aliases
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong(1)).isPositive();
            }
        }

        assertApplicationDenied(dataSource, """
                INSERT INTO public.reminder_occurrence_aliases (
                    occurrence_id, reminder_definition_id, owner_user_id, scheduled_at
                ) VALUES (
                    '84000000-0000-0000-0000-000000000001',
                    '84000000-0000-0000-0000-000000000002',
                    '10000000-0000-0000-0000-000000000004',
                    '2026-08-10T01:00:00Z'::timestamptz
                )
                """);
        assertApplicationDenied(dataSource, """
                UPDATE public.reminder_occurrence_aliases
                SET scheduled_at = scheduled_at + interval '1 minute'
                """);
        assertApplicationDenied(dataSource, """
                DELETE FROM public.reminder_occurrence_aliases
                """);
        assertApplicationDenied(dataSource, """
                ALTER TABLE public.reminder_occurrence_aliases
                ADD COLUMN forbidden_runtime_ddl text
                """);
    }

    private static void assertApplicationDenied(javax.sql.DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("SET SESSION AUTHORIZATION carebridge_application");
            assertThatThrownBy(() -> statement.execute(sql))
                    .isInstanceOf(java.sql.SQLException.class)
                    .hasMessageMatching(
                            "(?s).*(permission denied|must be owner of table reminder_occurrence_aliases).*");
        }
    }
}
