package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real PostgreSQL serialization evidence for CHK-011 lifecycle correction races. */
class ChecklistLifecycleCorrectionConcurrencyPostgresTest
        extends AbstractEmbeddedPostgresIntegrationTest {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate OLD_ANCHOR = LocalDate.now(DEFAULT_ZONE).minusDays(1);
    private static final LocalDate CORRECTED_ANCHOR = OLD_ANCHOR.plusDays(1);
    private static final int WAIT_SECONDS = 15;

    @Autowired private ChecklistDistributionService distributionService;
    @Autowired private UnifiedTaskActionFacade actionFacade;
    @Autowired private ChecklistInstanceRepository instanceRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private CareGroupRepository careGroupRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    private UUID mother;
    private UUID journey;
    private UUID careGroup;
    private UUID templateLineage;
    private UUID templateVersion;
    private UUID templateItem;
    private ChecklistLifecycleEligibilityValue substage;

    @BeforeEach
    void seedCanonicalMotherContext() {
        mother = UUID.randomUUID();
        templateLineage = UUID.randomUUID();
        templateVersion = UUID.randomUUID();
        templateItem = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, mother, "CHK-011 Mother", uniquePhone(mother), "MOTHER");

        UUID subject = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                select ?, u.person_id, u.user_id, 'MOTHER', u.display_name, 'ACTIVE', now(), now()
                  from users u where u.user_id = ?
                """, subject, mother);
        journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(mother)
                .careSubjectId(subject)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(OLD_ANCHOR)
                .lastMenstrualDate(OLD_ANCHOR)
                .estimatedDueDate(OLD_ANCHOR.plusWeeks(40))
                .build()).getId();
        jdbcTemplate.update(
                "update care_subjects set mother_journey_id = ? where care_subject_id = ?",
                journey, subject);
        careGroup = careGroupRepository.saveAndFlush(CareGroup.builder()
                .ownerUserId(mother)
                .groupName("CHK-011 concurrency group")
                .status(CareGroupStatus.ACTIVE)
                .linkedJourneyId(journey)
                .build()).getId();
        seedTemplateVersion();
        substage = ChecklistLifecycleEligibilityValue.builder()
                .stage(ContentStage.PREGNANCY.name())
                .anchorType(ChecklistAnchorType.LMP)
                .rangeUnit(ChecklistRangeUnit.WEEK)
                .startInclusive(0)
                .endInclusive(12)
                .active(true)
                .build();
    }

    @Test
    void chk011_twoCorrectionsCreateOneReplacementAndNoDuplicateTypedAudit() throws Exception {
        SeededOccurrence old = seedPendingOccurrence();
        UUID correctionCorrelation = UUID.randomUUID();
        ChecklistDistributionCommand correction = command(CORRECTED_ANCHOR, correctionCorrelation);
        CyclicBarrier startTogether = new CyclicBarrier(2);

        List<ChecklistDistributionResult> outcomes;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> inTransaction(() -> {
                await(startTogether);
                return distributionService.distribute(correction);
            }));
            var second = executor.submit(() -> inTransaction(() -> {
                await(startTogether);
                return distributionService.distribute(correction);
            }));
            outcomes = List.of(
                    first.get(WAIT_SECONDS, TimeUnit.SECONDS),
                    second.get(WAIT_SECONDS, TimeUnit.SECONDS));
        }

        assertThat(outcomes).extracting(ChecklistDistributionResult::createdInstances)
                .containsExactlyInAnyOrder(0, 1);
        assertThat(outcomes).extracting(ChecklistDistributionResult::cancelledInstances)
                .containsExactlyInAnyOrder(0, 1);
        assertThat(parentCount()).isEqualTo(2L);
        assertThat(taskCount()).isEqualTo(2L);
        assertHistorical(old.parentId(), "PENDING", 1L);
        assertTask(old.taskId(), "PENDING", 0L);
        assertCorrectedOccurrence("PENDING", 0L, "PENDING", 0L);

        assertTypedParentAuditCount(correctionCorrelation, "CHECKLIST_DISTRIBUTED", 1L);
        UUID replacementTask = correctedTaskId();
        assertTypedAuditCount(correctionCorrelation, "CHECKLIST_ASSIGNED", replacementTask, 1L);
        assertThat(cancelAuditCount(correctionCorrelation)).isZero();
        assertThat(auditCount(correctionCorrelation)).isEqualTo(2L);
        assertThat(templateActionCommandCount()).isZero();
    }

    @Test
    void chk011_actionWinningScopeLockPreservesTerminalHistoryAndCreatesReplacement()
            throws Exception {
        SeededOccurrence old = seedPendingOccurrence();
        UUID correctionCorrelation = UUID.randomUUID();
        ChecklistDistributionCommand correction = command(CORRECTED_ANCHOR, correctionCorrelation);
        CountDownLatch actionHasScopeLock = new CountDownLatch(1);
        CountDownLatch correctionTransactionStarted = new CountDownLatch(1);
        CountDownLatch releaseAction = new CountDownLatch(1);

        TaskActionResponse action;
        ChecklistDistributionResult corrected;
        AdvisoryLockSnapshot contention;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var actionFuture = executor.submit(() -> inTransaction(() -> {
                instanceRepository.acquireDistributionKeyLock(lifecycleScope());
                actionHasScopeLock.countDown();
                await(releaseAction);
                return actionFacade.apply(mother, TaskKind.CHECKLIST, old.taskId(),
                        new TaskActionRequest(TaskAction.COMPLETE, UUID.randomUUID(), null));
            }));
            var correctionFuture = executor.submit(() -> {
                await(actionHasScopeLock);
                return inTransaction(() -> {
                    correctionTransactionStarted.countDown();
                    return distributionService.distribute(correction);
                });
            });
            await(correctionTransactionStarted);
            try {
                contention = awaitScopeContention(lifecycleScope());
            } finally {
                releaseAction.countDown();
            }
            action = actionFuture.get(WAIT_SECONDS, TimeUnit.SECONDS);
            corrected = correctionFuture.get(WAIT_SECONDS, TimeUnit.SECONDS);
        }

        assertThat(contention.granted()).isEqualTo(1L);
        assertThat(contention.waiting()).isEqualTo(1L);
        assertThat(action.status()).isEqualTo("COMPLETED");
        assertThat(action.taskId()).isEqualTo(old.taskId());
        assertThat(action.instanceId()).isEqualTo(old.parentId());
        assertThat(corrected.createdInstances()).isEqualTo(1);
        assertThat(corrected.cancelledInstances()).isEqualTo(1);
        assertThat(parentCount()).isEqualTo(2L);
        assertThat(taskCount()).isEqualTo(2L);
        assertHistorical(old.parentId(), "COMPLETED", 2L);
        assertTask(old.taskId(), "COMPLETED", 1L);
        assertCorrectedOccurrence("PENDING", 0L, "PENDING", 0L);
        assertTypedAuditCount(action.correlationId(), "CHECKLIST_COMPLETED", old.taskId(), 1L);
        assertTypedParentAuditCount(correctionCorrelation, "CHECKLIST_DISTRIBUTED", 1L);
        assertTypedAuditCount(
                correctionCorrelation, "CHECKLIST_ASSIGNED", correctedTaskId(), 1L);
        assertThat(cancelAuditCount(correctionCorrelation)).isZero();
        assertThat(auditCount(action.correlationId())).isEqualTo(1L);
        assertThat(auditCount(correctionCorrelation)).isEqualTo(2L);
        assertThat(actionCommandCount(old.taskId())).isEqualTo(1L);
        assertThat(templateActionCommandCount()).isEqualTo(1L);
    }

    @Test
    void chk011_correctionWinningRaceRefreshesParentAfterScopeLockAndHidesCancelledTask()
            throws Exception {
        SeededOccurrence old = seedPendingOccurrence();
        UUID correctionCorrelation = UUID.randomUUID();
        ChecklistDistributionCommand correction = command(CORRECTED_ANCHOR, correctionCorrelation);
        CountDownLatch parentPreloaded = new CountDownLatch(1);
        CountDownLatch correctionCommitted = new CountDownLatch(1);

        ActionOutcome action;
        ChecklistDistributionResult corrected;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var actionFuture = executor.submit(() -> {
                try {
                    TaskActionResponse response = inTransaction(() -> {
                        ChecklistInstance cached = entityManager.find(ChecklistInstance.class, old.parentId());
                        assertThat(cached.getStatus().name()).isEqualTo("PENDING");
                        parentPreloaded.countDown();
                        await(correctionCommitted);
                        return actionFacade.apply(mother, TaskKind.CHECKLIST, old.taskId(),
                                new TaskActionRequest(TaskAction.COMPLETE, UUID.randomUUID(), null));
                    });
                    return new ActionOutcome(response, null);
                } catch (Throwable error) {
                    return new ActionOutcome(null, error);
                }
            });
            var correctionFuture = executor.submit(() -> {
                await(parentPreloaded);
                try {
                    return inTransaction(() -> distributionService.distribute(correction));
                } finally {
                    correctionCommitted.countDown();
                }
            });
            corrected = correctionFuture.get(WAIT_SECONDS, TimeUnit.SECONDS);
            action = actionFuture.get(WAIT_SECONDS, TimeUnit.SECONDS);
        }

        assertThat(corrected.createdInstances()).isEqualTo(1);
        assertThat(corrected.cancelledInstances()).isEqualTo(1);
        assertThat(action.response()).isNull();
        assertThat(action.error()).isInstanceOf(BusinessException.class);
        assertThat(((BusinessException) action.error()).getCode()).isEqualTo("TASK_NOT_FOUND");
        assertHistorical(old.parentId(), "PENDING", 1L);
        assertTask(old.taskId(), "PENDING", 0L);
        assertCorrectedOccurrence("PENDING", 0L, "PENDING", 0L);
        assertTypedParentAuditCount(correctionCorrelation, "CHECKLIST_DISTRIBUTED", 1L);
        assertTypedAuditCount(
                correctionCorrelation, "CHECKLIST_ASSIGNED", correctedTaskId(), 1L);
        assertThat(cancelAuditCount(correctionCorrelation)).isZero();
        assertThat(auditCount(correctionCorrelation)).isEqualTo(2L);
        assertThat(completedAuditCount(old.taskId())).isZero();
        assertThat(actionCommandCount(old.taskId())).isZero();
        assertThat(templateActionCommandCount()).isZero();
    }

    @Test
    void chk011_actionLedgerHistoryMakesPendingOccurrenceIneligibleForCancellation() {
        SeededOccurrence old = seedPendingOccurrence();
        jdbcTemplate.update("""
                insert into checklist_action_commands (
                    checklist_action_command_id, actor_user_id, task_kind, task_id,
                    client_request_id, payload_hash, action_type, result_status,
                    result_jsonb, applied_at, retain_until, legal_hold, created_at)
                values (?, ?, 'CHECKLIST', ?, ?, ?, 'COMPLETE', 'APPLIED',
                        '{}'::jsonb, now(), now() + interval '8 years', false, now())
                """, UUID.randomUUID(), mother, old.taskId(), UUID.randomUUID(), "c".repeat(64));

        ChecklistDistributionResult corrected = inTransaction(
                () -> distributionService.distribute(command(CORRECTED_ANCHOR, UUID.randomUUID())));

        assertThat(corrected.cancelledInstances()).isEqualTo(1);
        assertHistorical(old.parentId(), "PENDING", 1L);
        assertThat(jdbcTemplate.queryForObject(
                "select status from checklist_task_instances where checklist_task_instance_id=?",
                String.class, old.taskId())).isEqualTo("PENDING");
        assertThat(actionCommandCount(old.taskId())).isOne();
    }

    private SeededOccurrence seedPendingOccurrence() {
        UUID seedCorrelation = UUID.randomUUID();
        ChecklistDistributionResult seeded = inTransaction(
                () -> distributionService.distribute(command(OLD_ANCHOR, seedCorrelation)));
        assertThat(seeded.createdInstances()).isEqualTo(1);
        assertThat(seeded.createdTasks()).isEqualTo(1);
        UUID parentId = jdbcTemplate.queryForObject("""
                select checklist_instance_id from checklist_instances
                 where template_version_id = ? and recipient_user_id = ? and window_start = ?
                """, UUID.class, templateVersion, mother, OLD_ANCHOR);
        UUID taskId = jdbcTemplate.queryForObject("""
                select checklist_task_instance_id from checklist_task_instances
                 where checklist_instance_id = ?
                """, UUID.class, parentId);
        return new SeededOccurrence(parentId, taskId);
    }

    private ChecklistDistributionCommand command(LocalDate anchor, UUID correlationId) {
        return new ChecklistDistributionCommand(
                templateLineage,
                templateVersion,
                null,
                mother,
                ChecklistCareContextType.JOURNEY,
                journey,
                mother,
                ContentStage.PREGNANCY,
                substage,
                new ChecklistLifecycleDates(anchor, anchor.plusWeeks(40), null, null),
                anchor,
                ZoneId.of("UTC"),
                List.of(new ChecklistDistributionRecipient(
                        mother, ChecklistRecipientRole.MOTHER, true, true, true)),
                List.of(new ChecklistDistributionItem(
                        templateItem, "CHK-011 lifecycle task", 1, true,
                        ChecklistTargetSubject.MOTHER, ChecklistAnchorType.LMP, 0)),
                 correlationId,
                 1L);
    }

    private void seedTemplateVersion() {
        jdbcTemplate.update("""
                insert into care_item_templates (
                    template_id, entry_type, title, description, stage, is_active,
                    version, content_status, template_lineage_id, template_version_id,
                    recipient_scope, eligibility_anchor_type, eligibility_range_unit,
                    eligibility_start_inclusive, eligibility_end_inclusive,
                    migration_review_required, distribution_enabled,
                    approved_at, approved_by, created_at, updated_at)
                values (?, 'TEMPLATE_ROOT', 'CHK-011 PostgreSQL template', 'concurrency fixture',
                    'PREGNANCY', true, 1, 'DRAFT', ?, ?,
                    'MOTHER', 'LMP', 'WEEK', 0, 12,
                    false, false, null, null, now(), now())
                """, templateVersion, templateLineage, templateVersion);
        jdbcTemplate.update("""
                insert into care_item_templates (
                    template_id, parent_template_id, entry_type, title, display_order,
                    stage, is_active, content_status, target_subject, is_required,
                    created_at, updated_at)
                values (?, ?, 'CHECKLIST_ENTRY', 'CHK-011 lifecycle task', 1,
                    'PREGNANCY', true, 'APPROVED', 'MOTHER', true, now(), now())
                """, templateItem, templateVersion);
        jdbcTemplate.update("""
                update care_item_templates
                   set content_status = 'APPROVED', distribution_enabled = true,
                       approved_at = now(), approved_by = ?
                 where template_id = ?
                """, mother, templateVersion);
    }

    private String lifecycleScope() {
        return ChecklistDistributionKeyFactory.lifecycleScopeKey(
                templateVersion, mother, ChecklistRecipientRole.MOTHER.name(), null,
                ChecklistCareContextType.JOURNEY.name(), journey);
    }

    private long parentCount() {
        return jdbcTemplate.queryForObject("""
                select count(*) from checklist_instances
                 where template_version_id = ? and recipient_user_id = ?
                """, Long.class, templateVersion, mother);
    }

    private long taskCount() {
        return jdbcTemplate.queryForObject("""
                select count(*) from checklist_task_instances task
                  join checklist_instances parent
                    on parent.checklist_instance_id = task.checklist_instance_id
                 where parent.template_version_id = ? and parent.recipient_user_id = ?
                """, Long.class, templateVersion, mother);
    }

    private void assertParent(UUID parentId, String status, long version) {
        assertThat(jdbcTemplate.queryForMap("""
                select status, lock_version from checklist_instances
                 where checklist_instance_id = ?
                """, parentId))
                .containsEntry("status", status)
                .containsEntry("lock_version", version);
    }

    private void assertHistorical(UUID parentId, String status, long version) {
        assertThat(jdbcTemplate.queryForMap("""
                select status, historical_at, history_reason_code, lock_version
                  from checklist_instances
                 where checklist_instance_id = ?
                """, parentId))
                .containsEntry("status", status)
                .containsEntry("history_reason_code", "LIFECYCLE_STAGE_OBSOLETE")
                .containsEntry("lock_version", version);
        assertThat(jdbcTemplate.queryForObject("""
                select historical_at is not null
                  from checklist_instances
                 where checklist_instance_id = ?
                """, Boolean.class, parentId)).isTrue();
    }

    private void assertTask(UUID taskId, String status, long version) {
        assertThat(jdbcTemplate.queryForMap("""
                select status, lock_version from checklist_task_instances
                 where checklist_task_instance_id = ?
                """, taskId))
                .containsEntry("status", status)
                .containsEntry("lock_version", version);
    }

    private void assertCorrectedOccurrence(
            String parentStatus, long parentVersion, String taskStatus, long taskVersion) {
        UUID parentId = correctedParentId();
        assertParent(parentId, parentStatus, parentVersion);
        assertTask(correctedTaskId(), taskStatus, taskVersion);
    }

    private UUID correctedParentId() {
        return jdbcTemplate.queryForObject("""
                select checklist_instance_id from checklist_instances
                 where template_version_id = ? and recipient_user_id = ? and window_start = ?
                """, UUID.class, templateVersion, mother, CORRECTED_ANCHOR);
    }

    private UUID correctedTaskId() {
        return jdbcTemplate.queryForObject("""
                select checklist_task_instance_id from checklist_task_instances
                 where checklist_instance_id = ?
                """, UUID.class, correctedParentId());
    }

    private void assertTypedAuditCount(
            UUID correlationId, String action, UUID taskId, long expected) {
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from audit_events
                 where correlation_id = ? and event_category = ?
                   and checklist_task_instance_id = ? and event_origin = 'AUDIT_LOG'
                """, Long.class, correlationId, action, taskId)).isEqualTo(expected);
    }

    private void assertTypedParentAuditCount(UUID correlationId, String action, long expected) {
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from audit_events
                 where correlation_id = ? and event_category = ?
                   and checklist_task_instance_id is null and event_origin = 'AUDIT_LOG'
                """, Long.class, correlationId, action)).isEqualTo(expected);
    }

    private long auditCount(UUID correlationId) {
        return jdbcTemplate.queryForObject("""
                select count(*) from audit_events
                 where correlation_id = ? and event_origin = 'AUDIT_LOG'
                """, Long.class, correlationId);
    }

    private long cancelAuditCount(UUID correlationId) {
        return jdbcTemplate.queryForObject("""
                select count(*) from audit_events
                 where correlation_id = ? and event_category = 'CHECKLIST_CANCELLED'
                   and event_origin = 'AUDIT_LOG'
                """, Long.class, correlationId);
    }

    private long completedAuditCount(UUID taskId) {
        return jdbcTemplate.queryForObject("""
                select count(*) from audit_events
                 where checklist_task_instance_id = ?
                   and event_category = 'CHECKLIST_COMPLETED' and event_origin = 'AUDIT_LOG'
                """, Long.class, taskId);
    }

    private long actionCommandCount(UUID taskId) {
        return jdbcTemplate.queryForObject("""
                select count(*) from checklist_action_commands
                 where task_kind = 'CHECKLIST' and task_id = ?
                """, Long.class, taskId);
    }

    private long templateActionCommandCount() {
        return jdbcTemplate.queryForObject("""
                select count(*) from checklist_action_commands command
                  join checklist_task_instances task
                    on task.checklist_task_instance_id = command.task_id
                  join checklist_instances parent
                    on parent.checklist_instance_id = task.checklist_instance_id
                 where parent.template_version_id = ? and parent.recipient_user_id = ?
                """, Long.class, templateVersion, mother);
    }

    private AdvisoryLockSnapshot awaitScopeContention(String scope) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        AdvisoryLockSnapshot snapshot;
        do {
            snapshot = advisoryLockSnapshot(scope);
            if (snapshot.granted() == 1L && snapshot.waiting() == 1L) {
                return snapshot;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while observing CHK-011 advisory lock", exception);
            }
        } while (System.nanoTime() < deadline);
        return snapshot;
    }

    private AdvisoryLockSnapshot advisoryLockSnapshot(String scope) {
        return jdbcTemplate.queryForObject("""
                with expected as (
                    select hashtextextended(cast(? as text), 0) as scope_key
                )
                select count(*) filter (where locks.granted) as granted_count,
                       count(*) filter (where not locks.granted) as waiting_count
                  from pg_locks locks cross join expected
                 where locks.locktype = 'advisory'
                   and locks.classid::bigint = ((expected.scope_key >> 32) & 4294967295)
                   and locks.objid::bigint = (expected.scope_key & 4294967295)
                   and locks.objsubid = 1
                """, (result, row) -> new AdvisoryLockSnapshot(
                        result.getLong("granted_count"), result.getLong("waiting_count")), scope);
    }

    private <T> T inTransaction(CheckedSupplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            try {
                return work.get();
            } catch (RuntimeException runtime) {
                throw runtime;
            } catch (Exception checked) {
                throw new IllegalStateException(checked);
            }
        });
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Timed out at CHK-011 concurrency barrier", exception);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out at CHK-011 concurrency latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted at CHK-011 concurrency latch", exception);
        }
    }

    private static String uniquePhone(UUID userId) {
        return "09" + String.format("%08d", Math.floorMod(userId.getLeastSignificantBits(), 100_000_000L));
    }

    private record SeededOccurrence(UUID parentId, UUID taskId) {
    }

    private record ActionOutcome(TaskActionResponse response, Throwable error) {
    }

    private record AdvisoryLockSnapshot(long granted, long waiting) {
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
