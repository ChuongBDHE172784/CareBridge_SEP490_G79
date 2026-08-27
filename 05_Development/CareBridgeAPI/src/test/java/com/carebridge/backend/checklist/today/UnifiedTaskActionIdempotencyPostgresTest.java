package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.repository.ChecklistActionCommandRepository;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.provider.AuthorizedTask;
import com.carebridge.backend.checklist.today.provider.ReminderOccurrenceIdFactory;
import com.carebridge.backend.checklist.today.provider.ReminderTaskActionHandler;
import com.carebridge.backend.checklist.today.provider.ReminderTodayTaskProvider;
import com.carebridge.backend.checklist.today.provider.TaskActionHandler;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.service.IReminderService;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
class UnifiedTaskActionIdempotencyPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    private static final UUID ACTOR = UUID.fromString("40000000-0000-0000-0000-000000000101");
    private static final UUID FAMILY = UUID.fromString("40000000-0000-0000-0000-000000000102");
    private static final UUID TASK = UUID.fromString("40000000-0000-0000-0000-000000000401");
    private static final UUID REQUEST_ID = UUID.fromString("40000000-0000-0000-0000-000000000601");
    private static final UUID SECOND_REQUEST_ID = UUID.fromString("40000000-0000-0000-0000-000000000602");
    private static final UUID REMINDER_OCCURRENCE = UUID.fromString("40000000-0000-0000-0000-000000000402");
    private static final UUID REMINDER_DEFINITION = UUID.fromString("40000000-0000-0000-0000-000000000403");
    private static final UUID CARE_SUBJECT = UUID.fromString("40000000-0000-0000-0000-000000000404");
    private static final UUID JOURNEY = UUID.fromString("40000000-0000-0000-0000-000000000405");
    private static final UUID CARE_GROUP = UUID.fromString("40000000-0000-0000-0000-000000000406");
    private static final Instant NOW = Instant.parse("2026-08-03T12:00:00Z");

    @Autowired private ChecklistActionCommandRepository commandRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReminderTaskActionHandler reminderTaskActionHandler;
    @Autowired private ReminderTodayTaskProvider reminderTodayTaskProvider;
    @Autowired private IReminderService reminderService;

    @BeforeEach
    void setUp() {
        cleanFixtures();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, ACTOR, "CHK-028 Actor", "+8498" + System.nanoTime(), "MOTHER");
        CanonicalUserFixture.insertUser(
                jdbcTemplate, FAMILY, "CHK-028 Family", "+8497" + System.nanoTime(), "FAMILY");
        jdbcTemplate.update("""
                insert into care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                values (?, ?, ?, 'MOTHER', 'CHK-028 Actor', 'ACTIVE', now(), now())
                """, CARE_SUBJECT, ACTOR, ACTOR);
        jdbcTemplate.update("""
                insert into mother_journeys (
                    journey_id, owner_user_id, care_subject_id, journey_type,
                    start_date, status, version, created_at, updated_at)
                values (?, ?, ?, 'PREGNANCY', current_date, 'ACTIVE', 0, now(), now())
                """, JOURNEY, ACTOR, CARE_SUBJECT);
        jdbcTemplate.update("""
                insert into care_groups (
                    care_group_id, owner_user_id, group_name, linked_journey_id,
                    status, created_at, updated_at)
                values (?, ?, 'CHK-028 current context', ?, 'ACTIVE', now(), now())
                """, CARE_GROUP, ACTOR, JOURNEY);
        jdbcTemplate.update("""
                insert into care_group_members (
                    care_group_member_id, care_group_id, user_id, member_role,
                    invitation_status, permission_json, created_at, updated_at)
                values (gen_random_uuid(), ?, ?, 'MEMBER', 'ACCEPTED',
                        '{"CHECKLIST_VIEW":true,"CHECKLIST_COMPLETE":true}'::jsonb,
                        now(), now())
                """, CARE_GROUP, FAMILY);
        jdbcTemplate.update("""
                insert into care_tasks (
                    task_id, task_type, owner_user_id, creator_user_id, assignee_user_id,
                    title, status, origin, target_subject, created_at, updated_at)
                values (?, 'MANUAL_TASK', ?, ?, ?, 'Atomic idempotency task', 'OPEN',
                        'USER_CREATED', 'MOTHER', now(), now())
                """, TASK, ACTOR, ACTOR, ACTOR);
        jdbcTemplate.update("""
                insert into care_tasks (
                    task_id, task_type, owner_user_id, title, scheduled_at, status,
                    origin, target_subject, journey_id, created_at, updated_at)
                values (?, 'SCHEDULED_REMINDER', ?, 'Atomic reminder', now(), 'PENDING',
                        'USER_CREATED', 'MOTHER', ?, now(), now())
                """, REMINDER_DEFINITION, ACTOR, JOURNEY);
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    @Test
    void chk028_samePayloadMutatesOnceAndSecondTransactionReplaysOriginal() throws Exception {
        BarrierHandler handler = new BarrierHandler();
        UnifiedTaskActionFacade facade = facade(handler);
        TaskActionRequest request = new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null);

        List<Outcome> outcomes = invokeConcurrently(facade, request, request);

        assertThat(handler.authorizationCount()).isEqualTo(3);
        assertThat(handler.mutationCount()).isEqualTo(1);
        assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome.error()).isNull());
        TaskActionResponse original = outcomes.stream()
                .map(Outcome::response)
                .filter(response -> !response.idempotentReplay())
                .findFirst()
                .orElseThrow();
        TaskActionResponse replay = outcomes.stream()
                .map(Outcome::response)
                .filter(TaskActionResponse::idempotentReplay)
                .findFirst()
                .orElseThrow();
        assertThat(replay.correlationId()).isEqualTo(original.correlationId());
        assertThat(replay.appliedAt()).isEqualTo(original.appliedAt());
        assertThat(commandCount()).isEqualTo(1L);
    }

    @Test
    void chk028_changedPayloadIsRejectedAfterBothTransactionsAuthorize() throws Exception {
        BarrierHandler handler = new BarrierHandler();
        UnifiedTaskActionFacade facade = facade(handler);
        TaskActionRequest complete = new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null);
        TaskActionRequest skip = new TaskActionRequest(TaskAction.SKIP, REQUEST_ID, "USER_CHOICE");

        List<Outcome> outcomes = invokeConcurrently(facade, complete, skip);

        assertThat(handler.authorizationCount()).isEqualTo(3);
        assertThat(handler.mutationCount()).isEqualTo(1);
        assertThat(outcomes.stream().filter(outcome -> outcome.response() != null)).hasSize(1);
        assertThat(outcomes.stream().map(Outcome::error).filter(BusinessException.class::isInstance)
                .map(BusinessException.class::cast).map(BusinessException::getCode))
                .containsExactly("IDEMPOTENCY_KEY_REUSE");
        assertThat(commandCount()).isEqualTo(1L);
    }

    @Test
    void chk028_differentRequestIdsSerializeCareTaskTerminalMutation() throws Exception {
        assertDifferentRequestIdsSerializeTerminalMutation(
                TaskKind.CARE_TASK, TASK, null);
    }

    @Test
    void chk028_differentRequestIdsSerializeReminderTerminalMutation() throws Exception {
        assertDifferentRequestIdsSerializeTerminalMutation(
                TaskKind.REMINDER, REMINDER_OCCURRENCE, REMINDER_DEFINITION);
    }

    @Test
    void oldAndNewAliasesForOneReminderSerializeCompleteVersusSkipByDefinition() throws Exception {
        Instant newSchedule = jdbcTemplate.queryForObject(
                "select scheduled_at from care_tasks where task_id=?",
                (resultSet, row) -> resultSet.getTimestamp(1).toInstant(), REMINDER_DEFINITION);
        Instant oldSchedule = newSchedule.minus(1, ChronoUnit.DAYS);
        UUID newOccurrence = ReminderOccurrenceIdFactory.create(REMINDER_DEFINITION, newSchedule);
        UUID oldOccurrence = ReminderOccurrenceIdFactory.create(REMINDER_DEFINITION, oldSchedule);
        new JdbcTemplate(POSTGRES.getPostgresDatabase()).update("""
                insert into reminder_occurrence_aliases (
                    occurrence_id, reminder_definition_id, owner_user_id, scheduled_at)
                values (?, ?, ?, ?)
                on conflict (occurrence_id) do nothing
                """, oldOccurrence, REMINDER_DEFINITION, ACTOR,
                java.sql.Timestamp.from(oldSchedule));
        BarrierDelegatingHandler handler = new BarrierDelegatingHandler(reminderTaskActionHandler);
        UnifiedTaskActionFacade facade = facade(handler);

        List<Outcome> outcomes = invokeConcurrently(
                facade,
                oldOccurrence,
                new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null),
                newOccurrence,
                new TaskActionRequest(TaskAction.SKIP, SECOND_REQUEST_ID, "USER_CHOICE"));

        assertThat(handler.mutationCount()).isEqualTo(1);
        assertThat(outcomes.stream().filter(outcome -> outcome.response() != null)).hasSize(1);
        assertThat(outcomes.stream().map(Outcome::error).filter(BusinessException.class::isInstance)
                .map(BusinessException.class::cast).map(BusinessException::getCode))
                .containsExactly("TASK_ALREADY_TERMINAL");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from checklist_action_commands
                 where actor_user_id=? and task_kind='REMINDER'
                   and reminder_definition_id=?
                """, Long.class, ACTOR, REMINDER_DEFINITION)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from audit_events
                 where actor_user_id=? and resource_type='ReminderOccurrence'
                   and event_category in ('REMINDER_COMPLETED', 'REMINDER_SKIPPED')
                """, Long.class, ACTOR)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select status from care_tasks where task_id=?",
                String.class, REMINDER_DEFINITION)).isIn("COMPLETED", "SKIPPED");
    }

    @Test
    void reenabledReminderAtSameScheduleGetsFreshOccurrenceAndCannotReplayOldCommand() {
        Instant schedule = jdbcTemplate.queryForObject(
                "select scheduled_at from care_tasks where task_id=?",
                (resultSet, row) -> resultSet.getTimestamp(1).toInstant(), REMINDER_DEFINITION);
        UUID oldOccurrence = ReminderOccurrenceIdFactory.create(REMINDER_DEFINITION, schedule);
        UnifiedTaskActionFacade facade = facade(reminderTaskActionHandler);
        TaskActionRequest request = new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null);

        Outcome first = invoke(facade, TaskKind.REMINDER, oldOccurrence, request);
        assertThat(first.error()).isNull();
        assertThat(first.response().idempotentReplay()).isFalse();

        reminderService.deleteReminder(REMINDER_DEFINITION, ACTOR);
        reminderService.enableReminder(REMINDER_DEFINITION, ACTOR);
        UUID reenabledOccurrence = reminderTodayTaskProvider.findAuthorizedTasks(ACTOR).stream()
                .filter(task -> task.instanceId() == null)
                .map(task -> task.taskId())
                .findFirst()
                .orElseThrow();

        assertThat(reenabledOccurrence).isNotEqualTo(oldOccurrence);
        Outcome staleGeneration = invoke(
                facade,
                TaskKind.REMINDER,
                oldOccurrence,
                new TaskActionRequest(TaskAction.COMPLETE, SECOND_REQUEST_ID, null));
        assertThat(staleGeneration.response()).isNull();
        assertThat(staleGeneration.error()).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCode())
                        .isIn("TASK_NOT_FOUND", "TASK_ALREADY_TERMINAL"));
        assertThat(jdbcTemplate.queryForObject(
                "select status from care_tasks where task_id=?",
                String.class, REMINDER_DEFINITION)).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from checklist_action_commands
                 where actor_user_id=? and task_kind='REMINDER'
                   and reminder_definition_id=?
                """, Long.class, ACTOR, REMINDER_DEFINITION)).isEqualTo(1L);

        Outcome second = invoke(facade, TaskKind.REMINDER, reenabledOccurrence, request);
        assertThat(second.error()).isNull();
        assertThat(second.response().idempotentReplay()).isFalse();
        assertThat(jdbcTemplate.queryForObject("""
                select count(distinct task_id) from checklist_action_commands
                 where actor_user_id=? and task_kind='REMINDER'
                   and reminder_definition_id=?
                """, Long.class, ACTOR, REMINDER_DEFINITION)).isEqualTo(2L);
    }

    @Test
    void membershipRevokedBetweenAuthorizeAndLockedReauthorizeDeniesWithoutMutation() {
        Instant schedule = jdbcTemplate.queryForObject(
                "select scheduled_at from care_tasks where task_id=?",
                (resultSet, row) -> resultSet.getTimestamp(1).toInstant(), REMINDER_DEFINITION);
        UUID occurrence = ReminderOccurrenceIdFactory.create(REMINDER_DEFINITION, schedule);
        DataSource provisionerDataSource = POSTGRES.getPostgresDatabase();
        Runnable revokeMembership = () -> new TransactionTemplate(
                new DataSourceTransactionManager(provisionerDataSource))
                .executeWithoutResult(status -> new JdbcTemplate(provisionerDataSource).update("""
                        update care_group_members
                           set invitation_status='REVOKED', updated_at=now()
                         where care_group_id=? and user_id=?
                        """, CARE_GROUP, FAMILY));
        UnifiedTaskActionFacade facade = facade(new RevokingDelegatingHandler(
                reminderTaskActionHandler, revokeMembership));

        Outcome outcome = invoke(
                facade,
                FAMILY,
                TaskKind.REMINDER,
                occurrence,
                new TaskActionRequest(TaskAction.COMPLETE, SECOND_REQUEST_ID, null));

        assertThat(outcome.response()).isNull();
        assertThat(outcome.error()).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
        assertThat(jdbcTemplate.queryForObject(
                "select status from care_tasks where task_id=?",
                String.class, REMINDER_DEFINITION)).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from checklist_action_commands
                 where actor_user_id=? and task_kind='REMINDER'
                   and reminder_definition_id=?
                """, Long.class, FAMILY, REMINDER_DEFINITION)).isZero();
    }

    private void assertDifferentRequestIdsSerializeTerminalMutation(
            TaskKind kind, UUID taskId, UUID instanceId) throws Exception {
        TerminalBarrierHandler handler = new TerminalBarrierHandler(kind, taskId, instanceId);
        UnifiedTaskActionFacade facade = facade(handler);
        List<Outcome> outcomes = invokeConcurrently(
                facade, kind, taskId,
                new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null),
                new TaskActionRequest(TaskAction.COMPLETE, SECOND_REQUEST_ID, null));

        assertThat(handler.mutationCount()).isEqualTo(1);
        assertThat(outcomes.stream().filter(outcome -> outcome.response() != null)).hasSize(1);
        assertThat(outcomes.stream().map(Outcome::error).filter(BusinessException.class::isInstance)
                .map(BusinessException.class::cast).map(BusinessException::getCode))
                .containsExactly("TASK_ALREADY_TERMINAL");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from checklist_action_commands
                where actor_user_id = ? and task_kind = ? and task_id = ?
                """, Long.class, ACTOR, kind.name(), taskId)).isEqualTo(1L);
    }

    private UnifiedTaskActionFacade facade(TaskActionHandler handler) {
        return new UnifiedTaskActionFacade(
                List.of(handler), commandRepository, objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private List<Outcome> invokeConcurrently(
            UnifiedTaskActionFacade facade,
            TaskActionRequest firstRequest,
            TaskActionRequest secondRequest) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> invoke(facade, firstRequest));
            var second = executor.submit(() -> invoke(facade, secondRequest));
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }

    private List<Outcome> invokeConcurrently(
            UnifiedTaskActionFacade facade,
            UUID firstTaskId,
            TaskActionRequest firstRequest,
            UUID secondTaskId,
            TaskActionRequest secondRequest) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> invoke(
                    facade, TaskKind.REMINDER, firstTaskId, firstRequest));
            var second = executor.submit(() -> invoke(
                    facade, TaskKind.REMINDER, secondTaskId, secondRequest));
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }

    private List<Outcome> invokeConcurrently(
            UnifiedTaskActionFacade facade,
            TaskKind kind,
            UUID taskId,
            TaskActionRequest firstRequest,
            TaskActionRequest secondRequest) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> invoke(facade, kind, taskId, firstRequest));
            var second = executor.submit(() -> invoke(facade, kind, taskId, secondRequest));
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }

    private Outcome invoke(UnifiedTaskActionFacade facade, TaskActionRequest request) {
        return invoke(facade, TaskKind.CARE_TASK, TASK, request);
    }

    private Outcome invoke(
            UnifiedTaskActionFacade facade,
            TaskKind kind,
            UUID taskId,
            TaskActionRequest request) {
        return invoke(facade, ACTOR, kind, taskId, request);
    }

    private Outcome invoke(
            UnifiedTaskActionFacade facade,
            UUID actorUserId,
            TaskKind kind,
            UUID taskId,
            TaskActionRequest request) {
        try {
            TaskActionResponse response = new TransactionTemplate(transactionManager).execute(
                    status -> facade.apply(actorUserId, kind, taskId, request));
            return new Outcome(response, null);
        } catch (Throwable error) {
            return new Outcome(null, error);
        }
    }

    private long commandCount() {
        return jdbcTemplate.queryForObject("""
                select count(*) from checklist_action_commands
                where actor_user_id = ? and task_kind = 'CARE_TASK'
                  and task_id = ? and client_request_id = ?
                """, Long.class, ACTOR, TASK, REQUEST_ID);
    }

    private void cleanFixtures() {
        DataSource provisionerDataSource = POSTGRES.getPostgresDatabase();
        JdbcTemplate provisionerJdbcTemplate = new JdbcTemplate(provisionerDataSource);

        // Test-only cleanup uses the embedded provisioner boundary. SET LOCAL disables user
        // triggers only for this transaction and resets automatically when cleanup completes.
        new TransactionTemplate(new DataSourceTransactionManager(provisionerDataSource))
                .executeWithoutResult(status -> {
                    provisionerJdbcTemplate.execute("set local session_replication_role = replica");
                    provisionerJdbcTemplate.update(
                            "delete from checklist_action_commands where actor_user_id in (?, ?)",
                            ACTOR, FAMILY);
                    provisionerJdbcTemplate.update(
                            """
                            delete from audit_events
                             where actor_user_id in (?, ?)
                                or subject_user_id in (?, ?)
                            """,
                            ACTOR, FAMILY, ACTOR, FAMILY);
                    provisionerJdbcTemplate.update(
                            "delete from reminder_occurrence_aliases where reminder_definition_id = ?",
                            REMINDER_DEFINITION);
                    provisionerJdbcTemplate.update(
                            "delete from care_tasks where task_id in (?, ?)", TASK, REMINDER_DEFINITION);
                    provisionerJdbcTemplate.update(
                            "delete from care_group_members where care_group_id = ?", CARE_GROUP);
                    provisionerJdbcTemplate.update(
                            "delete from care_groups where care_group_id = ?", CARE_GROUP);
                    provisionerJdbcTemplate.update(
                            "delete from mother_journeys where journey_id = ?", JOURNEY);
                    provisionerJdbcTemplate.update(
                            "delete from care_subjects where care_subject_id = ?", CARE_SUBJECT);
                    provisionerJdbcTemplate.update(
                            "delete from users where user_id in (?, ?)", ACTOR, FAMILY);
                });
    }

    private record Outcome(TaskActionResponse response, Throwable error) {
    }

    private static final class BarrierHandler implements TaskActionHandler {
        private final CyclicBarrier authorizationBarrier = new CyclicBarrier(2);
        private final CountDownLatch mutationBarrier = new CountDownLatch(2);
        private final AtomicInteger authorizationCount = new AtomicInteger();
        private final AtomicInteger mutationCount = new AtomicInteger();

        @Override
        public TaskKind taskKind() {
            return TaskKind.CARE_TASK;
        }

        @Override
        public AuthorizedTask authorize(UUID actorUserId, UUID taskId) {
            int attempt = authorizationCount.incrementAndGet();
            if (attempt <= 2) {
                await(authorizationBarrier);
            }
            return new AuthorizedTask(TaskKind.CARE_TASK, taskId, null, "PENDING",
                    Set.of(TaskAction.COMPLETE, TaskAction.SKIP));
        }

        @Override
        public TaskActionResponse apply(
                AuthorizedTask task,
                UUID actorUserId,
                TaskAction action,
                String reason,
                Instant appliedAt,
                UUID correlationId) {
            mutationCount.incrementAndGet();
            mutationBarrier.countDown();
            await(mutationBarrier);
            return new TaskActionResponse(TaskKind.CARE_TASK, task.taskId(), null, action,
                    "PENDING", action == TaskAction.COMPLETE ? "COMPLETED" : "SKIPPED",
                    appliedAt, false, correlationId);
        }

        private int authorizationCount() {
            return authorizationCount.get();
        }

        private int mutationCount() {
            return mutationCount.get();
        }

        private static void await(CyclicBarrier barrier) {
            try {
                barrier.await(5, TimeUnit.SECONDS);
            } catch (Exception exception) {
                throw new IllegalStateException("Concurrent authorization barrier failed", exception);
            }
        }

        private static void await(CountDownLatch latch) {
            try {
                latch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Concurrent mutation barrier interrupted", exception);
            }
        }
    }

    private static final class TerminalBarrierHandler implements TaskActionHandler {
        private final TaskKind kind;
        private final UUID taskId;
        private final UUID instanceId;
        private final CyclicBarrier initialAuthorizationBarrier = new CyclicBarrier(2);
        private final CountDownLatch mutationBarrier = new CountDownLatch(2);
        private final AtomicInteger authorizationCount = new AtomicInteger();
        private final AtomicInteger mutationCount = new AtomicInteger();
        private final AtomicBoolean terminal = new AtomicBoolean();

        private TerminalBarrierHandler(TaskKind kind, UUID taskId, UUID instanceId) {
            this.kind = kind;
            this.taskId = taskId;
            this.instanceId = instanceId;
        }

        @Override
        public TaskKind taskKind() {
            return kind;
        }

        @Override
        public AuthorizedTask authorize(UUID actorUserId, UUID requestedTaskId) {
            int attempt = authorizationCount.incrementAndGet();
            if (attempt <= 2) {
                BarrierHandler.await(initialAuthorizationBarrier);
            }
            boolean alreadyTerminal = terminal.get();
            return new AuthorizedTask(kind, taskId, instanceId,
                    alreadyTerminal ? "COMPLETED" : "PENDING",
                    alreadyTerminal ? Set.of() : Set.of(TaskAction.COMPLETE));
        }

        @Override
        public TaskActionResponse apply(
                AuthorizedTask task,
                UUID actorUserId,
                TaskAction action,
                String reason,
                Instant appliedAt,
                UUID correlationId) {
            mutationCount.incrementAndGet();
            mutationBarrier.countDown();
            BarrierHandler.await(mutationBarrier);
            terminal.set(true);
            return new TaskActionResponse(kind, task.taskId(), instanceId, action,
                    "PENDING", "COMPLETED", appliedAt, false, correlationId);
        }

        private int mutationCount() {
            return mutationCount.get();
        }
    }

    private static final class BarrierDelegatingHandler implements TaskActionHandler {
        private final TaskActionHandler delegate;
        private final CyclicBarrier initialAuthorizationBarrier = new CyclicBarrier(2);
        private final AtomicInteger authorizationCount = new AtomicInteger();
        private final AtomicInteger mutationCount = new AtomicInteger();

        private BarrierDelegatingHandler(TaskActionHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public TaskKind taskKind() {
            return TaskKind.REMINDER;
        }

        @Override
        public AuthorizedTask authorize(UUID actorUserId, UUID taskId) {
            if (authorizationCount.incrementAndGet() <= 2) {
                BarrierHandler.await(initialAuthorizationBarrier);
            }
            return delegate.authorize(actorUserId, taskId);
        }

        @Override
        public AuthorizedTask authorizeReplay(
                UUID actorUserId, UUID taskId, UUID instanceId) {
            return delegate.authorizeReplay(actorUserId, taskId, instanceId);
        }

        @Override
        public UUID actionScopeId(AuthorizedTask task) {
            return delegate.actionScopeId(task);
        }

        @Override
        public AuthorizedTask authorizeForUpdate(UUID actorUserId, AuthorizedTask task) {
            return delegate.authorizeForUpdate(actorUserId, task);
        }

        @Override
        public TaskActionResponse apply(
                AuthorizedTask task,
                UUID actorUserId,
                TaskAction action,
                String reason,
                Instant appliedAt,
                UUID correlationId) {
            mutationCount.incrementAndGet();
            return delegate.apply(task, actorUserId, action, reason, appliedAt, correlationId);
        }

        private int mutationCount() {
            return mutationCount.get();
        }
    }

    private static final class RevokingDelegatingHandler implements TaskActionHandler {
        private final TaskActionHandler delegate;
        private final Runnable revokeMembership;
        private final AtomicBoolean revoked = new AtomicBoolean();

        private RevokingDelegatingHandler(
                TaskActionHandler delegate,
                Runnable revokeMembership) {
            this.delegate = delegate;
            this.revokeMembership = revokeMembership;
        }

        @Override
        public TaskKind taskKind() {
            return delegate.taskKind();
        }

        @Override
        public AuthorizedTask authorize(UUID actorUserId, UUID taskId) {
            return delegate.authorize(actorUserId, taskId);
        }

        @Override
        public UUID actionScopeId(AuthorizedTask task) {
            if (revoked.compareAndSet(false, true)) {
                revokeMembership.run();
            }
            return delegate.actionScopeId(task);
        }

        @Override
        public AuthorizedTask authorizeForUpdate(UUID actorUserId, AuthorizedTask task) {
            return delegate.authorizeForUpdate(actorUserId, task);
        }

        @Override
        public TaskActionResponse apply(
                AuthorizedTask task,
                UUID actorUserId,
                TaskAction action,
                String reason,
                Instant appliedAt,
                UUID correlationId) {
            return delegate.apply(task, actorUserId, action, reason, appliedAt, correlationId);
        }
    }
}
