package com.carebridge.backend.baby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.audit.entity.AuditAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class BabyJourneyLinkageRemovalMigrationPostgresTest {

    private static final String PRE_REMOVAL_TARGET = "20260727030000";
    private static final String REMOVAL_VERSION = "20260728010000";

    private static final UUID BABY_OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000005");
    private static final UUID EXPERT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID MOTHER_CARE_SUBJECT_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID BABY_CARE_SUBJECT_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID MOTHER_JOURNEY_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID LEGACY_BABY_JOURNEY_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID MOTHER_TRIAGE_ID =
            UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID BABY_TRIAGE_ID =
            UUID.fromString("c0000000-0000-0000-0000-000000000002");
    private static final UUID INVALID_RELINK_TRIAGE_ID =
            UUID.fromString("c0000000-0000-0000-0000-000000000003");
    private static final UUID BABY_CONTINUATION_TOKEN =
            UUID.fromString("c0100000-0000-0000-0000-000000000002");
    private static final UUID BABY_REQUEST_ID =
            UUID.fromString("94000000-0000-0000-0000-000000000002");
    private static final UUID BABY_IDEMPOTENCY_KEY =
            UUID.fromString("94000000-0000-0000-0000-000000000102");
    private static final UUID BABY_PERMISSION_ID =
            UUID.fromString("e2000000-0000-0000-0000-000000000002");
    private static final long BABY_CONSENT_ID = 9_000_000_000_002L;
    private static final UUID MOTHER_CONTEXT_SHARE_ID =
            UUID.fromString("f5000000-0000-0000-0000-000000000001");
    private static final UUID BABY_CONTEXT_SHARE_ID =
            UUID.fromString("f5000000-0000-0000-0000-000000000002");
    private static final UUID LINK_AUDIT_EVENT_ID =
            UUID.fromString("e1000000-0000-0000-0000-000000000003");

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void upgradeDetachesBabyLinkageAndPreservesMaternalAndAuditHistory() throws Exception {
        assertThat(flyway(PRE_REMOVAL_TARGET).migrate().success).isTrue();
        seedLegacyLinkageFixture();

        assertThat(flyway(null).migrate().success).isTrue();

        assertUpgradedRows();
        assertLinkageConstraintsValidated();
        assertRelinkAttemptsRejected();
        assertMigrationTriggersEnabled();
        assertMigrationRecorded();
    }

    @Test
    void cleanBootstrapAppliesLinkageRemovalMigration() throws Exception {
        Flyway flyway = flyway(null);

        assertThat(flyway.migrate().success).isTrue();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertLinkageConstraintsValidated();
        assertMigrationTriggersEnabled();
        assertMigrationRecorded();
    }

    private void seedLegacyLinkageFixture() throws Exception {
        try (Connection connection = connection();
                var statement = connection.createStatement()) {
            assertThat(statement.executeUpdate("""
                    UPDATE care_subjects
                       SET mother_journey_id = CASE care_subject_id
                           WHEN '%s'::uuid THEN '%s'::uuid
                           WHEN '%s'::uuid THEN '%s'::uuid
                       END
                     WHERE care_subject_id IN ('%s'::uuid, '%s'::uuid)
                    """.formatted(
                    MOTHER_CARE_SUBJECT_ID,
                    MOTHER_JOURNEY_ID,
                    BABY_CARE_SUBJECT_ID,
                    LEGACY_BABY_JOURNEY_ID,
                    MOTHER_CARE_SUBJECT_ID,
                    BABY_CARE_SUBJECT_ID)))
                    .isEqualTo(2);

            assertThat(statement.executeUpdate("""
                    INSERT INTO expert_consultation_requests (
                        id, requester_user_id, expert_profile_id, client_request_id,
                        topic, description, status, expires_at, created_at, updated_at
                    ) VALUES (
                        '%s', '%s', '%s', '%s',
                        'Legacy baby safety handoff', 'Migration fixture', 'PENDING',
                        now() + interval '1 day', now(), now()
                    )
                    """.formatted(
                    BABY_REQUEST_ID, BABY_OWNER_ID, EXPERT_ID, BABY_IDEMPOTENCY_KEY)))
                    .isEqualTo(1);

            assertThat(statement.executeUpdate("""
                    INSERT INTO data_permissions (
                        permission_id, owner_user_id, grantee_user_id, purpose, status,
                        legacy_consent_id, evidence_key, created_at
                    ) VALUES (
                        '%s', '%s', '%s', 'Legacy baby safety context', 'ACTIVE',
                        %d, '%s', now()
                    )
                    """.formatted(
                    BABY_PERMISSION_ID,
                    BABY_OWNER_ID,
                    EXPERT_ID,
                    BABY_CONSENT_ID,
                    BABY_IDEMPOTENCY_KEY)))
                    .isEqualTo(1);

            assertThat(statement.executeUpdate("""
                    INSERT INTO triage_sessions (
                        triage_session_id, user_id, care_subject_id, stage, risk_level,
                        status, emergency, disclaimer_version, symptoms, created_by,
                        result_jsonb, schema_version, content_hash, baby_profile_id,
                        journey_id, origin_dashboard, origin_reference_id, continuation_token,
                        continuation_expires_at, created_at, completed_at, updated_at
                    ) VALUES (
                        '%s', '%s', '%s', 'INFANT', 'YELLOW',
                        'COMPLETED', false, '1.0', 'Legacy baby fever', '%s',
                        '{"risk_level":"YELLOW"}'::jsonb, '1',
                        'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                        '%s', '%s', 'BABY_PROFILE', '%s', '%s', now() + interval '1 day',
                        now(), now(), now()
                    )
                    """.formatted(
                    BABY_TRIAGE_ID,
                    BABY_OWNER_ID,
                    BABY_CARE_SUBJECT_ID,
                    BABY_OWNER_ID,
                    BABY_CARE_SUBJECT_ID,
                    LEGACY_BABY_JOURNEY_ID,
                    BABY_CARE_SUBJECT_ID,
                    BABY_CONTINUATION_TOKEN)))
                    .isEqualTo(1);

            assertThat(statement.executeUpdate("""
                    INSERT INTO consultation_context_shares (
                        context_share_id, consultation_request_id, owner_user_id,
                        intake_session_id, expert_profile_id, consent_grant_id,
                        idempotency_key, journey_id, origin_dashboard, origin_reference_id,
                        triage_stage, risk_level, intake_status, risk_summary,
                        share_policy_version, created_at
                    ) VALUES (
                        '%s', '%s', '%s', '%s', '%s', %d,
                        '%s', '%s', 'BABY_PROFILE', '%s',
                        'INFANT', 'YELLOW', 'COMPLETED', 'Legacy baby safety summary',
                        'YELLOW_EXPERT_CONTEXT_V1', now()
                    )
                    """.formatted(
                    BABY_CONTEXT_SHARE_ID,
                    BABY_REQUEST_ID,
                    BABY_OWNER_ID,
                    BABY_TRIAGE_ID,
                    EXPERT_ID,
                    BABY_CONSENT_ID,
                    BABY_IDEMPOTENCY_KEY,
                    LEGACY_BABY_JOURNEY_ID,
                    BABY_CARE_SUBJECT_ID)))
                    .isEqualTo(1);

            assertThat(statement.executeUpdate("""
                    INSERT INTO audit_events (
                        audit_event_id, actor_user_id, event_category, subject_user_id,
                        subject_reference_id, resource_type, resource_id, decision,
                        after_payload_jsonb, occurred_at, created_at
                    ) VALUES (
                        '%s', '%s', 'BABY_JOURNEY_LINK_ACCEPTED', '%s',
                        '%s', 'CARE_SUBJECT', '%s', 'ALLOW',
                        jsonb_build_object(
                            'careSubjectId', '%s',
                            'motherJourneyId', '%s'),
                        now(), now()
                    )
                    """.formatted(
                    LINK_AUDIT_EVENT_ID,
                    BABY_OWNER_ID,
                    BABY_OWNER_ID,
                    BABY_CARE_SUBJECT_ID,
                    BABY_CARE_SUBJECT_ID,
                    BABY_CARE_SUBJECT_ID,
                    LEGACY_BABY_JOURNEY_ID)))
                    .isEqualTo(1);
        }
    }

    private void assertUpgradedRows() throws Exception {
        try (Connection connection = connection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT
                            (SELECT mother_journey_id FROM care_subjects
                              WHERE care_subject_id = '%s') AS baby_subject_journey,
                            (SELECT count(*) FROM care_subjects
                              WHERE care_subject_id = '%s') AS baby_subject_count,
                            (SELECT mother_journey_id FROM care_subjects
                              WHERE care_subject_id = '%s') AS mother_subject_journey,
                            (SELECT journey_id FROM triage_sessions
                              WHERE triage_session_id = '%s') AS baby_triage_journey,
                            (SELECT origin_dashboard FROM triage_sessions
                              WHERE triage_session_id = '%s') AS baby_triage_origin,
                            (SELECT origin_reference_id FROM triage_sessions
                              WHERE triage_session_id = '%s') AS baby_triage_reference,
                            (SELECT journey_id FROM triage_sessions
                              WHERE triage_session_id = '%s') AS mother_triage_journey,
                            (SELECT journey_id FROM consultation_context_shares
                              WHERE context_share_id = '%s') AS baby_context_journey,
                            (SELECT origin_dashboard FROM consultation_context_shares
                              WHERE context_share_id = '%s') AS baby_context_origin,
                            (SELECT origin_reference_id FROM consultation_context_shares
                              WHERE context_share_id = '%s') AS baby_context_reference,
                            (SELECT journey_id FROM consultation_context_shares
                              WHERE context_share_id = '%s') AS mother_context_journey,
                            (SELECT count(*) FROM mother_journeys
                              WHERE journey_id IN ('%s', '%s')) AS journey_count,
                            (SELECT after_payload_jsonb ->> 'careSubjectId' FROM audit_events
                              WHERE audit_event_id = '%s') AS audit_subject,
                            (SELECT after_payload_jsonb ->> 'motherJourneyId' FROM audit_events
                              WHERE audit_event_id = '%s') AS audit_journey,
                            (SELECT event_category FROM audit_events
                              WHERE audit_event_id = '%s') AS audit_category
                        """.formatted(
                        BABY_CARE_SUBJECT_ID,
                        BABY_CARE_SUBJECT_ID,
                        MOTHER_CARE_SUBJECT_ID,
                        BABY_TRIAGE_ID,
                        BABY_TRIAGE_ID,
                        BABY_TRIAGE_ID,
                        MOTHER_TRIAGE_ID,
                        BABY_CONTEXT_SHARE_ID,
                        BABY_CONTEXT_SHARE_ID,
                        BABY_CONTEXT_SHARE_ID,
                        MOTHER_CONTEXT_SHARE_ID,
                        MOTHER_JOURNEY_ID,
                        LEGACY_BABY_JOURNEY_ID,
                        LINK_AUDIT_EVENT_ID,
                        LINK_AUDIT_EVENT_ID,
                        LINK_AUDIT_EVENT_ID))) {
            assertThat(result.next()).isTrue();
            assertThat(result.getObject("baby_subject_journey", UUID.class)).isNull();
            assertThat(result.getLong("baby_subject_count")).isEqualTo(1);
            assertThat(result.getObject("mother_subject_journey", UUID.class))
                    .isEqualTo(MOTHER_JOURNEY_ID);
            assertThat(result.getObject("baby_triage_journey", UUID.class)).isNull();
            assertThat(result.getString("baby_triage_origin")).isEqualTo("BABY_PROFILE");
            assertThat(result.getObject("baby_triage_reference", UUID.class))
                    .isEqualTo(BABY_CARE_SUBJECT_ID);
            assertThat(result.getObject("mother_triage_journey", UUID.class))
                    .isEqualTo(MOTHER_JOURNEY_ID);
            assertThat(result.getObject("baby_context_journey", UUID.class)).isNull();
            assertThat(result.getString("baby_context_origin")).isEqualTo("BABY_PROFILE");
            assertThat(result.getObject("baby_context_reference", UUID.class))
                    .isEqualTo(BABY_CARE_SUBJECT_ID);
            assertThat(result.getObject("mother_context_journey", UUID.class))
                    .isEqualTo(MOTHER_JOURNEY_ID);
            assertThat(result.getLong("journey_count")).isEqualTo(2);
            assertThat(result.getString("audit_subject"))
                    .isEqualTo(BABY_CARE_SUBJECT_ID.toString());
            assertThat(result.getString("audit_journey"))
                    .isEqualTo(LEGACY_BABY_JOURNEY_ID.toString());
            assertThat(AuditAction.valueOf(result.getString("audit_category")))
                    .isEqualTo(AuditAction.BABY_JOURNEY_LINK_ACCEPTED);
        }
    }

    private void assertLinkageConstraintsValidated() throws Exception {
        Map<String, Boolean> constraints = new LinkedHashMap<>();
        try (Connection connection = connection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT conname, convalidated
                          FROM pg_constraint
                         WHERE conname IN (
                             'care_subjects_journey_fk',
                             'fk_context_journey_owner',
                             'care_subjects_baby_no_mother_journey_ck',
                             'chk_triage_lifecycle_binding',
                             'chk_triage_origin_stage',
                             'chk_context_origin_journey',
                             'fk_context_intake_snapshot_core',
                             'fk_context_intake_snapshot')
                        """)) {
            while (result.next()) {
                constraints.put(result.getString("conname"), result.getBoolean("convalidated"));
            }
        }

        assertThat(constraints).containsExactlyInAnyOrderEntriesOf(Map.of(
                "care_subjects_journey_fk", true,
                "fk_context_journey_owner", true,
                "care_subjects_baby_no_mother_journey_ck", true,
                "chk_triage_lifecycle_binding", true,
                "chk_triage_origin_stage", true,
                "chk_context_origin_journey", true,
                "fk_context_intake_snapshot_core", true,
                "fk_context_intake_snapshot", true));
    }

    private void assertRelinkAttemptsRejected() throws Exception {
        assertConstraintRejects("""
                UPDATE care_subjects
                   SET mother_journey_id = '%s'
                 WHERE care_subject_id = '%s'
                """.formatted(LEGACY_BABY_JOURNEY_ID, BABY_CARE_SUBJECT_ID),
                "care_subjects_baby_no_mother_journey_ck");

        assertConstraintRejects("""
                INSERT INTO triage_sessions (
                    triage_session_id, user_id, care_subject_id, stage, risk_level,
                    status, emergency, symptoms, created_by, baby_profile_id, journey_id,
                    origin_dashboard, origin_reference_id, continuation_token,
                    continuation_expires_at
                ) VALUES (
                    '%s', '%s', '%s', 'INFANT', 'YELLOW',
                    'PENDING', false, 'Invalid baby relink', '%s', '%s', '%s',
                    'BABY_PROFILE', '%s', gen_random_uuid(), now() + interval '1 day'
                )
                """.formatted(
                INVALID_RELINK_TRIAGE_ID,
                BABY_OWNER_ID,
                BABY_CARE_SUBJECT_ID,
                BABY_OWNER_ID,
                BABY_CARE_SUBJECT_ID,
                LEGACY_BABY_JOURNEY_ID,
                BABY_CARE_SUBJECT_ID),
                "chk_triage_lifecycle_binding");

        assertContextMutationRejected(
                "journey_id = '%s'".formatted(LEGACY_BABY_JOURNEY_ID),
                "chk_context_origin_journey");
        assertContextMutationRejected(
                "origin_reference_id = '%s'".formatted(LEGACY_BABY_JOURNEY_ID),
                "fk_context_intake_snapshot_core");
    }

    private void assertContextMutationRejected(String assignment, String constraintName)
            throws Exception {
        try (Connection connection = connection();
                var statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                statement.execute("""
                        ALTER TABLE consultation_context_shares
                            DISABLE TRIGGER trg_consultation_context_shares_append_only
                        """);
                assertThatThrownBy(() -> statement.executeUpdate("""
                        UPDATE consultation_context_shares
                           SET %s
                         WHERE context_share_id = '%s'
                        """.formatted(assignment, BABY_CONTEXT_SHARE_ID)))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining(constraintName);
            } finally {
                connection.rollback();
            }
        }
    }

    private void assertConstraintRejects(String sql, String constraintName) throws Exception {
        try (Connection connection = connection();
                var statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate(sql))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining(constraintName);
        }
    }

    private void assertMigrationTriggersEnabled() throws Exception {
        Map<String, String> triggers = new LinkedHashMap<>();
        try (Connection connection = connection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT tgname, tgenabled
                          FROM pg_trigger
                         WHERE (tgrelid = 'public.triage_sessions'::regclass
                                AND tgname = 'triage_completed_snapshot_guard_trg')
                            OR (tgrelid = 'public.consultation_context_shares'::regclass
                                AND tgname = 'trg_consultation_context_shares_append_only')
                        """)) {
            while (result.next()) {
                triggers.put(result.getString("tgname"), result.getString("tgenabled"));
            }
        }

        assertThat(triggers).containsExactlyInAnyOrderEntriesOf(Map.of(
                "triage_completed_snapshot_guard_trg", "O",
                "trg_consultation_context_shares_append_only", "O"));
    }

    private void assertMigrationRecorded() throws Exception {
        try (Connection connection = connection();
                var statement = connection.prepareStatement("""
                        SELECT count(*)
                          FROM flyway_schema_history
                         WHERE version = ?
                           AND success
                        """)) {
            statement.setString(1, REMOVAL_VERSION);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong(1)).isEqualTo(1);
            }
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration-legacy")
                .baselineOnMigrate(true)
                .outOfOrder(true);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
