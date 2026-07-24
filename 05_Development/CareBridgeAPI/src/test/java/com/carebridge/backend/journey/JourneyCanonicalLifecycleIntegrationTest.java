package com.carebridge.backend.journey;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.consent.service.ConsentService;
import com.carebridge.backend.journey.entity.*;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.MotherJourneyTransitionRepository;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;

@TestPropertySource(properties = {
        "carebridge.zego.app-id=1",
        "carebridge.zego.server-secret=synthetic-test-secret"
})
@Execution(ExecutionMode.SAME_THREAD)
class JourneyCanonicalLifecycleIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final java.util.UUID ELIGIBILITY_SUBMISSION_ID =
            java.util.UUID.fromString("00000000-0000-0000-0000-000000610200");

    @Autowired IJourneyTransitionService transitionService;
    @Autowired MotherJourneyRepository journeyRepository;
    @Autowired MotherJourneyTransitionRepository transitionRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired ConsentService consentService;

    @BeforeEach
    void seedMother() {
        jdbcTemplate.execute(
                "ALTER TABLE public.mother_journey_events DISABLE TRIGGER mother_journey_events_immutable_trg");
        try {
            jdbcTemplate.update(
                    "DELETE FROM public.mother_journey_events WHERE owner_user_id = ?",
                    JourneyLifecycleTestFactory.MOTHER_ID);
        } finally {
            jdbcTemplate.execute(
                    "ALTER TABLE public.mother_journey_events ENABLE TRIGGER mother_journey_events_immutable_trg");
        }
        jdbcTemplate.update(
                "UPDATE public.care_subjects SET mother_journey_id = NULL "
                        + "WHERE owner_user_id = ? AND subject_type = 'MOTHER'",
                JourneyLifecycleTestFactory.MOTHER_ID);
        jdbcTemplate.update(
                "DELETE FROM public.mother_journeys WHERE owner_user_id = ?",
                JourneyLifecycleTestFactory.MOTHER_ID);
        jdbcTemplate.update(
                "DELETE FROM public.care_subjects WHERE owner_user_id = ? AND subject_type = 'MOTHER'",
                JourneyLifecycleTestFactory.MOTHER_ID);
        jdbcTemplate.update(
                "DELETE FROM public.data_permissions WHERE owner_user_id = ? "
                        + "AND permission_kind = 'CONSENT_GRANT' AND scope_type = 'MOTHER_BASELINE'",
                JourneyLifecycleTestFactory.MOTHER_ID);
        jdbcTemplate.update("""
                INSERT INTO public.persons (person_id, display_name, created_at, updated_at)
                VALUES (?, 'Story 61 Mother', now(), now())
                ON CONFLICT (person_id) DO NOTHING
                """, JourneyLifecycleTestFactory.MOTHER_ID);
        jdbcTemplate.update("""
                INSERT INTO public.users (
                    user_id, person_id, email, role, account_status, enabled, locked,
                    email_verified, phone_verified, created_at, updated_at
                ) VALUES (?, ?, ?, 'MOTHER', 'ACTIVE', true, false, true, false, now(), now())
                ON CONFLICT (user_id) DO NOTHING
                """,
                JourneyLifecycleTestFactory.MOTHER_ID,
                JourneyLifecycleTestFactory.MOTHER_ID,
                "story61.mother@test.carebridge.local");
        jdbcTemplate.update("""
                INSERT INTO public.mother_journey_events (
                    event_id, owner_user_id, submission_id, journey_version,
                    schema_version, lifecycle_goal, locale, time_zone,
                    preferences, recorded_at, event_source, event_type,
                    event_payload_jsonb, effective_at, legacy_source, legacy_id
                ) VALUES (?, ?, ?, 1, 'MOTHER_BASELINE_V1',
                    'CURRENTLY_PREGNANT', 'vi-VN', 'Asia/Ho_Chi_Minh',
                    'NUTRITION', now(), 'SELF_REPORTED', 'BASELINE_CONTEXT',
                    '{}'::jsonb, now(), 'MOTHER_BASELINE', ?)
                """,
                java.util.UUID.randomUUID(),
                JourneyLifecycleTestFactory.MOTHER_ID,
                ELIGIBILITY_SUBMISSION_ID,
                ELIGIBILITY_SUBMISSION_ID.toString());
        jdbcTemplate.update("""
                INSERT INTO public.data_permissions (
                    permission_kind, owner_user_id, scope_type, purpose, scope_text,
                    policy_version, evidence_key, locale, granted_at, expires_at,
                    version_number, status, created_at, updated_at
                ) VALUES ('CONSENT_GRANT', ?, 'MOTHER_BASELINE', 'PERSONALIZE',
                    'STORE_BASELINE_AND_PERSONALIZE_MOTHER_LIFECYCLE',
                    'MOTHER_LIFECYCLE_V1', ?, 'vi-VN', now(),
                    now() + interval '30 days', 1, 'ACTIVE', now(), now())
                """,
                JourneyLifecycleTestFactory.MOTHER_ID,
                ELIGIBILITY_SUBMISSION_ID);
    }

    @Test
    void jrnTcInt001_postgresCurrentAndHistoryHappyPath() {
        var response = transitionService.createJourney(
                JourneyLifecycleTestFactory.pregnancyCreate(),
                JourneyLifecycleTestFactory.MOTHER_ID);

        assertThat(journeyRepository.findById(response.getId())).isPresent();
        assertThat(transitionRepository.countByJourneyId(response.getId())).isEqualTo(1L);
        assertThat(response.getVersion()).isZero();
        assertThat(auditLogRepository.findByEntityIdAndAction(
                response.getId(), AuditAction.JOURNEY_CREATED)).hasSize(1);
    }

    @Test
    void concurrentRevocationCommitsBeforeCreateEligibilityAndPreventsInitialization()
            throws Exception {
        Long consentId = jdbcTemplate.queryForObject("""
                SELECT legacy_consent_id FROM public.data_permissions
                WHERE permission_kind = 'CONSENT_GRANT'
                  AND owner_user_id = ? AND evidence_key = ?
                """, Long.class, JourneyLifecycleTestFactory.MOTHER_ID,
                ELIGIBILITY_SUBMISSION_ID);
        CountDownLatch revokedButUncommitted = new CountDownLatch(1);
        CountDownLatch allowRevocationCommit = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> revocation = executor.submit(() -> {
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                return transaction.execute(status -> {
                    consentService.revokeConsent(
                            JourneyLifecycleTestFactory.MOTHER_ID, consentId);
                    revokedButUncommitted.countDown();
                    try {
                        if (!allowRevocationCommit.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("revocation commit barrier timed out");
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                    return null;
                });
            });

            assertThat(revokedButUncommitted.await(5, TimeUnit.SECONDS)).isTrue();
            Future<Object> creation = executor.submit(() -> createAfterBarrier(
                    JourneyType.PREGNANCY, new CountDownLatch(0), new CountDownLatch(0)));

            assertThatThrownBy(() -> creation.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            allowRevocationCommit.countDown();
            revocation.get(5, TimeUnit.SECONDS);

            Object result = creation.get(5, TimeUnit.SECONDS);
            assertThat(result).isInstanceOf(BusinessException.class);
            assertThat(((BusinessException) result).getCode())
                    .isEqualTo("LIFECYCLE_CONSENT_INVALID");
        }

        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM public.mother_journeys WHERE owner_user_id = ?
                """, Long.class, JourneyLifecycleTestFactory.MOTHER_ID)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM public.mother_journey_events t
                JOIN public.mother_journeys j ON j.journey_id = t.mother_journey_id
                WHERE j.owner_user_id = ?
                  AND t.legacy_source = 'JOURNEY_TRANSITION'
                """, Long.class, JourneyLifecycleTestFactory.MOTHER_ID)).isZero();
    }

    @Test
    void jrnTcInt002_duplicateCanonicalCreatesProduceOneWinner() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Object> pregnancy = executor.submit(
                    () -> createAfterBarrier(JourneyType.PREGNANCY, ready, start));
            Future<Object> prePregnancy = executor.submit(
                    () -> createAfterBarrier(JourneyType.PRE_PREGNANCY, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> outcomes = List.of(pregnancy.get(), prePregnancy.get());

            assertThat(outcomes).filteredOn(CreateOutcome.class::isInstance).hasSize(1);
            assertThat(outcomes)
                    .filteredOn(BusinessException.class::isInstance)
                    .singleElement()
                    .satisfies(error -> assertThat(((BusinessException) error).getCode())
                            .isEqualTo("JOURNEY-015"));
        }

        assertThat(journeyRepository.countByOwnerUserIdAndStatus(
                JourneyLifecycleTestFactory.MOTHER_ID, JourneyStatus.ACTIVE)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM public.mother_journey_events e
                JOIN public.mother_journeys j ON j.journey_id = e.mother_journey_id
                WHERE j.owner_user_id = ? AND e.legacy_source = 'JOURNEY_TRANSITION'
                """, Long.class, JourneyLifecycleTestFactory.MOTHER_ID)).isEqualTo(1L);
    }

    @Test
    void jrnTcInt003_currentHistoryMutationIsAtomic() {
        var created = transitionService.createJourney(
                JourneyLifecycleTestFactory.pregnancyCreate(),
                JourneyLifecycleTestFactory.MOTHER_ID);

        var failing = JourneyLifecycleTestFactory.dateCorrection();
        failing.setChangeReason("x".repeat(501));
        assertThatThrownBy(() -> transitionService.updateJourney(
                JourneyLifecycleTestFactory.MOTHER_ID, created.getId(), failing))
                .isInstanceOf(RuntimeException.class);

        MotherJourney unchanged = journeyRepository.findById(created.getId()).orElseThrow();
        assertThat(unchanged.getLastMenstrualDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(transitionRepository.countByJourneyId(created.getId())).isEqualTo(1L);

        transitionService.updateJourney(
                JourneyLifecycleTestFactory.MOTHER_ID,
                created.getId(),
                JourneyLifecycleTestFactory.dateCorrection());

        MotherJourney current = journeyRepository.findById(created.getId()).orElseThrow();
        assertThat(current.getLastMenstrualDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(transitionRepository.countByJourneyId(created.getId())).isEqualTo(2L);
        assertThat(auditLogRepository.findByEntityIdAndAction(
                created.getId(), AuditAction.JOURNEY_UPDATED)).hasSize(1);
    }

    @Test
    void jrnTcInt004_migrationCreatesSchemaAndHistoryIsReadable() throws Exception {
        String database = "story61_legacy_backfill";
        String url = createLegacyDatabase(database, false);

        runStoryMigration(url);

        try (Connection connection = open(url);
             Statement statement = connection.createStatement()) {
            assertThat(singleLong(statement, """
                    SELECT count(*) FROM information_schema.columns
                    WHERE table_schema='public'
                      AND table_name='mother_journeys'
                      AND column_name IN ('version', 'date_source', 'date_confidence')
                    """)).isEqualTo(3L);
            assertThat(singleLong(statement, """
                    SELECT count(*) FROM mother_journey_transitions
                    WHERE event_type='MIGRATED'
                      AND source='MIGRATION'
                      AND journey_version=0
                    """)).isEqualTo(1L);
            assertThat(singleLong(statement, "SELECT count(*) FROM mother_journeys"))
                    .isEqualTo(1L);
        }
    }

    @Test
    void jrnTcInt005_uniqueIndexRejectsDuplicateCanonicalActiveOwner() throws Exception {
        String database = "story61_duplicate_preflight";
        String url = createLegacyDatabase(database, true);

        assertThatThrownBy(() -> runStoryMigration(url))
                .hasRootCauseMessage(
                        "ERROR: Canonical journey migration blocked: "
                                + "duplicate ACTIVE lifecycle rows\n"
                                + "  Where: PL/pgSQL function inline_code_block line 11 at RAISE");

        try (Connection connection = open(url);
             Statement statement = connection.createStatement()) {
            assertThat(singleLong(statement, "SELECT count(*) FROM mother_journeys"))
                    .isEqualTo(2L);
            assertThat(singleLong(statement, """
                    SELECT count(*) FROM pg_indexes
                    WHERE schemaname='public'
                      AND indexname='uq_mother_journeys_one_canonical_active'
                    """)).isZero();
        }
    }

    @Test
    void jrnTcInt006_optimisticVersionAdvancesAndHistoryMatchesWinner() throws Exception {
        var created = transitionService.createJourney(
                JourneyLifecycleTestFactory.pregnancyCreate(),
                JourneyLifecycleTestFactory.MOTHER_ID);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Object> first = executor.submit(
                    () -> updateAfterPreload(created.getId(), LocalDate.of(2026, 6, 2), ready, start));
            Future<Object> second = executor.submit(
                    () -> updateAfterPreload(created.getId(), LocalDate.of(2026, 6, 3), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> outcomes = List.of(first.get(), second.get());

            assertThat(outcomes).filteredOn(UpdateOutcome.class::isInstance).hasSize(1);
            assertThat(outcomes)
                    .filteredOn(BusinessException.class::isInstance)
                    .singleElement()
                    .satisfies(error -> assertThat(((BusinessException) error).getCode())
                            .isEqualTo("JOURNEY-017"));
        }

        assertThat(journeyRepository.findById(created.getId()).orElseThrow().getVersion())
                .isEqualTo(1L);
        assertThat(transitionService.getHistory(
                JourneyLifecycleTestFactory.MOTHER_ID,
                created.getId(),
                PageRequest.of(0, 20)).getItems())
                .extracting("journeyVersion")
                .containsExactlyInAnyOrder(0L, 1L);
    }

    @Test
    void transitionHistoryRejectsDirectUpdateAndDelete() {
        var created = transitionService.createJourney(
                JourneyLifecycleTestFactory.pregnancyCreate(),
                JourneyLifecycleTestFactory.MOTHER_ID);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                UPDATE public.mother_journey_events
                SET reason = 'tampered'
                WHERE mother_journey_id = ? AND legacy_source = 'JOURNEY_TRANSITION'
                """, created.getId()))
                .hasRootCauseMessage("ERROR: IMMUTABLE_TABLE: public.mother_journey_events "
                        + "does not allow UPDATE or DELETE\n"
                        + "  Where: PL/pgSQL function carebridge_reject_mutation() line 3 at RAISE");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                DELETE FROM public.mother_journey_events
                WHERE mother_journey_id = ? AND legacy_source = 'JOURNEY_TRANSITION'
                """, created.getId()))
                .hasRootCauseMessage("ERROR: IMMUTABLE_TABLE: public.mother_journey_events "
                        + "does not allow UPDATE or DELETE\n"
                        + "  Where: PL/pgSQL function carebridge_reject_mutation() line 3 at RAISE");
        assertThat(transitionRepository.countByJourneyId(created.getId())).isEqualTo(1L);
    }

    private Object createAfterBarrier(
            JourneyType type, CountDownLatch ready, CountDownLatch start) {
        try {
            var request = JourneyLifecycleTestFactory.pregnancyCreate();
            request.setJourneyType(type);
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("create race barrier timed out");
            }
            return new CreateOutcome(transitionService.createJourney(
                    request, JourneyLifecycleTestFactory.MOTHER_ID).getId());
        } catch (BusinessException exception) {
            return exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private Object updateAfterPreload(
            java.util.UUID journeyId,
            LocalDate lastMenstrualDate,
            CountDownLatch ready,
            CountDownLatch start) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        try {
            return transaction.execute(status -> {
                journeyRepository.findById(journeyId).orElseThrow();
                ready.countDown();
                try {
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("update race barrier timed out");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                var request = JourneyLifecycleTestFactory.dateCorrection();
                request.setLastMenstrualDate(lastMenstrualDate);
                return new UpdateOutcome(transitionService.updateJourney(
                        JourneyLifecycleTestFactory.MOTHER_ID, journeyId, request).getVersion());
            });
        } catch (BusinessException exception) {
            return exception;
        }
    }

    private record CreateOutcome(java.util.UUID journeyId) {
    }

    private record UpdateOutcome(long version) {
    }

    private String createLegacyDatabase(String database, boolean duplicate) throws Exception {
        try (Connection admin = open(POSTGRES.getJdbcUrl());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = POSTGRES.getJdbcUrl().replace("/test?", "/" + database + "?");
        try (Connection connection = open(url);
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE users (
                        user_id uuid PRIMARY KEY
                    )
                    """);
            statement.execute("""
                    CREATE TABLE mother_journeys (
                        journey_id uuid PRIMARY KEY,
                        owner_user_id uuid NOT NULL REFERENCES users(user_id),
                        journey_type varchar(20) NOT NULL,
                        status varchar(20) NOT NULL,
                        created_at timestamptz NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO users(user_id)
                    VALUES ('00000000-0000-0000-0000-000000000101')
                    """);
            statement.execute("""
                    INSERT INTO mother_journeys(
                        journey_id, owner_user_id, journey_type, status, created_at
                    ) VALUES (
                        '00000000-0000-0000-0000-000000001001',
                        '00000000-0000-0000-0000-000000000101',
                        'PREGNANCY',
                        'ACTIVE',
                        '2026-07-18T03:00:00Z'
                    )
                    """);
            if (duplicate) {
                statement.execute("""
                        INSERT INTO mother_journeys(
                            journey_id, owner_user_id, journey_type, status, created_at
                        ) VALUES (
                            '00000000-0000-0000-0000-000000001002',
                            '00000000-0000-0000-0000-000000000101',
                            'PRE_PREGNANCY',
                            'ACTIVE',
                            '2026-07-18T03:01:00Z'
                        )
                        """);
            }
        }
        return url;
    }

    private void runStoryMigration(String url) {
        Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("20260718089999"))
                .target(MigrationVersion.fromVersion("20260718090000"))
                .outOfOrder(true)
                .load()
                .migrate();
    }

    private Connection open(String url) throws Exception {
        return DriverManager.getConnection(
                url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private long singleLong(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }
}
