package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.time.Duration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Production-shaped upgrade evidence for the checklist P1/P2 migrations. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistP1P2BackfillEmbeddedPostgresTest {

    @Test
    @Timeout(180)
    void backfillStampsLegacyRowsAndFailsClosedOnFamilyDuplicate() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start()) {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);

            Flyway p1 = flyway(dataSource, "20260811120000");
            assertThat(p1.migrate().success).isTrue();

            try (Connection connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {
                statement.executeUpdate("""
                    INSERT INTO public.care_group_members
                        (care_group_member_id, care_group_id, user_id, member_role,
                         invitation_status, permission_json, created_at, updated_at)
                    VALUES
                        ('51000000-0000-0000-0000-000000000099',
                         '50000000-0000-0000-0000-000000000001',
                         '10000000-0000-0000-0000-000000000006',
                         'CO_CAREGIVER', 'ACCEPTED',
                         '{"CHECKLIST_VIEW": true, "CHECKLIST_COMPLETE": true}'::jsonb,
                         now(), now())
                    ON CONFLICT (care_group_member_id) DO NOTHING
                    """);
            }

            Flyway p2 = flyway(dataSource, null);
            assertThat(p2.migrate().success).isTrue();
            assertThat(p2.migrate().migrationsExecuted).isZero();

            try (Connection connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {
                // The fixture grants the deployment owner the schema-owner
                // role so Flyway can run the migration. Exercise the actual
                // application identity here; it must not spoof the migration
                // GUC and mutate frozen V1 rows.
                //
                // The production ACL intentionally does not grant the
                // application role raw UPDATE on checklist templates. Grant
                // only the table privileges needed by this trigger contract
                // assertion so a permission error cannot mask the barrier.
                statement.execute("GRANT USAGE ON SCHEMA public TO carebridge_application");
                statement.execute("GRANT SELECT, UPDATE ON public.care_item_templates TO carebridge_application");
                statement.execute("GRANT SELECT ON public.mother_journeys, public.audit_events, public.care_group_members TO carebridge_application");
                try (var privileges = statement.executeQuery("""
                        SELECT has_function_privilege(
                            'carebridge_application',
                            'public.checklist_p2_access_timeline_valid(uuid,jsonb)',
                            'EXECUTE')
                        """)) {
                    assertThat(privileges.next()).isTrue();
                    assertThat(privileges.getBoolean(1)).isTrue();
                }
                statement.execute("SET ROLE carebridge_application");
                statement.execute("select set_config('carebridge.checklist_v1_writes_frozen', 'true', false)");
                assertThatThrownBy(() -> statement.executeUpdate("""
                        UPDATE public.care_item_templates
                           SET title=title
                         WHERE template_id='60000000-0000-0000-0000-000000000001'
                        """))
                        .hasMessageContaining("CHECKLIST_V1_WRITES_FROZEN");
                try (var journey = statement.executeQuery("""
                        SELECT gestational_dating_basis, gestational_dating_revision,
                               gestational_dating_effective_at
                          FROM public.mother_journeys
                         WHERE journey_id='40000000-0000-0000-0000-000000000001'
                        """)) {
                    assertThat(journey.next()).isTrue();
                    assertThat(journey.getString("gestational_dating_basis")).isEqualTo("LMP");
                    assertThat(journey.getLong("gestational_dating_revision")).isEqualTo(1L);
                    assertThat(journey.getTimestamp("gestational_dating_effective_at")).isNotNull();
                }
                try (var events = statement.executeQuery("""
                        SELECT count(*), min(subject_reference_id::text),
                               min(resource_type), min(resource_id::text),
                               min(event_origin), min(payload->>'journeyVersion'),
                               min(payload->>'inferredSource'), min(payload->>'correlationId')
                          FROM public.audit_events
                         WHERE event_category='MOTHER_JOURNEY_TRANSITION'
                           AND event_origin='JOURNEY_EVENT'
                           AND subject_reference_id='40000000-0000-0000-0000-000000000001'
                           AND resource_type='mother_journeys'
                           AND resource_id='40000000-0000-0000-0000-000000000001'
                           AND payload->>'eventType'='PREGNANCY_EPOCH_STARTED'
                           AND payload->>'gestationalDatingRevision'='1'
                           AND payload->>'journeyVersion' ~ '^[0-9]+$'
                           AND payload->>'inferredSource'='true'
                        """)) {
                    assertThat(events.next()).isTrue();
                    assertThat(events.getLong(1)).isEqualTo(1L);
                    assertThat(events.getString(2))
                            .isEqualTo("40000000-0000-0000-0000-000000000001");
                    assertThat(events.getString(3)).isEqualTo("mother_journeys");
                    assertThat(events.getString(4))
                            .isEqualTo("40000000-0000-0000-0000-000000000001");
                    assertThat(events.getString(5)).isEqualTo("JOURNEY_EVENT");
                    assertThat(events.getString(6)).isNotBlank();
                    assertThat(events.getString(7)).isEqualTo("true");
                    assertThat(events.getString(8)).isNotBlank();
                }
                try (var members = statement.executeQuery("""
                        SELECT count(*)
                          FROM public.care_group_members
                         WHERE care_group_id='50000000-0000-0000-0000-000000000001'
                           AND user_id='10000000-0000-0000-0000-000000000006'
                           AND invitation_status='REVOKED'
                           AND checklist_access_quarantine_reason_code='FAMILY_MEMBER_DUPLICATE'
                        """)) {
                    assertThat(members.next()).isTrue();
                    assertThat(members.getLong(1)).isEqualTo(2L);
                }
                try (var audits = statement.executeQuery("""
                        SELECT count(*)
                          FROM public.audit_events
                         WHERE event_category='CHECKLIST_ACCESS_REVOKED'
                           AND resource_type='CARE_GROUP_MEMBER'
                           AND actor_service='CHECKLIST_P2_BACKFILL'
                        """)) {
                    assertThat(audits.next()).isTrue();
                    assertThat(audits.getLong(1)).isEqualTo(2L);
                }
            }
        }
    }

    @Test
    @Timeout(180)
    void p2FailsClosedWhenLegacyAccessAuditReferencesUnknownMember() throws Exception {
        try (EmbeddedPostgres postgres = databaseBeforeP1()) {
            var dataSource = postgres.getPostgresDatabase();
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {
                statement.executeUpdate("""
                    INSERT INTO public.audit_events
                        (audit_event_id, actor_user_id, actor_type, actor_service,
                         event_category, resource_type, resource_id, reason_code,
                         before_payload_jsonb, after_payload_jsonb, correlation_id,
                         event_origin, occurred_at, created_at)
                    VALUES
                        ('91000000-0000-0000-0000-000000000001', NULL,
                         'SYSTEM', 'CHECKLIST_P2_BACKFILL',
                         'CHECKLIST_ACCESS_BASELINE', 'CARE_GROUP_MEMBER',
                         '91000000-0000-0000-0000-000000000099',
                         'LEGACY_ACCESS_BASELINE',
                         '{"schema":"CHECKLIST_ACCESS_AUDIT_V1","eventType":"LEGACY_ACCESS_BASELINE","membershipStatus":"ACCEPTED","checklistView":true,"checklistComplete":false,"accessEpoch":0,"effectiveFrom":"2026-08-11T00:00:00Z","correlationId":"91000000-0000-0000-0000-000000000002"}'::jsonb,
                         '{"schema":"CHECKLIST_ACCESS_AUDIT_V1","eventType":"LEGACY_ACCESS_BASELINE","membershipStatus":"ACCEPTED","checklistView":true,"checklistComplete":false,"accessEpoch":1,"effectiveFrom":"2026-08-11T00:00:00Z","correlationId":"91000000-0000-0000-0000-000000000002"}'::jsonb,
                         '91000000-0000-0000-0000-000000000002',
                         'CHECKLIST_ACCESS', now(), now())
                    """);
            }

            assertThat(flyway(dataSource, "20260811120000").migrate().success).isTrue();
            assertThatThrownBy(() -> flyway(dataSource, null).migrate())
                    .hasMessageContaining("CHECKLIST_ACCESS_AUDIT_MEMBER_NOT_FOUND");
        }
    }

    @Test
    @Timeout(180)
    void p2FailsClosedWhenLegacyQuarantinePayloadIsMalformed() throws Exception {
        try (EmbeddedPostgres postgres = databaseBeforeP1()) {
            var dataSource = postgres.getPostgresDatabase();
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {
                statement.executeUpdate("""
                    INSERT INTO public.audit_events
                        (audit_event_id, actor_user_id, actor_type, actor_service,
                         event_category, resource_type, resource_id, reason_code,
                         payload, correlation_id, event_origin, occurred_at, created_at)
                    VALUES
                        ('92000000-0000-0000-0000-000000000001', NULL,
                         'SERVICE', 'CHECKLIST_LEGACY_BACKFILL',
                         'CHECKLIST_MIGRATION_QUARANTINED', 'LEGACY_CHECKLIST_ITEM',
                         '92000000-0000-0000-0000-000000000099',
                         'TASK_PARENT_CONTRACT_MISMATCH',
                         '{"sourceTable":"preparation_checklist_items"}'::jsonb,
                         '92000000-0000-0000-0000-000000000002',
                         'CHECKLIST_MIGRATION', now(), now())
                    """);
            }

            assertThat(flyway(dataSource, "20260811120000").migrate().success).isTrue();
            assertThatThrownBy(() -> flyway(dataSource, null).migrate())
                    .hasMessageContaining("audit_events_checklist_migration_payload_ck");
        }
    }

    private static EmbeddedPostgres databaseBeforeP1() throws Exception {
        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(30))
                .setServerConfig("max_connections", "100")
                .start();
        try {
            var dataSource = postgres.getPostgresDatabase();
            EmbeddedPostgresRoleFixture.provision(dataSource);
            assertThat(flyway(dataSource, "20260810120000").migrate().success).isTrue();
            return postgres;
        } catch (Exception exception) {
            postgres.close();
            throw exception;
        }
    }

    private static Flyway flyway(javax.sql.DataSource dataSource, String target) {
        var configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                // Explicit test-harness equivalent of the deployment runner's
                // pre-set writer-freeze/session-role GUCs.  P2 must not create
                // its own bypass inside migration SQL.
                .initSql("select set_config('carebridge.checklist_v1_writes_frozen', 'true', false);"
                        + " select set_config('carebridge.checklist_p1_p2_role', 'MIGRATION', false);")
                .cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
