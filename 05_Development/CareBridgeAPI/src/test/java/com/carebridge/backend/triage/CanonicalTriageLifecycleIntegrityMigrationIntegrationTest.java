package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class CanonicalTriageLifecycleIntegrityMigrationIntegrationTest {

    private static final MigrationVersion CUT_OVER =
            MigrationVersion.fromVersion("20260722231900");
    private static final MigrationVersion PRE_STORY_67 =
            MigrationVersion.fromVersion("20260722120000");
    private static final MigrationVersion STORY_67 =
            MigrationVersion.fromVersion("20260722210000");
    private static final MigrationVersion LIFECYCLE_BRIDGE =
            MigrationVersion.fromVersion("20260722231350");
    private static final MigrationVersion CANONICAL =
            MigrationVersion.fromVersion("20260724211000");

    private static final String OWNER = "71000000-0000-0000-0000-000000000001";
    private static final String OTHER_OWNER = "71000000-0000-0000-0000-000000000002";
    private static final String SUBJECT = "71000000-0000-0000-0000-000000000011";
    private static final String JOURNEY = "71000000-0000-0000-0000-000000000021";
    private static final String TRIAGE = "71000000-0000-0000-0000-000000000031";
    private static final String EMERGENCY = "71000000-0000-0000-0000-000000000041";
    private static final String SOURCE = "71000000-0000-0000-0000-000000000051";
    private static final String BOUND_TRIAGE = "71000000-0000-0000-0000-000000000032";
    private static final String OTHER_TRIAGE = "71000000-0000-0000-0000-000000000033";
    private static final String PENDING_TRIAGE = "71000000-0000-0000-0000-000000000034";
    private static final String IMU_EVENT = "71000000-0000-0000-0000-000000000042";
    private static final String UNLINKED_EMERGENCY =
            "71000000-0000-0000-0000-000000000043";
    private static final String WRONG_LINK_EMERGENCY =
            "71000000-0000-0000-0000-000000000044";

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void cleanBootstrapInstallsCanonicalIntegrityAndRemovesBridge() throws Exception {
        migrate(CANONICAL, false);

        assertThat(number("""
                SELECT count(*)
                  FROM information_schema.tables
                 WHERE (table_schema='carebridge_migration_bridge'
                        AND table_name IN (
                            'triage_lifecycle_bridge',
                            'lifecycle_safety_outcome_bridge'))
                    OR (table_schema='public'
                        AND table_name='triage_lifecycle_bridge')
                """)).isZero();
        assertThat(number("""
                SELECT count(*)
                  FROM pg_proc routine
                  JOIN pg_namespace namespace
                    ON namespace.oid=routine.pronamespace
                 WHERE namespace.nspname='carebridge_migration_bridge'
                   AND routine.proname='sync_triage_lifecycle_bridge'
                """)).isZero();
        assertThat(columns("mother_journey_events",
                "triage_session_id", "emergency_session_id", "risk_level", "stage",
                "origin_dashboard", "origin_reference_id", "origin_action"))
                .isEqualTo(7);
        assertThat(number("SELECT count(*) FROM pg_trigger WHERE tgname="
                + "'triage_completed_snapshot_guard_trg' AND tgenabled <> 'D'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM pg_trigger WHERE tgname="
                + "'mother_journey_events_immutable_trg' AND tgenabled <> 'D'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM pg_trigger WHERE tgname="
                + "'mother_journey_events_00_safety_source_trg' AND tgenabled <> 'D'"))
                .isOne();
    }

    @Test
    void unpublishedLowerVersionCanRunOutOfOrderAfterCanonicalCutover() throws Exception {
        migrate(CUT_OVER, false);
        assertThat(exists("intake_sessions")).isFalse();
        execute("DROP SCHEMA IF EXISTS carebridge_migration_bridge CASCADE");
        execute("""
                DELETE FROM flyway_schema_history
                 WHERE version IN (
                    '20260722119950',
                    '20260722210000',
                    '20260722231350',
                    '20260722231360')
                """);

        migrate(CUT_OVER, true);
        migrate(CANONICAL, true);

        assertThat(number("""
                SELECT count(*)
                  FROM information_schema.tables
                 WHERE (table_schema='carebridge_migration_bridge'
                        AND table_name IN (
                            'triage_lifecycle_bridge',
                            'lifecycle_safety_outcome_bridge'))
                    OR (table_schema='public'
                        AND table_name='triage_lifecycle_bridge')
                """)).isZero();
        assertThat(number("""
                SELECT count(*)
                  FROM pg_proc routine
                  JOIN pg_namespace namespace
                    ON namespace.oid=routine.pronamespace
                 WHERE namespace.nspname='carebridge_migration_bridge'
                   AND routine.proname='sync_triage_lifecycle_bridge'
                """)).isZero();
        assertThat(columns("triage_sessions", "journey_id", "origin_dashboard",
                "origin_reference_id", "continuation_token",
                "continuation_expires_at", "continuation_acknowledged_at"))
                .isEqualTo(6);
    }

    @Test
    void appliedHistoricalStory67ChainBridgesBeforePhase2AndUpgrades() throws Exception {
        migrate(PRE_STORY_67, false);
        seedLegacyLifecycleAggregate();
        migrate(STORY_67, false);
        execute("""
                UPDATE intake_sessions
                   SET journey_id='71000000-0000-0000-0000-000000000021',
                       origin_dashboard='MOTHER_JOURNEY',
                       origin_reference_id='71000000-0000-0000-0000-000000000021',
                       continuation_token='71000000-0000-0000-0000-000000000071',
                       continuation_expires_at=now()+interval '7 days'
                 WHERE id='71000000-0000-0000-0000-000000000031'
                """);
        execute("""
                INSERT INTO lifecycle_safety_outcomes (
                    outcome_id,owner_user_id,journey_id,intake_session_id,
                    emergency_session_id,risk_level,stage,origin_dashboard,
                    origin_reference_id,origin_action,occurred_at,recorded_at)
                VALUES ('71000000-0000-0000-0000-000000000061',
                    '71000000-0000-0000-0000-000000000001',
                    '71000000-0000-0000-0000-000000000021',
                    '71000000-0000-0000-0000-000000000031',NULL,
                    'YELLOW','PREGNANCY','MOTHER_JOURNEY',
                    '71000000-0000-0000-0000-000000000021',
                    'RETURN_TO_MOTHER_JOURNEY',now(),now())
                """);

        migrate(LIFECYCLE_BRIDGE, false);

        assertThat(number("""
                SELECT count(*)
                  FROM carebridge_migration_bridge.triage_lifecycle_bridge
                 WHERE triage_session_id='71000000-0000-0000-0000-000000000031'
                   AND journey_id='71000000-0000-0000-0000-000000000021'
                   AND continuation_token='71000000-0000-0000-0000-000000000071'
                """)).isOne();
        assertThat(exists("lifecycle_safety_outcomes")).isFalse();
        assertThat(number("""
                SELECT count(*)
                  FROM carebridge_migration_bridge.lifecycle_safety_outcome_bridge
                 WHERE outcome_id='71000000-0000-0000-0000-000000000061'
                   AND triage_session_id='71000000-0000-0000-0000-000000000031'
                """)).isOne();

        migrate(CANONICAL, false);

        assertThat(number("""
                SELECT count(*) FROM triage_sessions
                 WHERE triage_session_id='71000000-0000-0000-0000-000000000031'
                   AND journey_id='71000000-0000-0000-0000-000000000021'
                   AND origin_dashboard='MOTHER_JOURNEY'
                   AND origin_reference_id='71000000-0000-0000-0000-000000000021'
                   AND continuation_token='71000000-0000-0000-0000-000000000071'
                """)).isOne();
        assertThat(number("""
                SELECT count(*) FROM mother_journey_events
                 WHERE event_type='SAFETY_OUTCOME'
                   AND triage_session_id='71000000-0000-0000-0000-000000000031'
                   AND risk_level='YELLOW'
                """)).isOne();
    }

    @Test
    void lifecycleBridgeWaitsForWriterAndCapturesCommittedBindingAndOutcome()
            throws Exception {
        migrate(PRE_STORY_67, false);
        seedLegacyLifecycleAggregate();
        migrate(STORY_67, false);

        try (Connection writer = connection()) {
            writer.setAutoCommit(false);
            try (var update = writer.prepareStatement("""
                    UPDATE intake_sessions
                       SET journey_id=?, origin_dashboard='MOTHER_JOURNEY',
                           origin_reference_id=?, continuation_token=?,
                           continuation_expires_at='2026-07-31T00:00:00Z'
                     WHERE id=?
                    """)) {
                update.setObject(1, java.util.UUID.fromString(JOURNEY));
                update.setObject(2, java.util.UUID.fromString(JOURNEY));
                update.setObject(3, java.util.UUID.fromString(
                        "71000000-0000-0000-0000-000000000075"));
                update.setObject(4, java.util.UUID.fromString(TRIAGE));
                assertThat(update.executeUpdate()).isOne();
            }
            try (var insert = writer.prepareStatement("""
                    INSERT INTO lifecycle_safety_outcomes (
                        outcome_id,owner_user_id,journey_id,intake_session_id,
                        emergency_session_id,risk_level,stage,origin_dashboard,
                        origin_reference_id,origin_action,occurred_at,recorded_at)
                    VALUES (?,?,?,?,NULL,'YELLOW','PREGNANCY','MOTHER_JOURNEY',?,
                            'RETURN_TO_MOTHER_JOURNEY',
                            '2026-07-24T01:00:00Z','2026-07-24T01:00:01Z')
                    """)) {
                insert.setObject(1, java.util.UUID.fromString(
                        "71000000-0000-0000-0000-000000000062"));
                insert.setObject(2, java.util.UUID.fromString(OWNER));
                insert.setObject(3, java.util.UUID.fromString(JOURNEY));
                insert.setObject(4, java.util.UUID.fromString(TRIAGE));
                insert.setObject(5, java.util.UUID.fromString(JOURNEY));
                assertThat(insert.executeUpdate()).isOne();
            }

            try (var executor = Executors.newSingleThreadExecutor()) {
                var migration = executor.submit(() -> migrate(LIFECYCLE_BRIDGE, false));
                assertThatThrownBy(() -> migration.get(300, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
                writer.commit();
                migration.get(15, TimeUnit.SECONDS);
            }
        }

        assertThat(number("""
                SELECT count(*)
                  FROM carebridge_migration_bridge.triage_lifecycle_bridge
                 WHERE triage_session_id='71000000-0000-0000-0000-000000000031'
                   AND journey_id='71000000-0000-0000-0000-000000000021'
                   AND origin_dashboard='MOTHER_JOURNEY'
                   AND origin_reference_id='71000000-0000-0000-0000-000000000021'
                   AND continuation_token='71000000-0000-0000-0000-000000000075'
                   AND continuation_expires_at='2026-07-31T00:00:00Z'
                """)).isOne();
        assertThat(number("""
                SELECT count(*)
                  FROM carebridge_migration_bridge.lifecycle_safety_outcome_bridge
                 WHERE outcome_id='71000000-0000-0000-0000-000000000062'
                   AND triage_session_id='71000000-0000-0000-0000-000000000031'
                   AND occurred_at='2026-07-24T01:00:00Z'
                   AND recorded_at='2026-07-24T01:00:01Z'
                """)).isOne();
        assertThat(exists("lifecycle_safety_outcomes")).isFalse();
        assertThat(number("SELECT count(*) FROM pg_trigger WHERE tgname="
                + "'triage_lifecycle_bridge_sync_trg' AND tgenabled <> 'D'"))
                .isOne();
    }

    @Test
    void canonicalUpgradeEnrichesPartialBindingWhenLegacyContinuationWasNull()
            throws Exception {
        migrate(PRE_STORY_67, false);
        seedLegacyLifecycleAggregate();
        migrate(STORY_67, false);
        execute("""
                UPDATE intake_sessions
                   SET journey_id='71000000-0000-0000-0000-000000000021',
                       origin_dashboard='MOTHER_JOURNEY',
                       origin_reference_id='71000000-0000-0000-0000-000000000021',
                       continuation_token='71000000-0000-0000-0000-000000000076',
                       continuation_expires_at=now()+interval '7 days'
                 WHERE id='71000000-0000-0000-0000-000000000031'
                """);

        migrate(LIFECYCLE_BRIDGE, false);
        execute("""
                UPDATE carebridge_migration_bridge.triage_lifecycle_bridge
                   SET continuation_token=NULL,
                       continuation_expires_at=NULL,
                       continuation_acknowledged_at=NULL
                 WHERE triage_session_id='71000000-0000-0000-0000-000000000031'
                """);

        assertThat(number("""
                SELECT count(*)
                  FROM carebridge_migration_bridge.triage_lifecycle_bridge
                 WHERE triage_session_id='71000000-0000-0000-0000-000000000031'
                   AND journey_id='71000000-0000-0000-0000-000000000021'
                   AND continuation_token IS NULL
                   AND continuation_expires_at IS NULL
                   AND continuation_acknowledged_at IS NULL
                """)).isOne();

        migrate(CANONICAL, false);

        assertThat(number("""
                SELECT count(*)
                  FROM triage_sessions
                 WHERE triage_session_id='71000000-0000-0000-0000-000000000031'
                   AND journey_id='71000000-0000-0000-0000-000000000021'
                   AND origin_dashboard='MOTHER_JOURNEY'
                   AND origin_reference_id='71000000-0000-0000-0000-000000000021'
                   AND continuation_token IS NOT NULL
                   AND continuation_expires_at <= completed_at
                   AND continuation_acknowledged_at IS NOT NULL
                """)).isOne();
    }

    @Test
    void upgradeReconcilesLegacyOutcomeBackfillsEvidenceAndMapsRedEmergency() throws Exception {
        migrate(CUT_OVER, false);
        seedCanonicalAggregate();
        execute("""
                CREATE TABLE lifecycle_safety_outcomes (
                    outcome_id uuid PRIMARY KEY,
                    owner_user_id uuid NOT NULL,
                    journey_id uuid NOT NULL,
                    intake_session_id uuid NOT NULL UNIQUE,
                    emergency_session_id uuid,
                    risk_level varchar(10) NOT NULL,
                    stage varchar(20) NOT NULL,
                    origin_dashboard varchar(30) NOT NULL,
                    origin_reference_id uuid NOT NULL,
                    origin_action varchar(40) NOT NULL,
                    occurred_at timestamptz NOT NULL,
                    recorded_at timestamptz NOT NULL
                );
                INSERT INTO lifecycle_safety_outcomes VALUES (
                    '71000000-0000-0000-0000-000000000061',
                    '71000000-0000-0000-0000-000000000001',
                    '71000000-0000-0000-0000-000000000021',
                    '71000000-0000-0000-0000-000000000031',
                    '71000000-0000-0000-0000-000000000041',
                    'RED','PREGNANCY','MOTHER_JOURNEY',
                    '71000000-0000-0000-0000-000000000021',
                    'RETURN_TO_MOTHER_JOURNEY',now(),now());
                """);

        migrate(CANONICAL, false);

        assertThat(exists("lifecycle_safety_outcomes")).isFalse();
        assertThat(number("""
                SELECT count(*) FROM mother_journey_events
                WHERE event_type='SAFETY_OUTCOME'
                  AND owner_user_id='71000000-0000-0000-0000-000000000001'
                  AND mother_journey_id='71000000-0000-0000-0000-000000000021'
                  AND triage_session_id='71000000-0000-0000-0000-000000000031'
                  AND emergency_session_id='71000000-0000-0000-0000-000000000041'
                  AND risk_level='RED' AND stage='PREGNANCY'
                  AND origin_dashboard='MOTHER_JOURNEY'
                  AND origin_action='RETURN_TO_MOTHER_JOURNEY'
                """)).isOne();
        assertThat(number("SELECT count(*) FROM triage_sessions WHERE triage_session_id='"
                + TRIAGE + "' AND emergency AND content_hash IS NOT NULL "
                + "AND schema_version IS NOT NULL "
                + "AND journey_id='" + JOURNEY + "' "
                + "AND origin_dashboard='MOTHER_JOURNEY' "
                + "AND origin_reference_id='" + JOURNEY + "' "
                + "AND continuation_token IS NOT NULL "
                + "AND continuation_expires_at <= completed_at "
                + "AND continuation_acknowledged_at IS NOT NULL"))
                .isOne();
        assertThat(number("""
                SELECT count(*)
                  FROM safety_event_actions
                 WHERE action_type='TRIAGE_ESCALATION'
                   AND action_phase='LINKED'
                   AND safety_event_id='71000000-0000-0000-0000-000000000041'
                   AND triage_handoff_id='71000000-0000-0000-0000-000000000031'
                   AND owner_user_id='71000000-0000-0000-0000-000000000001'
                """)).isOne();
        assertThat(number("SELECT count(*) FROM triage_session_evidence WHERE triage_session_id='"
                + TRIAGE + "' AND evidence_type='CITATION' AND knowledge_source_id='"
                + SOURCE + "' AND content_hash IS NOT NULL"))
                .isOne();
        assertThat(number("SELECT count(*) FROM triage_session_evidence WHERE triage_session_id='"
                + TRIAGE + "' AND evidence_type='CLAIM' AND claim_code='CLAIM-1'"))
                .isOne();
    }

    @Test
    void canonicalMigrationRejectsAnUnconsumedLifecycleBridgeRow() throws Exception {
        migrate(CUT_OVER, false);
        execute("""
                INSERT INTO carebridge_migration_bridge.triage_lifecycle_bridge(
                    triage_session_id,owner_user_id,captured_at)
                VALUES ('71000000-0000-0000-0000-000000000099',
                    '71000000-0000-0000-0000-000000000098',now())
                """);

        assertThatThrownBy(() -> migrate(CANONICAL, false))
                .hasStackTraceContaining("TRIAGE_LIFECYCLE_BRIDGE_CONSUMPTION_FAILED");
    }

    @Test
    void scalarChecksOwnerBoundFksAndCompletedSnapshotGuardRejectTampering() throws Exception {
        migrate(CANONICAL, false);
        seedCanonicalAggregate();
        execute("""
                INSERT INTO triage_sessions (
                    triage_session_id,user_id,stage,symptoms,risk_level,status,emergency,
                    result_jsonb,schema_version,created_at,completed_at,created_by,
                    journey_id,origin_dashboard,origin_reference_id,
                    continuation_token,continuation_expires_at)
                VALUES (
                    '71000000-0000-0000-0000-000000000032',
                    '71000000-0000-0000-0000-000000000001','PREGNANCY','snapshot',
                    'YELLOW','COMPLETED',false,'{\"riskLevel\":\"YELLOW\"}'::jsonb,
                    'TRIAGE_V1',now(),now(),
                    '71000000-0000-0000-0000-000000000001',
                    '71000000-0000-0000-0000-000000000021','MOTHER_JOURNEY',
                    '71000000-0000-0000-0000-000000000021',
                    '71000000-0000-0000-0000-000000000071',now()+interval '1 hour');
                """);
        assertThat(number("SELECT count(*) FROM triage_sessions WHERE triage_session_id="
                + "'71000000-0000-0000-0000-000000000032' AND content_hash IS NOT NULL"))
                .isOne();

        assertThatThrownBy(() -> execute("""
                UPDATE triage_sessions SET result_jsonb='{"riskLevel":"GREEN"}'::jsonb
                WHERE triage_session_id='71000000-0000-0000-0000-000000000032'
                """)).hasMessageContaining("completed triage snapshot is immutable");
        execute("""
                UPDATE triage_sessions SET continuation_acknowledged_at=now()
                WHERE triage_session_id='71000000-0000-0000-0000-000000000032'
                """);
        execute("""
                UPDATE triage_sessions
                   SET symptom_list='[]'::jsonb, emergency_flag=false,
                       extracted_at=now(), structured_created_by='SYSTEM'
                 WHERE triage_session_id='71000000-0000-0000-0000-000000000032'
                """);
        assertThat(number("SELECT count(*) FROM triage_sessions WHERE triage_session_id="
                + "'71000000-0000-0000-0000-000000000032' "
                + "AND continuation_acknowledged_at IS NOT NULL "
                + "AND symptom_list='[]'::jsonb AND structured_created_by='SYSTEM'"))
                .isOne();

        assertThatThrownBy(() -> insertSafetyOutcome("BLUE", null, OWNER, JOURNEY, TRIAGE))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> insertSafetyOutcome("GREEN", EMERGENCY, OWNER, JOURNEY, TRIAGE))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> insertSafetyOutcome("RED", EMERGENCY, OTHER_OWNER, JOURNEY, TRIAGE))
                .isInstanceOf(Exception.class);
    }

    @Test
    void safetyOutcomeInsertRequiresExactCompletedTriageAndEmergencySources() throws Exception {
        migrate(CANONICAL, false);
        seedCanonicalAggregate();
        seedBoundSafetyOutcomeSources();

        assertTriageSourceRejected(() -> insertSafetyOutcome(
                "YELLOW", "PREGNANCY", "MOTHER_JOURNEY", JOURNEY,
                "RETURN_TO_MOTHER_JOURNEY", null, OWNER, JOURNEY, BOUND_TRIAGE));
        assertTriageSourceRejected(() -> insertSafetyOutcome(
                "RED", "POSTPARTUM", "MOTHER_JOURNEY", JOURNEY,
                "RETURN_TO_MOTHER_JOURNEY", null, OWNER, JOURNEY, BOUND_TRIAGE));
        assertTriageSourceRejected(() -> insertSafetyOutcome(
                "RED", "PREGNANCY", "MOTHER_JOURNEY", JOURNEY,
                "RETURN_TO_MOTHER_JOURNEY", null, OWNER,
                "71000000-0000-0000-0000-000000000022", BOUND_TRIAGE));
        assertTriageSourceRejected(() -> insertSafetyOutcome(
                "RED", "PREGNANCY", "BABY_PROFILE", JOURNEY,
                "RETURN_TO_BABY_PROFILE", null, OWNER, JOURNEY, BOUND_TRIAGE));
        assertTriageSourceRejected(() -> insertSafetyOutcome(
                "RED", "PREGNANCY", "MOTHER_JOURNEY",
                "71000000-0000-0000-0000-000000000022",
                "RETURN_TO_MOTHER_JOURNEY", null, OWNER, JOURNEY, BOUND_TRIAGE));
        assertTriageSourceRejected(() -> insertSafetyOutcome(
                "RED", "PREGNANCY", "MOTHER_JOURNEY", JOURNEY,
                "RETURN_TO_BABY_PROFILE", null, OWNER, JOURNEY, BOUND_TRIAGE));
        assertTriageSourceRejected(() -> insertSafetyOutcome(
                "RED", "PREGNANCY", "MOTHER_JOURNEY", JOURNEY,
                "RETURN_TO_MOTHER_JOURNEY", null, OWNER, JOURNEY, PENDING_TRIAGE));

        assertEmergencySourceRejected(() -> insertSafetyOutcome(
                "RED", "PREGNANCY", "MOTHER_JOURNEY", JOURNEY,
                "RETURN_TO_MOTHER_JOURNEY", IMU_EVENT,
                OWNER, JOURNEY, BOUND_TRIAGE));
        assertEmergencySourceRejected(() -> insertSafetyOutcome(
                "RED", "PREGNANCY", "MOTHER_JOURNEY", JOURNEY,
                "RETURN_TO_MOTHER_JOURNEY", UNLINKED_EMERGENCY,
                OWNER, JOURNEY, BOUND_TRIAGE));
        assertEmergencySourceRejected(() -> insertSafetyOutcome(
                "RED", "PREGNANCY", "MOTHER_JOURNEY", JOURNEY,
                "RETURN_TO_MOTHER_JOURNEY", WRONG_LINK_EMERGENCY,
                OWNER, JOURNEY, BOUND_TRIAGE));

        insertSafetyOutcome(
                "RED", "PREGNANCY", "MOTHER_JOURNEY", JOURNEY,
                "RETURN_TO_MOTHER_JOURNEY", EMERGENCY,
                OWNER, JOURNEY, BOUND_TRIAGE);
        assertThat(number("SELECT count(*) FROM mother_journey_events "
                + "WHERE event_type='SAFETY_OUTCOME' AND triage_session_id='"
                + BOUND_TRIAGE + "' AND emergency_session_id='" + EMERGENCY + "'"))
                .isOne();
    }

    private void seedCanonicalAggregate() throws Exception {
        execute("""
                INSERT INTO persons(person_id,display_name,created_at,updated_at) VALUES
                  ('71000000-0000-0000-0000-000000000001','Canonical Mother',now(),now()),
                  ('71000000-0000-0000-0000-000000000002','Other Mother',now(),now())
                ON CONFLICT (person_id) DO NOTHING;
                INSERT INTO users(user_id,person_id,email,role,account_status,enabled,locked,
                                  email_verified,phone_verified,created_at,updated_at) VALUES
                  ('71000000-0000-0000-0000-000000000001',
                   '71000000-0000-0000-0000-000000000001','canonical.mother@test','MOTHER',
                   'ACTIVE',true,false,true,false,now(),now()),
                  ('71000000-0000-0000-0000-000000000002',
                   '71000000-0000-0000-0000-000000000002','other.mother@test','MOTHER',
                   'ACTIVE',true,false,true,false,now(),now())
                ON CONFLICT (user_id) DO NOTHING;
                INSERT INTO care_subjects (
                    care_subject_id,person_id,owner_user_id,subject_type,nickname,status,
                    created_at,updated_at)
                VALUES ('71000000-0000-0000-0000-000000000011',
                    '71000000-0000-0000-0000-000000000001',
                    '71000000-0000-0000-0000-000000000001','MOTHER','Canonical Mother',
                    'ACTIVE',now(),now())
                ON CONFLICT (care_subject_id) DO NOTHING;
                INSERT INTO mother_journeys (
                    journey_id,care_subject_id,owner_user_id,journey_type,status,
                    created_at,updated_at)
                VALUES ('71000000-0000-0000-0000-000000000021',
                    '71000000-0000-0000-0000-000000000011',
                    '71000000-0000-0000-0000-000000000001','PREGNANCY','ACTIVE',now(),now())
                ON CONFLICT (journey_id) DO NOTHING;
                UPDATE care_subjects SET mother_journey_id=
                    '71000000-0000-0000-0000-000000000021'
                WHERE care_subject_id='71000000-0000-0000-0000-000000000011';
                INSERT INTO triage_sessions (
                    triage_session_id,user_id,stage,symptoms,raw_ai_response,risk_level,
                    status,emergency,created_at,completed_at,created_by)
                VALUES ('71000000-0000-0000-0000-000000000031',
                    '71000000-0000-0000-0000-000000000001','PREGNANCY','legacy red',
                    '{"status":"COMPLETED","riskLevel":"RED","responseSchemaVersion":"2.0",'
                    '"citations":[{"sourceId":"71000000-0000-0000-0000-000000000051",'
                    '"title":"Lifecycle guidance","organization":"Lifecycle Test","url":"https://triage-lifecycle.example.org/guide",'
                    '"excerpt":"Seek emergency care","domain":"triage-lifecycle.example.org","sourceVersion":"2026",'
                    '"lastReviewed":"2026-07-01","section":"Emergency","matchedSymptoms":[],'
                    '"matchedRules":["RED"],"sourceStatus":"APPROVED",'
                    '"retrievedAt":"2026-07-24T00:00:00Z","retrievalMode":"LOCAL"}],'
                    '"claims":[{"claimId":"CLAIM-1","text":"Seek emergency care",'
                    '"evidenceIds":["71000000-0000-0000-0000-000000000051"]}]}'
                    ,'RED','COMPLETED',false,now(),now(),
                    '71000000-0000-0000-0000-000000000001')
                ON CONFLICT (triage_session_id) DO NOTHING;
                INSERT INTO safety_events (
                    safety_event_id,user_id,detected_at,event_type,status,record_type,
                    created_at,updated_at)
                VALUES ('71000000-0000-0000-0000-000000000041',
                    '71000000-0000-0000-0000-000000000001',now(),'EMERGENCY_OPENED',
                    'ACTIVE','EMERGENCY_SESSION',now(),now())
                ON CONFLICT (safety_event_id) DO NOTHING;
                INSERT INTO knowledge_sources (
                    knowledge_source_id,domain,base_url,organization,category,status,
                    discovery_mode,created_at,updated_at)
                VALUES ('71000000-0000-0000-0000-000000000051','triage-lifecycle.example.org',
                    'https://triage-lifecycle.example.org','Lifecycle Test','GOVERNMENT','APPROVED','MANUAL_ADMIN_ADD',
                    now(),now())
                ON CONFLICT (knowledge_source_id) DO NOTHING;
                """);
    }

    private void seedBoundSafetyOutcomeSources() throws Exception {
        execute("""
                INSERT INTO triage_sessions (
                    triage_session_id,user_id,stage,symptoms,risk_level,status,emergency,
                    result_jsonb,schema_version,created_at,completed_at,created_by,
                    journey_id,origin_dashboard,origin_reference_id,
                    continuation_token,continuation_expires_at)
                VALUES
                  ('71000000-0000-0000-0000-000000000032',
                   '71000000-0000-0000-0000-000000000001','PREGNANCY','bound red',
                   'RED','COMPLETED',true,'{"riskLevel":"RED"}'::jsonb,'TRIAGE_V1',
                   now(),now(),'71000000-0000-0000-0000-000000000001',
                   '71000000-0000-0000-0000-000000000021','MOTHER_JOURNEY',
                   '71000000-0000-0000-0000-000000000021',
                   '71000000-0000-0000-0000-000000000072',now()+interval '1 hour'),
                  ('71000000-0000-0000-0000-000000000033',
                   '71000000-0000-0000-0000-000000000001','PREGNANCY','other red',
                   'RED','COMPLETED',true,'{"riskLevel":"RED"}'::jsonb,'TRIAGE_V1',
                   now(),now(),'71000000-0000-0000-0000-000000000001',
                   '71000000-0000-0000-0000-000000000021','MOTHER_JOURNEY',
                   '71000000-0000-0000-0000-000000000021',
                   '71000000-0000-0000-0000-000000000073',now()+interval '1 hour'),
                  ('71000000-0000-0000-0000-000000000034',
                   '71000000-0000-0000-0000-000000000001','PREGNANCY','pending red',
                   'RED','IN_PROGRESS',true,'{}'::jsonb,'TRIAGE_V1',
                   now(),NULL,'71000000-0000-0000-0000-000000000001',
                   '71000000-0000-0000-0000-000000000021','MOTHER_JOURNEY',
                   '71000000-0000-0000-0000-000000000021',
                   '71000000-0000-0000-0000-000000000074',now()+interval '1 hour');

                INSERT INTO safety_events (
                    safety_event_id,user_id,detected_at,event_type,status,record_type,
                    created_at,updated_at)
                VALUES
                  ('71000000-0000-0000-0000-000000000042',
                   '71000000-0000-0000-0000-000000000001',now(),'FALL_DETECTED',
                   'DETECTED','IMU_EVENT',now(),now()),
                  ('71000000-0000-0000-0000-000000000043',
                   '71000000-0000-0000-0000-000000000001',now(),'EMERGENCY_OPENED',
                   'RESOLVED','EMERGENCY_SESSION',now(),now()),
                  ('71000000-0000-0000-0000-000000000044',
                   '71000000-0000-0000-0000-000000000001',now(),'EMERGENCY_OPENED',
                   'RESOLVED','EMERGENCY_SESSION',now(),now());

                INSERT INTO safety_event_actions (
                    safety_event_action_id,safety_event_id,action_type,owner_user_id,
                    triage_handoff_id,attempt_number,idempotency_key,action_phase,
                    alert_generation,created_at)
                VALUES
                  (gen_random_uuid(),'71000000-0000-0000-0000-000000000041',
                   'TRIAGE_ESCALATION','71000000-0000-0000-0000-000000000001',
                   '71000000-0000-0000-0000-000000000032',1,
                   'triage-escalation:71000000-0000-0000-0000-000000000032',
                   'LINKED',0,now()),
                  (gen_random_uuid(),'71000000-0000-0000-0000-000000000044',
                   'TRIAGE_ESCALATION','71000000-0000-0000-0000-000000000001',
                   '71000000-0000-0000-0000-000000000033',1,
                   'triage-escalation:71000000-0000-0000-0000-000000000033',
                   'LINKED',0,now());
                """);
    }

    private void seedLegacyLifecycleAggregate() throws Exception {
        execute("""
                INSERT INTO users(user_id,email,role,account_status,enabled,locked,
                                  email_verified,phone_verified,created_at,updated_at)
                VALUES ('71000000-0000-0000-0000-000000000001',
                        'legacy.lifecycle@test','MOTHER','ACTIVE',true,false,true,false,now(),now());
                INSERT INTO mother_journeys(
                    journey_id,owner_user_id,journey_type,status,created_at,updated_at)
                VALUES ('71000000-0000-0000-0000-000000000021',
                        '71000000-0000-0000-0000-000000000001',
                        'PREGNANCY','ACTIVE',now(),now());
                INSERT INTO intake_sessions(
                    id,user_id,stage,symptoms,risk_level,status,disclaimer,
                    created_at,completed_at,created_by)
                VALUES ('71000000-0000-0000-0000-000000000031',
                        '71000000-0000-0000-0000-000000000001','PREGNANCY',
                        'legacy lifecycle','YELLOW','COMPLETED','fixture',now(),now(),
                        '71000000-0000-0000-0000-000000000001');
                """);
    }

    private void insertSafetyOutcome(
            String risk, String emergency, String owner, String journey, String triage)
            throws Exception {
        insertSafetyOutcome(
                risk, "PREGNANCY", "MOTHER_JOURNEY", journey,
                "RETURN_TO_MOTHER_JOURNEY", emergency, owner, journey, triage);
    }

    private void insertSafetyOutcome(
            String risk,
            String stage,
            String originDashboard,
            String originReference,
            String originAction,
            String emergency,
            String owner,
            String journey,
            String triage)
            throws Exception {
        String emergencyValue = emergency == null ? "NULL" : "'" + emergency + "'";
        execute("""
                INSERT INTO mother_journey_events (
                    event_id,mother_journey_id,owner_user_id,event_type,event_payload_jsonb,
                    schema_version,actor_user_id,effective_at,recorded_at,legacy_source,legacy_id,
                    triage_session_id,emergency_session_id,risk_level,stage,
                    origin_dashboard,origin_reference_id,origin_action)
                VALUES (gen_random_uuid(),'%s','%s','SAFETY_OUTCOME','{}','1','%s',now(),now(),
                    'TEST',gen_random_uuid()::text,'%s',%s,'%s','%s','%s','%s','%s')
                """.formatted(
                journey, owner, owner, triage, emergencyValue, risk, stage,
                originDashboard, originReference, originAction));
    }

    private void assertTriageSourceRejected(ThrowingCallable insert) {
        assertThatThrownBy(insert)
                .hasMessageContaining("SAFETY_OUTCOME_TRIAGE_SOURCE_MISMATCH");
    }

    private void assertEmergencySourceRejected(ThrowingCallable insert) {
        assertThatThrownBy(insert)
                .hasMessageContaining("SAFETY_OUTCOME_EMERGENCY_SOURCE_MISMATCH");
    }

    private void migrate(MigrationVersion target, boolean outOfOrder) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .outOfOrder(outOfOrder)
                .load()
                .migrate();
    }

    private int columns(String table, String... names) throws Exception {
        String quoted = java.util.Arrays.stream(names)
                .map(name -> "'" + name + "'")
                .collect(java.util.stream.Collectors.joining(","));
        return (int) number("SELECT count(*) FROM information_schema.columns WHERE table_schema="
                + "'public' AND table_name='" + table + "' AND column_name IN (" + quoted + ")");
    }

    private boolean exists(String table) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT to_regclass('public." + table + "') IS NOT NULL")) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private long number(String sql) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
