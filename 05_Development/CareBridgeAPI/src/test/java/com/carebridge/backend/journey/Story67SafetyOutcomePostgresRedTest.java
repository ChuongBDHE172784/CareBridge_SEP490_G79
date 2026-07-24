package com.carebridge.backend.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.emergency.service.IFamilyAlertService;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.IJourneyTimelineService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Active RED-phase PostgreSQL acceptance tests for Story 6.7 AC2, AC3, and AC5. */
class Story67SafetyOutcomePostgresRedTest extends AbstractPostgresIntegrationTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-07-22T12:00:00Z");

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private IJourneyTimelineService timelineService;

    @MockitoBean private IFamilyAlertService familyAlertService;
    @MockitoBean private ChildTriageAiClient childTriageAiClient;
    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;

    private UUID ownerId;
    private UUID journeyId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        ownerId = seedMother("story67-owner@carebridge.test");
        journeyId = seedJourney(ownerId, JourneyType.POSTPARTUM);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    @Test
    void migration_shouldCreateLifecycleBindingAndMinimumDataAppendOnlyProjection() {
        assertThat(columnNames("intake_sessions"))
                .contains(
                        "journey_id",
                        "origin_dashboard",
                        "origin_reference_id",
                        "continuation_token",
                        "continuation_expires_at",
                        "continuation_acknowledged_at");

        assertThat(jdbcTemplate.queryForObject(
                "select to_regclass('public.lifecycle_safety_outcomes')::text",
                String.class)).isEqualTo("lifecycle_safety_outcomes");

        assertThat(columnNames("lifecycle_safety_outcomes")).containsExactly(
                "outcome_id",
                "owner_user_id",
                "journey_id",
                "intake_session_id",
                "emergency_session_id",
                "risk_level",
                "stage",
                "origin_dashboard",
                "origin_reference_id",
                "origin_action",
                "occurred_at",
                "recorded_at");

        Map<String, String> foreignKeys = constraintDefinitions("lifecycle_safety_outcomes", "f");
        assertThat(foreignKeys.values()).anySatisfy(definition -> assertThat(definition)
                .contains("FOREIGN KEY (journey_id, owner_user_id)")
                .contains("REFERENCES mother_journeys(journey_id, owner_user_id)"));
        assertThat(foreignKeys.values()).anySatisfy(definition -> assertThat(definition)
                .contains("FOREIGN KEY (intake_session_id, owner_user_id)")
                .contains("REFERENCES intake_sessions(id, user_id)"));
        assertThat(foreignKeys.values()).anySatisfy(definition -> assertThat(definition)
                .contains("FOREIGN KEY (emergency_session_id, owner_user_id)")
                .contains("REFERENCES emergency_sessions(id, user_id)"));

        assertThat(constraintDefinitions("lifecycle_safety_outcomes", "u").values())
                .anySatisfy(definition -> assertThat(definition)
                        .contains("UNIQUE (intake_session_id)"));
        assertThat(indexDefinition("idx_safety_journey_timeline"))
                .contains("journey_id")
                .contains("occurred_at DESC")
                .contains("recorded_at DESC")
                .contains("outcome_id DESC");

        List<String> triggerDefinitions = jdbcTemplate.queryForList("""
                select pg_get_triggerdef(oid)
                from pg_trigger
                where tgrelid = 'public.lifecycle_safety_outcomes'::regclass
                  and not tgisinternal
                """, String.class);
        assertThat(triggerDefinitions)
                .as("UPDATE and DELETE must be rejected by a database boundary")
                .anySatisfy(definition -> assertThat(definition).contains("UPDATE", "DELETE"));
    }

    @Test
    void projection_shouldRejectMutationAndCrossOwnerReferences() {
        UUID intakeId = insertLifecycleIntake(ownerId, journeyId, "RED");
        UUID outcomeId = insertOutcome(intakeId, ownerId, journeyId, null, "RED");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "update lifecycle_safety_outcomes set risk_level = 'GREEN' where outcome_id = ?",
                outcomeId)).rootCause().hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "delete from lifecycle_safety_outcomes where outcome_id = ?",
                outcomeId)).rootCause().hasMessageContaining("append-only");

        UUID otherOwnerId = seedMother("story67-other@carebridge.test");
        UUID otherIntakeId = insertLifecycleIntake(otherOwnerId, seedJourney(otherOwnerId, JourneyType.POSTPARTUM), "GREEN");
        assertThatThrownBy(() -> insertOutcome(
                otherIntakeId, otherOwnerId, journeyId, null, "GREEN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateAndConcurrentProjection_shouldCommitExactlyOneRowPerIntake() throws Exception {
        UUID intakeId = insertLifecycleIntake(ownerId, journeyId, "YELLOW");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Integer> first = executor.submit(() -> insertOutcomeOnConflict(start, intakeId));
        Future<Integer> second = executor.submit(() -> insertOutcomeOnConflict(start, intakeId));

        try {
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS) + second.get(15, TimeUnit.SECONDS)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }

        assertThat(countOutcomes(intakeId)).isEqualTo(1L);
    }

    @Test
    void projectionTransactionRollback_thenRetry_shouldRecoverToOneDurableOutcome() {
        UUID intakeId = insertLifecycleIntake(ownerId, journeyId, "GREEN");
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            insertOutcome(intakeId, ownerId, journeyId, null, "GREEN");
            throw new RollbackProbeException();
        })).isInstanceOf(RollbackProbeException.class);
        assertThat(countOutcomes(intakeId)).isZero();

        insertOutcome(intakeId, ownerId, journeyId, null, "GREEN");
        assertThat(countOutcomes(intakeId)).isEqualTo(1L);
    }

    @Test
    void multipleRedIntakes_shouldProjectOnceEachWhileReusingOneAuthoritativeEmergency() {
        UUID emergencyId = insertEmergency(ownerId);
        UUID firstIntakeId = insertLifecycleIntake(ownerId, journeyId, "RED");
        UUID secondIntakeId = insertLifecycleIntake(ownerId, journeyId, "RED");
        linkEscalation(firstIntakeId, emergencyId, ownerId);
        linkEscalation(secondIntakeId, emergencyId, ownerId);

        insertOutcome(firstIntakeId, ownerId, journeyId, emergencyId, "RED");
        insertOutcome(secondIntakeId, ownerId, journeyId, emergencyId, "RED");

        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from lifecycle_safety_outcomes
                where emergency_session_id = ? and risk_level = 'RED'
                """, Long.class, emergencyId)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("""
                select count(distinct emergency_session_id) from lifecycle_safety_outcomes
                where intake_session_id in (?, ?)
                """, Long.class, firstIntakeId, secondIntakeId)).isEqualTo(1L);
    }

    @Test
    void unifiedTimeline_shouldGloballyOrderAndPageTransitionsWithSafetyOutcomes() {
        UUID intakeId = insertLifecycleIntake(ownerId, journeyId, "GREEN");
        UUID outcomeId = insertOutcome(intakeId, ownerId, journeyId, null, "GREEN");
        UUID transitionId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into mother_journey_transitions (
                    transition_id, journey_id, event_type, from_stage, to_stage,
                    changes_json, source, confidence, reason, actor_user_id,
                    effective_at, recorded_at, journey_version)
                values (?, ?, 'STAGE_CHANGED', 'PREGNANCY', 'POSTPARTUM',
                    '{}'::jsonb, 'SYSTEM_DERIVED', 'CONFIRMED', 'STORY_6_7_TEST',
                    null, ?, ?, 1)
                """, transitionId, journeyId,
                Timestamp.from(OCCURRED_AT.plusSeconds(60)),
                Timestamp.from(OCCURRED_AT.plusSeconds(61)));

        var firstPage = timelineService.getTimeline(ownerId, journeyId, PageRequest.of(0, 1));
        var secondPage = timelineService.getTimeline(ownerId, journeyId, PageRequest.of(1, 1));

        assertThat(firstPage.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getItemType()).isEqualTo("LIFECYCLE_TRANSITION");
            assertThat(item.getItemId()).isEqualTo(transitionId);
        });
        assertThat(secondPage.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getItemType()).isEqualTo("SAFETY_OUTCOME");
            assertThat(item.getItemId()).isEqualTo(outcomeId);
            assertThat(item.getSourceIntakeId()).isEqualTo(intakeId);
        });
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    private int insertOutcomeOnConflict(CountDownLatch start, UUID intakeId) throws Exception {
        start.await();
        return jdbcTemplate.update("""
                insert into lifecycle_safety_outcomes (
                    outcome_id, owner_user_id, journey_id, intake_session_id, risk_level, stage,
                    origin_dashboard, origin_reference_id, origin_action, occurred_at, recorded_at)
                values (?, ?, ?, ?, 'YELLOW', 'POSTPARTUM', 'MOTHER_JOURNEY', ?,
                        'RETURN_TO_MOTHER_JOURNEY', ?, now())
                on conflict (intake_session_id) do nothing
                """,
                UUID.randomUUID(), ownerId, journeyId, intakeId, journeyId, Timestamp.from(OCCURRED_AT));
    }

    private UUID insertOutcome(
            UUID intakeId,
            UUID userId,
            UUID boundJourneyId,
            UUID emergencyId,
            String riskLevel) {
        UUID outcomeId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into lifecycle_safety_outcomes (
                    outcome_id, owner_user_id, journey_id, intake_session_id, emergency_session_id,
                    risk_level, stage, origin_dashboard, origin_reference_id, origin_action,
                    occurred_at, recorded_at)
                values (?, ?, ?, ?, ?, ?, 'POSTPARTUM', 'MOTHER_JOURNEY', ?,
                        'RETURN_TO_MOTHER_JOURNEY', ?, now())
                """,
                outcomeId,
                userId,
                boundJourneyId,
                intakeId,
                emergencyId,
                riskLevel,
                boundJourneyId,
                Timestamp.from(OCCURRED_AT));
        return outcomeId;
    }

    private UUID insertLifecycleIntake(UUID userId, UUID boundJourneyId, String riskLevel) {
        UUID intakeId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into intake_sessions (
                    id, user_id, symptoms, risk_level, status, disclaimer, created_at,
                    completed_at, created_by, stage, client_request_id, journey_id,
                    origin_dashboard, origin_reference_id, continuation_token,
                    continuation_expires_at)
                values (?, ?, 'SYNTHETIC_REDACTED', ?, 'COMPLETED', 'Synthetic disclaimer',
                        ?, ?, ?, 'POSTPARTUM', ?, ?, 'MOTHER_JOURNEY', ?, ?, ?)
                """,
                intakeId,
                userId,
                riskLevel,
                Timestamp.from(OCCURRED_AT.minusSeconds(10)),
                Timestamp.from(OCCURRED_AT),
                userId,
                "story67-" + intakeId,
                boundJourneyId,
                boundJourneyId,
                UUID.randomUUID(),
                Timestamp.from(OCCURRED_AT.plusSeconds(604800)));
        return intakeId;
    }

    private UUID insertEmergency(UUID userId) {
        UUID emergencyId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into emergency_sessions (
                    id, user_id, status, trigger_source, created_at, created_by)
                values (?, ?, 'ACTIVE', 'AUTO_TRIAGE', ?, ?)
                """, emergencyId, userId, Timestamp.from(OCCURRED_AT), userId);
        return emergencyId;
    }

    private void linkEscalation(UUID intakeId, UUID emergencyId, UUID userId) {
        jdbcTemplate.update("""
                insert into triage_emergency_escalations (
                    intake_session_id, emergency_session_id, user_id, triggered_at)
                values (?, ?, ?, ?)
                """, intakeId, emergencyId, userId, Timestamp.from(OCCURRED_AT));
    }

    private long countOutcomes(UUID intakeId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from lifecycle_safety_outcomes where intake_session_id = ?",
                Long.class,
                intakeId);
    }

    private UUID seedMother(String email) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("$2a$10$abcdefghijklmnopqrstuv")
                .name("Story 6.7 Synthetic Mother")
                .role(Role.MOTHER)
                .enabled(true)
                .locked(false)
                .accountStatus("ACTIVE")
                .emailVerified(true)
                .phoneVerified(false)
                .build());
        return user.getId();
    }

    private UUID seedJourney(UUID userId, JourneyType type) {
        MotherJourney journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(userId)
                .journeyType(type)
                .startDate(LocalDate.of(2026, 7, 1))
                .status(JourneyStatus.ACTIVE)
                .build());
        return journey.getId();
    }

    private List<String> columnNames(String tableName) {
        return jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = 'public' and table_name = ?
                order by ordinal_position
                """, String.class, tableName);
    }

    private Map<String, String> constraintDefinitions(String tableName, String type) {
        return jdbcTemplate.query("""
                select conname, pg_get_constraintdef(oid) as definition
                from pg_constraint
                where conrelid = ('public.' || ?)::regclass and contype = ?
                order by conname
                """, resultSet -> {
                    Map<String, String> definitions = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        definitions.put(resultSet.getString("conname"), resultSet.getString("definition"));
                    }
                    return definitions;
                }, tableName, type);
    }

    private String indexDefinition(String indexName) {
        return jdbcTemplate.queryForObject(
                "select indexdef from pg_indexes where schemaname = 'public' and indexname = ?",
                String.class,
                indexName);
    }

    private static final class RollbackProbeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
