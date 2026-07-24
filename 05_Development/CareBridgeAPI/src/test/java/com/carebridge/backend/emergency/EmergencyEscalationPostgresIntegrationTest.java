package com.carebridge.backend.emergency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.emergency.service.IFamilyAlertService;
import com.carebridge.backend.emergency.service.EmergencyNotificationOutboxDeliveryService;
import com.carebridge.backend.emergency.service.FamilyAlertDeliveryOutcome;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.ITriageService;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Real PostgreSQL coverage for Story 6.6 RED-to-emergency invariants. */
class EmergencyEscalationPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private IEmergencyService emergencyService;
    @Autowired private ITriageService triageService;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @Autowired private EmergencyNotificationOutboxDeliveryService outboxDeliveryService;

    @MockitoBean private IFamilyAlertService familyAlertService;
    @MockitoBean private ChildTriageAiClient childTriageAiClient;
    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;

    private UUID firstUserId;

    @BeforeEach
    void setUp() {
        wipeUsersAndDependents();
        clearInvocations(familyAlertService);
        clearInvocations(childTriageAiClient);
        when(familyAlertService.sendAlert(any(EmergencySessionOpened.class)))
                .thenReturn(FamilyAlertDeliveryOutcome.DELIVERED);
        firstUserId = seedMother("story66-first@carebridge.test", "Story 6.6 First Mother");
    }

    @AfterEach
    void cleanUp() {
        wipeUsersAndDependents();
    }

    @Test
    void migration_shouldCreateAssociationTableAndEnforceOneActiveSessionPerUser() {
        assertThat(jdbcTemplate.queryForObject(
                "select to_regclass('public.triage_emergency_escalations')::text",
                String.class)).isEqualTo("triage_emergency_escalations");
        assertThat(jdbcTemplate.queryForObject(
                "select to_regclass('public.emergency_notification_outbox')::text",
                String.class)).isEqualTo("emergency_notification_outbox");

        assertThat(columnNames("triage_emergency_escalations")).containsExactly(
                "intake_session_id", "emergency_session_id", "user_id", "triggered_at");
        assertThat(columnNames("emergency_notification_outbox")).containsExactly(
                "emergency_session_id", "status", "attempt_count", "next_attempt_at",
                "last_error_code", "claim_token", "created_at", "delivered_at", "terminal_at");

        Map<String, String> escalationForeignKeys =
                constraintDefinitions("triage_emergency_escalations", "f");
        assertThat(escalationForeignKeys).containsKeys(
                "triage_emergency_escalations_intake_session_id_fkey",
                "triage_emergency_escalations_emergency_session_id_fkey",
                "triage_emergency_escalations_user_id_fkey",
                "fk_triage_escalation_intake_owner",
                "fk_triage_escalation_emergency_owner");
        assertThat(escalationForeignKeys.get("fk_triage_escalation_intake_owner"))
                .contains("FOREIGN KEY (intake_session_id, user_id)")
                .contains("REFERENCES intake_sessions(id, user_id)");
        assertThat(escalationForeignKeys.get("fk_triage_escalation_emergency_owner"))
                .contains("FOREIGN KEY (emergency_session_id, user_id)")
                .contains("REFERENCES emergency_sessions(id, user_id)");

        Map<String, String> outboxForeignKeys =
                constraintDefinitions("emergency_notification_outbox", "f");
        assertThat(outboxForeignKeys)
                .containsKey("emergency_notification_outbox_emergency_session_id_fkey");
        assertThat(outboxForeignKeys.values()).allSatisfy(definition -> assertThat(definition)
                .contains("FOREIGN KEY (emergency_session_id)")
                .contains("REFERENCES emergency_sessions(id)"));

        Map<String, String> escalationPrimaryKeys =
                constraintDefinitions("triage_emergency_escalations", "p");
        assertThat(escalationPrimaryKeys).containsKey("triage_emergency_escalations_pkey");
        assertThat(escalationPrimaryKeys.get("triage_emergency_escalations_pkey"))
                .contains("PRIMARY KEY (intake_session_id)");
        Map<String, String> outboxPrimaryKeys =
                constraintDefinitions("emergency_notification_outbox", "p");
        assertThat(outboxPrimaryKeys).containsKey("emergency_notification_outbox_pkey");
        assertThat(outboxPrimaryKeys.get("emergency_notification_outbox_pkey"))
                .contains("PRIMARY KEY (emergency_session_id)");

        assertThat(indexDefinition("uq_intake_sessions_id_user"))
                .containsIgnoringCase("UNIQUE INDEX")
                .contains("intake_sessions")
                .contains("(id, user_id)");
        assertThat(indexDefinition("uq_emergency_sessions_id_user"))
                .containsIgnoringCase("UNIQUE INDEX")
                .contains("emergency_sessions")
                .contains("(id, user_id)");
        assertThat(indexDefinition("idx_triage_emergency_escalations_emergency"))
                .contains("triage_emergency_escalations")
                .contains("emergency_session_id");
        assertThat(indexDefinition("idx_triage_emergency_escalations_user"))
                .contains("triage_emergency_escalations")
                .contains("user_id")
                .contains("triggered_at");

        Map<String, String> outboxChecks =
                constraintDefinitions("emergency_notification_outbox", "c");
        assertThat(outboxChecks).containsKeys(
                "chk_emergency_notification_outbox_status",
                "chk_emergency_notification_outbox_attempts",
                "chk_emergency_notification_outbox_terminal_state",
                "chk_emergency_notification_outbox_claim_state");
        assertThat(outboxChecks.get("chk_emergency_notification_outbox_status"))
                .contains("status", "PENDING", "DELIVERED", "SUPPRESSED");
        assertThat(outboxChecks.get("chk_emergency_notification_outbox_attempts"))
                .contains("attempt_count", ">= 0");
        assertThat(outboxChecks.get("chk_emergency_notification_outbox_terminal_state"))
                .contains("terminal_at", "delivered_at", "PENDING", "DELIVERED", "SUPPRESSED");
        assertThat(outboxChecks.get("chk_emergency_notification_outbox_claim_state"))
                .contains("claim_token", "status", "PENDING");

        String indexDefinition = indexDefinition("uq_emergency_sessions_one_active_per_user");
        assertThat(indexDefinition)
                .containsIgnoringCase("UNIQUE INDEX")
                .contains("emergency_sessions")
                .containsIgnoringCase("WHERE")
                .contains("status")
                .contains("ACTIVE");
        assertThat(indexDefinition("idx_emergency_notification_outbox_pending"))
                .contains("emergency_notification_outbox")
                .contains("next_attempt_at")
                .contains("created_at");

        insertEmergency(UUID.randomUUID(), firstUserId, "ACTIVE", "MANUAL");

        assertThatThrownBy(() ->
                insertEmergency(UUID.randomUUID(), firstUserId, "ACTIVE", "AUTO_TRIAGE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void expiredClaimLease_whileFirstProviderCallRuns_shouldNotDuplicateDelivery() throws Exception {
        UUID sessionId = UUID.randomUUID();
        insertEmergency(sessionId, firstUserId, "ACTIVE", "MANUAL");
        jdbcTemplate.update(
                "insert into emergency_notification_outbox (emergency_session_id) values (?)",
                sessionId);

        CountDownLatch providerEntered = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        doAnswer(invocation -> {
            providerEntered.countDown();
            if (!releaseProvider.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("provider release timed out");
            }
            return FamilyAlertDeliveryOutcome.DELIVERED;
        }).when(familyAlertService).sendAlert(any(EmergencySessionOpened.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> firstDelivery = executor.submit(() -> outboxDeliveryService.deliver(sessionId));
        try {
            assertThat(providerEntered.await(5, TimeUnit.SECONDS)).isTrue();

            // Simulate the old 30-second lease expiring while provider I/O is still running.
            jdbcTemplate.update(
                    "update emergency_notification_outbox set next_attempt_at = now() - interval '1 second' "
                            + "where emergency_session_id = ?",
                    sessionId);
            Future<?> competingDelivery =
                    executor.submit(() -> outboxDeliveryService.deliver(sessionId));

            competingDelivery.get(5, TimeUnit.SECONDS);
            releaseProvider.countDown();
            firstDelivery.get(5, TimeUnit.SECONDS);
        } finally {
            releaseProvider.countDown();
            executor.shutdownNow();
        }

        verify(familyAlertService, times(1)).sendAlert(any(EmergencySessionOpened.class));
        assertThat(jdbcTemplate.queryForObject(
                "select status from emergency_notification_outbox where emergency_session_id = ?",
                String.class,
                sessionId)).isEqualTo("DELIVERED");
    }

    @Test
    void sameIntakeReplay_shouldReturnOneDurablyLinkedEmergencySession() {
        UUID intakeSessionId = insertCompletedRedIntake(firstUserId);

        EmergencySessionResponse first =
                emergencyService.openOrReuseFromTriage(intakeSessionId, firstUserId);
        EmergencySessionResponse replay =
                emergencyService.openOrReuseFromTriage(intakeSessionId, firstUserId);

        assertThat(replay.getSessionId()).isEqualTo(first.getSessionId());
        assertThat(countActiveSessions(firstUserId)).isEqualTo(1L);
        assertThat(countEscalationLinks(firstUserId)).isEqualTo(1L);
        verify(familyAlertService, times(1)).sendAlert(any(EmergencySessionOpened.class));
    }

    @Test
    void concurrentReplayOfSameIntake_shouldCreateOneLinkAndOneActiveSession() throws Exception {
        UUID intakeSessionId = insertCompletedRedIntake(firstUserId);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<UUID> first = executor.submit(() -> {
            start.await();
            return emergencyService.openOrReuseFromTriage(intakeSessionId, firstUserId).getSessionId();
        });
        Future<UUID> second = executor.submit(() -> {
            start.await();
            return emergencyService.openOrReuseFromTriage(intakeSessionId, firstUserId).getSessionId();
        });

        UUID firstEmergencyId;
        UUID secondEmergencyId;
        try {
            start.countDown();
            firstEmergencyId = first.get(15, TimeUnit.SECONDS);
            secondEmergencyId = second.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(secondEmergencyId).isEqualTo(firstEmergencyId);
        assertThat(countActiveSessions(firstUserId)).isEqualTo(1L);
        assertThat(countEscalationLinks(firstUserId)).isEqualTo(1L);
        verify(familyAlertService, times(1)).sendAlert(any(EmergencySessionOpened.class));
    }

    @Test
    void concurrentRedIntakesForSameUser_shouldCreateTwoLinksToOneActiveSession() throws Exception {
        UUID firstIntakeId = insertCompletedRedIntake(firstUserId);
        UUID secondIntakeId = insertCompletedRedIntake(firstUserId);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<UUID> first = executor.submit(() -> {
            start.await();
            return emergencyService.openOrReuseFromTriage(firstIntakeId, firstUserId).getSessionId();
        });
        Future<UUID> second = executor.submit(() -> {
            start.await();
            return emergencyService.openOrReuseFromTriage(secondIntakeId, firstUserId).getSessionId();
        });

        UUID firstEmergencyId;
        UUID secondEmergencyId;
        try {
            start.countDown();
            firstEmergencyId = first.get(15, TimeUnit.SECONDS);
            secondEmergencyId = second.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(secondEmergencyId).isEqualTo(firstEmergencyId);
        assertThat(countActiveSessions(firstUserId)).isEqualTo(1L);
        assertThat(countEscalationLinks(firstUserId)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForList(
                "select distinct emergency_session_id from triage_emergency_escalations where user_id = ?",
                UUID.class,
                firstUserId)).containsExactly(firstEmergencyId);
        verify(familyAlertService, times(1)).sendAlert(any(EmergencySessionOpened.class));
    }

    @Test
    void existingManualActiveSession_shouldBeReusedAndAssociatedWithoutSecondCreationEvent() {
        EmergencySessionResponse manual = emergencyService.openFlow(
                OpenEmergencyRequest.builder().triggerSource("MANUAL").build(), firstUserId);
        UUID intakeSessionId = insertCompletedRedIntake(firstUserId);

        EmergencySessionResponse triage =
                emergencyService.openOrReuseFromTriage(intakeSessionId, firstUserId);

        assertThat(triage.getSessionId()).isEqualTo(manual.getSessionId());
        assertThat(countActiveSessions(firstUserId)).isEqualTo(1L);
        assertThat(countEscalationLinks(firstUserId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select trigger_source from emergency_sessions where id = ?",
                String.class,
                manual.getSessionId())).isEqualTo("MANUAL");
        verify(familyAlertService, times(1)).sendAlert(any(EmergencySessionOpened.class));
    }

    @Test
    void redIntakesForDifferentUsers_shouldRemainIsolated() {
        UUID secondUserId = seedMother("story66-second@carebridge.test", "Story 6.6 Second Mother");
        UUID firstIntakeId = insertCompletedRedIntake(firstUserId);
        UUID secondIntakeId = insertCompletedRedIntake(secondUserId);

        EmergencySessionResponse first =
                emergencyService.openOrReuseFromTriage(firstIntakeId, firstUserId);
        EmergencySessionResponse second =
                emergencyService.openOrReuseFromTriage(secondIntakeId, secondUserId);

        assertThat(second.getSessionId()).isNotEqualTo(first.getSessionId());
        assertThat(first.getUserId()).isEqualTo(firstUserId);
        assertThat(second.getUserId()).isEqualTo(secondUserId);
        assertThat(countActiveSessions(firstUserId)).isEqualTo(1L);
        assertThat(countActiveSessions(secondUserId)).isEqualTo(1L);
        assertThat(countEscalationLinks(firstUserId)).isEqualTo(1L);
        assertThat(countEscalationLinks(secondUserId)).isEqualTo(1L);
        verify(familyAlertService, times(2)).sendAlert(any(EmergencySessionOpened.class));
    }

    @Test
    void runIntake_pythonRed_shouldSynchronouslyCreateDurableEmergencyAssociation() {
        when(childTriageAiClient.triageChild(any())).thenReturn(redJson());

        IntakeSessionResponse response = triageService.runIntake(infantRequest(), firstUserId);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getRiskLevel()).isEqualTo("RED");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from triage_emergency_escalations where intake_session_id = ?",
                Long.class,
                response.getSessionId())).isEqualTo(1L);
        assertThat(countActiveSessions(firstUserId)).isEqualTo(1L);
        verify(familyAlertService, times(1)).sendAlert(any(EmergencySessionOpened.class));
    }

    @Test
    void runIntake_yellowAndNeedMoreInfo_shouldCreateNoEmergencyOrAssociation() {
        when(childTriageAiClient.triageChild(any()))
                .thenReturn(yellowJson(), needMoreInfoJson());

        IntakeSessionResponse yellow = triageService.runIntake(infantRequest(), firstUserId);
        IntakeSessionResponse needMore = triageService.runIntake(infantRequest(), firstUserId);

        assertThat(yellow.getRiskLevel()).isEqualTo("YELLOW");
        assertThat(needMore.getStatus()).isEqualTo("NEED_MORE_INFO");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from emergency_sessions", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from triage_emergency_escalations", Long.class)).isZero();
    }

    @Test
    void migration_shouldBeRepeatSafeAndReconcileLegacyDuplicatesWithoutDeletingHistory()
            throws Exception {
        String schema = "story66_" + UUID.randomUUID().toString().replace("-", "");
        String quotedSchema = quoteIdentifier(schema);
        UUID userId = UUID.randomUUID();
        UUID oldestId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID laterId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant oldestCreatedAt = Instant.parse("2026-07-22T01:00:00Z");
        Instant laterCreatedAt = Instant.parse("2026-07-22T02:00:00Z");

        jdbcTemplate.execute("create schema " + quotedSchema);
        try {
            jdbcTemplate.execute("create table " + quotedSchema
                    + ".users (user_id uuid primary key)");
            jdbcTemplate.execute("create table " + quotedSchema
                    + ".intake_sessions (id uuid primary key, user_id uuid not null references "
                    + quotedSchema + ".users(user_id))");
            jdbcTemplate.execute("create table " + quotedSchema
                    + ".emergency_sessions (id uuid primary key, user_id uuid not null references "
                    + quotedSchema + ".users(user_id), status varchar(20) not null, "
                    + "created_at timestamptz not null, resolved_at timestamptz)");
            jdbcTemplate.execute("create table " + quotedSchema
                    + ".emergency_reference (id uuid primary key, emergency_session_id uuid not null references "
                    + quotedSchema + ".emergency_sessions(id))");
            jdbcTemplate.update("insert into " + quotedSchema + ".users(user_id) values (?)", userId);
            insertLegacyEmergency(quotedSchema, oldestId, userId, oldestCreatedAt);
            insertLegacyEmergency(quotedSchema, laterId, userId, laterCreatedAt);
            jdbcTemplate.update("insert into " + quotedSchema
                    + ".emergency_reference(id, emergency_session_id) values (?, ?), (?, ?)",
                    UUID.randomUUID(), oldestId, UUID.randomUUID(), laterId);

            executeStory66MigrationInSchema(schema);
            executeStory66MigrationInSchema(schema);

            assertThat(jdbcTemplate.queryForObject("select status from " + quotedSchema
                    + ".emergency_sessions where id = ?", String.class, oldestId))
                    .isEqualTo("ACTIVE");
            assertThat(jdbcTemplate.queryForObject("select status from " + quotedSchema
                    + ".emergency_sessions where id = ?", String.class, laterId))
                    .isEqualTo("CANCELLED");
            assertThat(jdbcTemplate.queryForObject("select resolved_at from " + quotedSchema
                    + ".emergency_sessions where id = ?", Instant.class, laterId))
                    .isEqualTo(laterCreatedAt);
            assertThat(jdbcTemplate.queryForObject("select count(*) from " + quotedSchema
                    + ".emergency_reference", Long.class)).isEqualTo(2L);
            assertThatThrownBy(() -> insertLegacyEmergency(
                    quotedSchema, UUID.randomUUID(), userId, Instant.parse("2026-07-22T03:00:00Z")))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.execute("drop schema " + quotedSchema + " cascade");
        }
    }

    @Test
    void migration_transactionRollback_shouldRestoreLegacyStateAndRemoveNewObjects()
            throws Exception {
        String schema = "story66_rollback_" + UUID.randomUUID().toString().replace("-", "");
        String quotedSchema = quoteIdentifier(schema);
        UUID userId = UUID.randomUUID();
        UUID oldestId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID laterId = UUID.fromString("00000000-0000-0000-0000-000000000012");

        jdbcTemplate.execute("create schema " + quotedSchema);
        try {
            jdbcTemplate.execute("create table " + quotedSchema
                    + ".users (user_id uuid primary key)");
            jdbcTemplate.execute("create table " + quotedSchema
                    + ".intake_sessions (id uuid primary key, user_id uuid not null references "
                    + quotedSchema + ".users(user_id))");
            jdbcTemplate.execute("create table " + quotedSchema
                    + ".emergency_sessions (id uuid primary key, user_id uuid not null references "
                    + quotedSchema + ".users(user_id), status varchar(20) not null, "
                    + "created_at timestamptz not null, resolved_at timestamptz)");
            jdbcTemplate.execute("create table " + quotedSchema
                    + ".emergency_reference (id uuid primary key, emergency_session_id uuid not null references "
                    + quotedSchema + ".emergency_sessions(id))");
            jdbcTemplate.update("insert into " + quotedSchema + ".users(user_id) values (?)", userId);
            insertLegacyEmergency(
                    quotedSchema, oldestId, userId, Instant.parse("2026-07-22T01:00:00Z"));
            insertLegacyEmergency(
                    quotedSchema, laterId, userId, Instant.parse("2026-07-22T02:00:00Z"));
            jdbcTemplate.update("insert into " + quotedSchema
                    + ".emergency_reference(id, emergency_session_id) values (?, ?), (?, ?)",
                    UUID.randomUUID(), oldestId, UUID.randomUUID(), laterId);

            String migrationSql = new ClassPathResource(
                    "db/migration/V20260722120000__guarantee_triage_emergency_idempotency.sql")
                    .getContentAsString(StandardCharsets.UTF_8);
            try (Connection connection = dataSource.getConnection();
                    var statement = connection.createStatement()) {
                connection.setAutoCommit(false);
                statement.execute("set search_path to " + quotedSchema);
                statement.execute(migrationSql);
                connection.rollback();
            }

            assertThat(jdbcTemplate.queryForObject("select count(*) from " + quotedSchema
                    + ".emergency_sessions where status = 'ACTIVE'", Long.class)).isEqualTo(2L);
            assertThat(jdbcTemplate.queryForObject("select count(*) from " + quotedSchema
                    + ".emergency_reference", Long.class)).isEqualTo(2L);
            assertThat(jdbcTemplate.queryForObject(
                    "select to_regclass(?)::text",
                    String.class,
                    schema + ".uq_emergency_sessions_one_active_per_user"))
                    .isNull();
            assertThat(jdbcTemplate.queryForObject(
                    "select to_regclass(?)::text",
                    String.class,
                    schema + ".triage_emergency_escalations"))
                    .isNull();
            assertThat(jdbcTemplate.queryForObject(
                    "select to_regclass(?)::text",
                    String.class,
                    schema + ".emergency_notification_outbox"))
                    .isNull();
        } finally {
            jdbcTemplate.execute("drop schema " + quotedSchema + " cascade");
        }
    }

    private UUID seedMother(String email, String name) {
        User mother = userRepository.save(User.builder()
                .email(email)
                .passwordHash("$2a$10$abcdefghijklmnopqrstuv")
                .name(name)
                .role(Role.MOTHER)
                .enabled(true)
                .locked(false)
                .accountStatus("ACTIVE")
                .emailVerified(true)
                .phoneVerified(false)
                .build());
        return mother.getId();
    }

    private List<String> columnNames(String tableName) {
        return jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = 'public' and table_name = ?
                order by ordinal_position
                """, String.class, tableName);
    }

    private Map<String, String> constraintDefinitions(String tableName, String constraintType) {
        return jdbcTemplate.query("""
                select conname as constraint_name, pg_get_constraintdef(oid) as definition
                from pg_constraint
                where conrelid = ('public.' || ?)::regclass and contype = ?
                order by conname
                """, resultSet -> {
                    Map<String, String> definitions = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        definitions.put(
                                resultSet.getString("constraint_name"),
                                resultSet.getString("definition"));
                    }
                    return definitions;
                }, tableName, constraintType);
    }

    private String indexDefinition(String indexName) {
        return jdbcTemplate.queryForObject(
                "select indexdef from pg_indexes where schemaname = 'public' and indexname = ?",
                String.class,
                indexName);
    }

    private RunIntakeRequest infantRequest() {
        return RunIntakeRequest.builder()
                .stage(TriageStage.INFANT)
                .symptoms("SYNTHETIC_REDACTED")
                .childAgeMonths(6)
                .breathingStatus("NORMAL")
                .consciousnessStatus("ALERT")
                .feedingStatus("NORMAL")
                .build();
    }

    private String redJson() {
        return """
                {"riskLevel":"RED","riskColor":"#EF4444","summary":"Synthetic danger sign",
                 "possibleConcern":"Synthetic breathing concern","recommendedAction":"Seek emergency care",
                 "emergencyActionRequired":true,"redFlags":["SYNTHETIC_DANGER"],
                 "matchedRules":["RED_BREATHING_DISTRESS"],"citations":[],"questions":[],
                 "recommendationCode":"SEEK_EMERGENCY_CARE","disclaimer":"Risk classification only."}
                """;
    }

    private String yellowJson() {
        return """
                {"riskLevel":"YELLOW","riskColor":"#F59E0B","summary":"Review recommended",
                 "recommendedAction":"Contact a clinician","emergencyActionRequired":false,
                 "redFlags":[],"matchedRules":["YELLOW_REVIEW"],"citations":[],"questions":[],
                 "disclaimer":"Risk classification only."}
                """;
    }

    private String needMoreInfoJson() {
        return """
                {"riskLevel":"NEED_MORE_INFO","status":"NEED_MORE_INFO",
                 "questions":[{"questionKey":"duration","text":"How long?","answerType":"TEXT","options":[]}],
                 "disclaimer":"More information is required."}
                """;
    }

    private void executeStory66MigrationInSchema(String schema) throws Exception {
        String migrationSql = new ClassPathResource(
                "db/migration/V20260722120000__guarantee_triage_emergency_idempotency.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("set search_path to " + quoteIdentifier(schema));
                try {
                    statement.execute(migrationSql);
                } finally {
                    statement.execute("set search_path to public");
                }
            }
            return null;
        });
    }

    private void insertLegacyEmergency(
            String quotedSchema, UUID sessionId, UUID userId, Instant createdAt) {
        jdbcTemplate.update("insert into " + quotedSchema
                + ".emergency_sessions(id, user_id, status, created_at) values (?, ?, 'ACTIVE', ?)",
                sessionId, userId, Timestamp.from(createdAt));
    }

    private String quoteIdentifier(String identifier) {
        if (!identifier.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Unsafe SQL identifier");
        }
        return '"' + identifier + '"';
    }

    private UUID insertCompletedRedIntake(UUID userId) {
        UUID intakeSessionId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update("""
                insert into intake_sessions (
                    id, user_id, symptoms, raw_ai_response, risk_level, status,
                    disclaimer, created_at, completed_at, created_by, stage)
                values (?, ?, 'SYNTHETIC_REDACTED', '{"riskLevel":"RED"}', 'RED', 'COMPLETED',
                        'Synthetic test disclaimer', ?, ?, ?, 'POSTPARTUM')
                """,
                intakeSessionId,
                userId,
                Timestamp.from(now),
                Timestamp.from(now),
                userId);
        return intakeSessionId;
    }

    private void insertEmergency(UUID sessionId, UUID userId, String status, String triggerSource) {
        jdbcTemplate.update("""
                insert into emergency_sessions (
                    id, user_id, status, trigger_source, created_at, created_by)
                values (?, ?, ?, ?, ?, ?)
                """,
                sessionId,
                userId,
                status,
                triggerSource,
                Timestamp.from(Instant.now()),
                userId);
    }

    private long countActiveSessions(UUID userId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from emergency_sessions where user_id = ? and status = 'ACTIVE'",
                Long.class,
                userId);
    }

    private long countEscalationLinks(UUID userId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from triage_emergency_escalations where user_id = ?",
                Long.class,
                userId);
    }

    private void wipeUsersAndDependents() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }
}
