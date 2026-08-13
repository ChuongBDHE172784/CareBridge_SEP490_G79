package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistMaterializationMode;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.repository.ChecklistActionCommandRepository;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.content.entity.ContentStage;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ChecklistDistributionServiceTest {

    private final ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
    private final ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
    private final ChecklistActionCommandRepository commands = mock(ChecklistActionCommandRepository.class);
    private final ChecklistAuditWriter audit = mock(ChecklistAuditWriter.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-29T03:00:00Z"), ZoneOffset.UTC);
    private ChecklistDistributionService service;

    @BeforeEach
    void setUp() {
        service = new ChecklistDistributionService(
                instances, tasks, commands, audit, new ChecklistLifecycleEligibilityService(), clock);
        var ids = new AtomicInteger(100);
        when(instances.save(any())).thenAnswer(invocation -> {
            ChecklistInstance value = invocation.getArgument(0);
            if (value.getId() == null) value.setId(new UUID(0, ids.getAndIncrement()));
            return value;
        });
        when(tasks.save(any())).thenAnswer(invocation -> {
            ChecklistTaskInstance value = invocation.getArgument(0);
            if (value.getId() == null) value.setId(new UUID(0, ids.getAndIncrement()));
            return value;
        });
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(any())).thenReturn(List.of());
    }

    @Test
    void motherAndEachEligibleFamilyRecipientReceiveSeparateIdempotentInstancesAndChildren() {
        var command = command(
                ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(
                        new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                                ChecklistRecipientRole.MOTHER, true, true, true),
                        new ChecklistDistributionRecipient(UUID.fromString("77777777-7777-7777-7777-777777777777"),
                                ChecklistRecipientRole.FAMILY, true, true, true),
                        new ChecklistDistributionRecipient(UUID.fromString("88888888-8888-8888-8888-888888888888"),
                                ChecklistRecipientRole.FAMILY, true, false, true)),
                ChecklistDistributionTestFactory.CARE_GROUP_ID);
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
        when(tasks.findByTaskKey(any())).thenReturn(Optional.empty());
        when(instances.findAllByRecipientUserIdAndRecipientRoleAndCareGroupIdAndCareContextTypeAndCareContextIdAndTemplateVersionId(
                any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        var result = service.distribute(command);

        assertThat(result.createdInstances()).isEqualTo(2);
        assertThat(result.createdTasks()).isEqualTo(4);
        assertThat(result.deniedRecipients()).isEqualTo(1);
        verify(instances, times(2)).save(any());
        verify(instances).save(argThat(instance -> instance.getRecipientRole() == ChecklistRecipientRole.MOTHER
                && instance.getCareGroupId() == null));
        verify(instances).save(argThat(instance -> instance.getRecipientRole() == ChecklistRecipientRole.FAMILY
                && ChecklistDistributionTestFactory.CARE_GROUP_ID.equals(instance.getCareGroupId())));
        verify(tasks, times(4)).save(any());
    }

    @Test
    void ownerMismatchIsAuditedAndNeverDistributed() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                ChecklistDistributionTestFactory.CARE_GROUP_ID);

        var result = service.distribute(command);

        assertThat(result.conflicts()).isEqualTo(1);
        verify(audit).write(any());
        verify(instances, never()).save(any());
    }

    @Test
    void personalMotherDistributionPersistsWithoutCareGroup() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                null);
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
        when(instances.findAllByLogicalPersonalIdentity(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(tasks.findByTaskKey(any())).thenReturn(Optional.empty());

        var result = service.distribute(command);

        assertThat(result.createdInstances()).isOne();
        assertThat(result.createdTasks()).isEqualTo(2);
        verify(instances).save(argThat(instance -> instance.getCareGroupId() == null
                && instance.getRecipientRole() == ChecklistRecipientRole.MOTHER));
    }

    @Test
    void targetlessV2NonCadenceDistributionPersistsContractAndInteractiveSentinel() {
        ChecklistDistributionCommand legacy = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                null);
        ChecklistDistributionCommand v2 = new ChecklistDistributionCommand(
                legacy.templateLineageId(), legacy.templateVersionId(), legacy.careGroupId(),
                legacy.careGroupOwnerUserId(), legacy.contextType(), legacy.contextId(),
                legacy.contextOwnerUserId(), legacy.stage(), legacy.substage(), legacy.lifecycleDates(),
                legacy.effectiveDate(), legacy.timezone(), legacy.recipients(),
                List.of(new ChecklistDistributionItem(ChecklistDistributionTestFactory.ITEM_VERSION_ID,
                        "Recommendation", 1, true, null, null, null)),
                legacy.correlationId(), legacy.gestationalDatingRevision(), legacy.cadence(), (short) 2);
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
        when(instances.findAllByLogicalPersonalIdentity(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(tasks.findByTaskKey(any())).thenReturn(Optional.empty());

        var result = service.distribute(v2);

        assertThat(result.createdInstances()).isOne();
        assertThat(result.createdTasks()).isOne();
        verify(instances).save(argThat(instance ->
                instance.getChecklistContractVersion().equals((short) 2)
                        && "v2".equals(instance.getKeyVersion())
                        && "O:USER_CREATED".equals(instance.getPeriodKey())
                        && "UTC".equals(instance.getScheduleZoneId())
                        && instance.getMaterializationMode() == ChecklistMaterializationMode.INTERACTIVE
                        && Boolean.TRUE.equals(instance.getWasActionable())));
        verify(tasks).save(argThat(task -> task.getChecklistContractVersion().equals((short) 2)
                && task.getTargetSubject() == null
                && Boolean.TRUE.equals(task.getRequired())));
    }

    @Test
    void familyDistributionWithoutCareGroupIsRejected() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(UUID.randomUUID(),
                        ChecklistRecipientRole.FAMILY, true, true, true)),
                null);

        assertThatThrownBy(() -> service.distribute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("care group");
    }

    @Test
    void deterministicReplayReusesPersistedParentAndChildren() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                ChecklistDistributionTestFactory.CARE_GROUP_ID);
        var existing = matchingInstance(command, ChecklistDistributionTestFactory.INSTANCE_ID);
        when(instances.findByDistributionKey(any())).thenReturn(Optional.of(existing));
        ChecklistTaskInstance firstTask = ChecklistTaskInstance.builder()
                    .id(UUID.randomUUID())
                    .checklistInstanceId(existing.getId())
                    .templateVersionId(command.templateVersionId())
                    .templateItemVersionId(ChecklistDistributionTestFactory.ITEM_VERSION_ID)
                    .taskKey(ChecklistDistributionKeyFactory.childKey(
                            existing.getId(), ChecklistDistributionTestFactory.ITEM_VERSION_ID))
                    .titleSnapshot("Task A")
                    .displayOrder(1)
                    .required(true)
                    .targetSubject(ChecklistTargetSubject.MOTHER)
                    .dueAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();
        UUID secondItem = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        ChecklistTaskInstance secondTask = ChecklistTaskInstance.builder()
                    .id(UUID.randomUUID())
                    .checklistInstanceId(existing.getId())
                    .templateVersionId(command.templateVersionId())
                    .templateItemVersionId(secondItem)
                    .taskKey(ChecklistDistributionKeyFactory.childKey(existing.getId(), secondItem))
                    .titleSnapshot("Task B")
                    .displayOrder(2)
                    .required(false)
                    .targetSubject(ChecklistTargetSubject.MOTHER)
                    .dueAt(Instant.parse("2026-01-02T00:00:00Z"))
                    .build();
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(existing.getId()))
                .thenReturn(java.util.stream.Stream.of(firstTask, secondTask)
                        .sorted(java.util.Comparator.comparing(ChecklistTaskInstance::getTaskKey))
                        .toList());
        when(instances.findAllByLogicalPersonalIdentity(
                any(), any(), any(), any(), any(), any())).thenReturn(List.of(existing));

        var result = service.distribute(command);

        assertThat(result.existingInstances()).isEqualTo(1);
        assertThat(result.existingTasks()).isEqualTo(2);
        verify(instances, never()).save(any());
        verify(tasks, never()).save(any());
    }

    @Test
    void completedLegacyParentNeverReceivesNewPendingChildren() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                null);
        var completed = matchingInstance(command, ChecklistDistributionTestFactory.INSTANCE_ID);
        completed.setStatus(ChecklistInstanceStatus.COMPLETED);
        completed.setCompletedAt(clock.instant());
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
        when(instances.findAllByLogicalPersonalIdentity(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(completed));
        when(instances.findForUpdateById(completed.getId())).thenReturn(Optional.of(completed));
        when(tasks.findByTaskKey(any())).thenReturn(Optional.empty());

        var result = service.distribute(command);

        assertThat(result.existingInstances()).isOne();
        assertThat(result.createdTasks()).isZero();
        assertThat(completed.getStatus()).isEqualTo(ChecklistInstanceStatus.COMPLETED);
        verify(tasks, never()).save(any());
    }

    @Test
    void lifecycleCorrectionMarksAllObsoleteParentsHistoricalAndCreatesOneReplacement() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                ChecklistDistributionTestFactory.CARE_GROUP_ID);
        var obsolete = matchingInstance(command, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        obsolete.setWindowStart(LocalDate.of(2025, 12, 1));
        obsolete.setWindowEnd(LocalDate.of(2025, 12, 7));
        obsolete.setStatus(ChecklistInstanceStatus.PENDING);
        var started = matchingInstance(command, UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        started.setWindowStart(LocalDate.of(2025, 11, 1));
        started.setWindowEnd(LocalDate.of(2025, 11, 7));
        started.setStatus(ChecklistInstanceStatus.IN_PROGRESS);
        var obsoleteTask = ChecklistTaskInstance.builder()
                .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01"))
                .checklistInstanceId(obsolete.getId())
                .status(ChecklistTaskStatus.PENDING)
                .build();
        when(instances.findAllByLogicalPersonalIdentity(
                any(), any(), any(), any(), any(), any())).thenReturn(List.of(obsolete, started));
        when(instances.findForUpdateById(obsolete.getId())).thenReturn(Optional.of(obsolete));
        when(instances.findForUpdateById(started.getId())).thenReturn(Optional.of(started));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(obsolete.getId()))
                .thenReturn(List.of(obsoleteTask));
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
        when(tasks.findByTaskKey(any())).thenReturn(Optional.empty());

        var result = service.distribute(command);

        assertThat(result.cancelledInstances()).isEqualTo(2);
        assertThat(obsolete.getHistoricalAt()).isNotNull();
        assertThat(obsolete.getHistoryReasonCode()).isEqualTo(
                ChecklistHistoryReconciliationService.HISTORY_REASON_CODE);
        assertThat(obsolete.getStatus()).isEqualTo(ChecklistInstanceStatus.PENDING);
        assertThat(obsolete.getCancellationReasonCode()).isNull();
        assertThat(obsoleteTask.getStatus()).isEqualTo(ChecklistTaskStatus.PENDING);
        assertThat(obsoleteTask.getActionReasonCode()).isNull();
        assertThat(started.getHistoricalAt()).isNotNull();
        assertThat(started.getHistoryReasonCode()).isEqualTo(
                ChecklistHistoryReconciliationService.HISTORY_REASON_CODE);
        assertThat(started.getStatus()).isEqualTo(ChecklistInstanceStatus.IN_PROGRESS);
        assertThat(result.createdInstances()).isEqualTo(1);
    }

    @Test
    void datingRevisionSupersedesSameCalendarWindow() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                ChecklistDistributionTestFactory.CARE_GROUP_ID);
        var prior = matchingInstance(command, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        prior.setGestationalDatingRevision(2L);
        when(instances.findAllByLogicalPersonalIdentity(
                any(), any(), any(), any(), any(), any())).thenReturn(List.of(prior));
        when(instances.findForUpdateById(prior.getId())).thenReturn(Optional.of(prior));
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
        when(tasks.findByTaskKey(any())).thenReturn(Optional.empty());

        var result = service.distribute(command);

        assertThat(result.cancelledInstances()).isOne();
        assertThat(prior.getHistoricalAt()).isNotNull();
        assertThat(result.createdInstances()).isOne();
    }

    @Test
    void lifecycleScopeLockIsAcquiredBeforeObsoleteWindowDiscovery() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                ChecklistDistributionTestFactory.CARE_GROUP_ID);
        when(instances.findAllByLogicalPersonalIdentity(
                any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(instances.findByDistributionKey(anyString())).thenReturn(Optional.empty());
        when(tasks.findByTaskKey(anyString())).thenReturn(Optional.empty());

        service.distribute(command);

        InOrder order = inOrder(instances, tasks);
        order.verify(instances).acquireDistributionKeyLock(anyString());
        order.verify(instances)
                .findAllByLogicalPersonalIdentity(
                        any(), any(), any(), any(), any(), any());
        order.verify(instances).save(any());
        order.verify(tasks).findAllForUpdateByChecklistInstanceIdOrderByTaskKey(any());
    }

    @Test
    void lifecycleCorrectionDoesNotOverwriteAChildThatProgressedBeforeItsLockWasAcquired() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                ChecklistDistributionTestFactory.CARE_GROUP_ID);
        var obsolete = matchingInstance(command, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        obsolete.setWindowStart(LocalDate.of(2025, 12, 1));
        obsolete.setWindowEnd(LocalDate.of(2025, 12, 7));
        var discoveredPending = ChecklistTaskInstance.builder()
                .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02"))
                .checklistInstanceId(obsolete.getId())
                .status(ChecklistTaskStatus.PENDING)
                .build();
        var lockedProgressed = ChecklistTaskInstance.builder()
                .id(discoveredPending.getId())
                .checklistInstanceId(obsolete.getId())
                .status(ChecklistTaskStatus.IN_PROGRESS)
                .build();
        when(instances.findAllByLogicalPersonalIdentity(
                any(), any(), any(), any(), any(), any())).thenReturn(List.of(obsolete));
        when(instances.findForUpdateById(obsolete.getId())).thenReturn(Optional.of(obsolete));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(obsolete.getId()))
                .thenReturn(List.of(lockedProgressed));
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
        when(tasks.findByTaskKey(any())).thenReturn(Optional.empty());

        var result = service.distribute(command);

        assertThat(result.cancelledInstances()).isEqualTo(1);
        assertThat(obsolete.getHistoricalAt()).isNotNull();
        assertThat(obsolete.getHistoryReasonCode()).isEqualTo(
                ChecklistHistoryReconciliationService.HISTORY_REASON_CODE);
        assertThat(lockedProgressed.getStatus()).isEqualTo(ChecklistTaskStatus.IN_PROGRESS);
    }

    @Test
    void lifecycleCorrectionPreservesPendingAggregateWithActionLedgerHistory() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                ChecklistDistributionTestFactory.CARE_GROUP_ID);
        var obsolete = matchingInstance(command, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        obsolete.setWindowStart(LocalDate.of(2025, 12, 1));
        obsolete.setWindowEnd(LocalDate.of(2025, 12, 7));
        var pending = ChecklistTaskInstance.builder()
                .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa03"))
                .checklistInstanceId(obsolete.getId())
                .taskKey("a".repeat(64))
                .status(ChecklistTaskStatus.PENDING)
                .build();
        when(instances.findAllByLogicalPersonalIdentity(
                any(), any(), any(), any(), any(), any())).thenReturn(List.of(obsolete));
        when(instances.findForUpdateById(obsolete.getId())).thenReturn(Optional.of(obsolete));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(obsolete.getId()))
                .thenReturn(List.of(pending));
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());

        var result = service.distribute(command);

        assertThat(result.cancelledInstances()).isEqualTo(1);
        assertThat(obsolete.getHistoricalAt()).isNotNull();
        assertThat(obsolete.getHistoryReasonCode()).isEqualTo(
                ChecklistHistoryReconciliationService.HISTORY_REASON_CODE);
        assertThat(pending.getStatus()).isEqualTo(ChecklistTaskStatus.PENDING);
    }

    @Test
    void familyObsoleteWindowUsesLegacyCancellationAndNeverChecklistHistory() {
        UUID familyUserId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        var familyRecipient = new ChecklistDistributionRecipient(
                familyUserId, ChecklistRecipientRole.FAMILY, true, true, true);
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(familyRecipient),
                ChecklistDistributionTestFactory.CARE_GROUP_ID);
        var obsolete = familyInstance(command, familyUserId,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        obsolete.setWindowStart(LocalDate.of(2025, 12, 1));
        obsolete.setWindowEnd(LocalDate.of(2025, 12, 7));
        var pending = ChecklistTaskInstance.builder()
                .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa04"))
                .checklistInstanceId(obsolete.getId())
                .taskKey("b".repeat(64))
                .status(ChecklistTaskStatus.PENDING)
                .build();
        when(instances
                .findAllByRecipientUserIdAndRecipientRoleAndCareGroupIdAndCareContextTypeAndCareContextIdAndTemplateVersionId(
                        familyUserId,
                        ChecklistRecipientRole.FAMILY,
                        ChecklistDistributionTestFactory.CARE_GROUP_ID,
                        command.contextType(),
                        command.contextId(),
                        command.templateVersionId()))
                .thenReturn(List.of(obsolete));
        when(instances.findForUpdateById(obsolete.getId())).thenReturn(Optional.of(obsolete));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(obsolete.getId()))
                .thenReturn(List.of(pending));
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
        when(tasks.findByTaskKey(any())).thenReturn(Optional.empty());

        var result = service.distribute(command);

        assertThat(result.cancelledInstances()).isOne();
        assertThat(obsolete.getStatus()).isEqualTo(ChecklistInstanceStatus.CANCELLED);
        assertThat(obsolete.getCancelledAt()).isEqualTo(clock.instant());
        assertThat(obsolete.getCancellationReasonCode()).isEqualTo("LIFECYCLE_WINDOW_OBSOLETE");
        assertThat(obsolete.getHistoricalAt()).isNull();
        assertThat(obsolete.getHistoryReasonCode()).isNull();
        assertThat(pending.getStatus()).isEqualTo(ChecklistTaskStatus.CANCELLED);
        assertThat(pending.getCancelledAt()).isEqualTo(clock.instant());
        assertThat(pending.getActionReasonCode()).isEqualTo("LIFECYCLE_WINDOW_OBSOLETE");
        assertThat(result.createdInstances()).isEqualTo(1);
    }

    @Test
    void sameKeyWithDifferentCanonicalPayloadIsQuarantinedInsteadOfOverwritten() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                ChecklistDistributionTestFactory.CARE_GROUP_ID);
        var conflicting = matchingInstance(command, ChecklistDistributionTestFactory.INSTANCE_ID);
        conflicting.setTemplateLineageId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
        when(instances.findByDistributionKey(any())).thenReturn(Optional.of(conflicting));
        when(instances.findAllByLogicalPersonalIdentity(
                any(), any(), any(), any(), any(), any())).thenReturn(List.of(conflicting));

        var result = service.distribute(command);

        assertThat(result.conflicts()).isEqualTo(1);
        verify(audit).write(any());
        verify(tasks, never()).save(any());
    }

    @Test
    void catchUpCreatesClosedHistoryAndNeverLeavesAnActionableTask() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                null).withCadence(catchUpCadence("W:G:0000:2026-01-01"));
        List<ChecklistTaskInstance> persistedTasks = new java.util.ArrayList<>();
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
        when(instances.findAllByLogicalPersonalIdentity(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(tasks.findByTaskKey(any())).thenReturn(Optional.empty());
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(any()))
                .thenAnswer(invocation -> persistedTasks.stream().toList());
        doAnswer(invocation -> {
            ChecklistTaskInstance task = invocation.getArgument(0, ChecklistTaskInstance.class);
            if (!persistedTasks.contains(task)) {
                persistedTasks.add(task);
            }
            if (task.getId() == null) task.setId(UUID.randomUUID());
            return task;
        }).when(tasks).save(org.mockito.ArgumentMatchers.any(ChecklistTaskInstance.class));

        var result = service.distributeDetailed(command).total();

        assertThat(result.createdInstances()).isOne();
        assertThat(result.createdTasks()).isEqualTo(2);
        verify(instances, times(2)).save(any());
        verify(instances, times(2)).save(argThat(instance ->
                instance.getMaterializationMode() == ChecklistMaterializationMode.CATCH_UP
                        && Boolean.FALSE.equals(instance.getWasActionable())
                        && instance.getHistoricalAt() != null
                        && "CADENCE_PERIOD_CLOSED".equals(instance.getHistoryReasonCode())
                        && instance.getStatus() == ChecklistInstanceStatus.CANCELLED));
        assertThat(persistedTasks).allMatch(task -> task.getStatus() == ChecklistTaskStatus.CANCELLED
                && "CADENCE_PERIOD_CLOSED".equals(task.getActionReasonCode()));
    }

    @Test
    void catchUpClosesExistingInteractiveOccurrenceInsteadOfCreatingConflict() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                null).withCadence(catchUpCadence("W:G:0000:2026-01-01"));
        ChecklistInstance existing = matchingInstance(command, ChecklistDistributionTestFactory.INSTANCE_ID);
        existing.setPeriodKey(command.cadence().periodKey());
        existing.setScheduleZoneId("UTC");
        existing.setChecklistContractVersion((short) 2);
        existing.setMaterializationMode(ChecklistMaterializationMode.INTERACTIVE);
        existing.setWasActionable(Boolean.TRUE);
        ChecklistTaskInstance task = ChecklistTaskInstance.builder()
                .id(UUID.randomUUID())
                .checklistInstanceId(existing.getId())
                .taskKey("c".repeat(64))
                .status(ChecklistTaskStatus.PENDING)
                .titleSnapshot("Task")
                .build();
        when(instances.findByDistributionKey(any())).thenReturn(Optional.of(existing));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(existing.getId()))
                .thenReturn(List.of(task));

        var result = service.distributeDetailed(command).total();

        assertThat(result.conflicts()).isZero();
        assertThat(existing.getHistoricalAt()).isEqualTo(clock.instant());
        assertThat(existing.getHistoryReasonCode()).isEqualTo("CADENCE_PERIOD_CLOSED");
        assertThat(existing.getStatus()).isEqualTo(ChecklistInstanceStatus.CANCELLED);
        assertThat(task.getStatus()).isEqualTo(ChecklistTaskStatus.CANCELLED);
        assertThat(task.getActionReasonCode()).isEqualTo("CADENCE_PERIOD_CLOSED");
    }

    @Test
    void catchUpPreservesCompletedEvidenceWhileClosingOccurrenceInHistory() {
        var command = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)),
                null).withCadence(catchUpCadence("W:G:0000:2026-01-01"));
        ChecklistInstance existing = matchingInstance(command, ChecklistDistributionTestFactory.INSTANCE_ID);
        existing.setPeriodKey(command.cadence().periodKey());
        existing.setScheduleZoneId("UTC");
        existing.setChecklistContractVersion((short) 2);
        existing.setMaterializationMode(ChecklistMaterializationMode.INTERACTIVE);
        existing.setWasActionable(Boolean.TRUE);
        existing.setStatus(ChecklistInstanceStatus.COMPLETED);
        existing.setCompletedAt(clock.instant().minusSeconds(60));
        ChecklistTaskInstance task = ChecklistTaskInstance.builder()
                .id(UUID.randomUUID())
                .checklistInstanceId(existing.getId())
                .taskKey("d".repeat(64))
                .status(ChecklistTaskStatus.COMPLETED)
                .completedAt(clock.instant().minusSeconds(60))
                .titleSnapshot("Task")
                .build();
        when(instances.findByDistributionKey(any())).thenReturn(Optional.of(existing));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(existing.getId()))
                .thenReturn(List.of(task));

        service.distributeDetailed(command);

        assertThat(existing.getStatus()).isEqualTo(ChecklistInstanceStatus.COMPLETED);
        assertThat(existing.getCompletedAt()).isEqualTo(clock.instant().minusSeconds(60));
        assertThat(existing.getCancelledAt()).isNull();
        assertThat(existing.getHistoricalAt()).isEqualTo(clock.instant());
        assertThat(task.getStatus()).isEqualTo(ChecklistTaskStatus.COMPLETED);
    }

    private static ChecklistCadenceMetadata catchUpCadence(String periodKey) {
        return ChecklistCadenceMetadata.interactive(
                ChecklistScheduleType.WEEKLY,
                ChecklistMaterializationPolicy.EACH_WEEK,
                periodKey,
                ZoneId.of("UTC")).catchUp();
    }

    @Test
    void familyPermissionTruthTableIsDefaultDenyForReadAndAction() {
        var policy = new ChecklistFamilyPermissionPolicy();

        assertThat(policy.canRead(true, false, false)).isFalse();
        assertThat(policy.canAct(true, false, false)).isFalse();
        assertThat(policy.canRead(true, true, false)).isTrue();
        assertThat(policy.canAct(true, true, false)).isFalse();
        assertThat(policy.canRead(true, false, true)).isFalse();
        assertThat(policy.canAct(true, false, true)).isFalse();
        assertThat(policy.canRead(true, true, true)).isTrue();
        assertThat(policy.canAct(true, true, true)).isTrue();
        assertThat(policy.canRead(false, true, true)).isFalse();
        assertThat(policy.canAct(false, true, true)).isFalse();
    }

    @Test
    void missingItemDueAnchorFailsBeforeAnyInstanceIsPersisted() {
        ChecklistDistributionCommand base = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)), null);
        ChecklistDistributionCommand invalid = new ChecklistDistributionCommand(
                base.templateLineageId(), base.templateVersionId(), base.careGroupId(),
                base.careGroupOwnerUserId(), base.contextType(), base.contextId(), base.contextOwnerUserId(),
                base.stage(), base.substage(), base.lifecycleDates(), base.effectiveDate(), base.timezone(),
                base.recipients(), List.of(new ChecklistDistributionItem(
                        ChecklistDistributionTestFactory.ITEM_VERSION_ID, "Baby task", 1, true,
                        ChecklistTargetSubject.BABY, ChecklistAnchorType.BIRTH_DATE, 0)),
                base.correlationId(), base.gestationalDatingRevision());

        ChecklistDistributionExecutionResult result = service.distributeDetailed(invalid);

        assertThat(result.recipients()).singleElement().satisfies(recipient ->
                assertThat(recipient.dispositionCode()).isEqualTo("ITEM_DUE_ANCHOR_MISSING"));
        verify(instances, never()).save(any());
        verify(tasks, never()).save(any());
    }

    @Test
    void missingRootAnchorReturnsStableDispositionBeforePersistence() {
        ChecklistDistributionCommand base = command(ChecklistCareContextType.JOURNEY,
                ChecklistDistributionTestFactory.CONTEXT_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                List.of(new ChecklistDistributionRecipient(ChecklistDistributionTestFactory.RECIPIENT_ID,
                        ChecklistRecipientRole.MOTHER, true, true, true)), null);
        var postpartum = ChecklistLifecycleEligibilityValue.builder()
                .stage(ContentStage.POSTPARTUM.name())
                .anchorType(ChecklistAnchorType.DELIVERY_DATE)
                .rangeUnit(ChecklistRangeUnit.DAY)
                .startInclusive(0).endInclusive(42).active(true).build();
        ChecklistDistributionCommand missing = new ChecklistDistributionCommand(
                base.templateLineageId(), base.templateVersionId(), base.careGroupId(),
                base.careGroupOwnerUserId(), base.contextType(), base.contextId(), base.contextOwnerUserId(),
                ContentStage.POSTPARTUM, postpartum, new ChecklistLifecycleDates(null, null, null, null),
                base.effectiveDate(), base.timezone(), base.recipients(), base.items(),
                base.correlationId(), base.gestationalDatingRevision());

        ChecklistDistributionExecutionResult result = service.distributeDetailed(missing);

        assertThat(result.recipients()).singleElement().satisfies(recipient ->
                assertThat(recipient.dispositionCode()).isEqualTo("LIFECYCLE_ANCHOR_MISSING"));
        verify(instances, never()).save(any());
        verify(tasks, never()).save(any());
    }

    private static ChecklistDistributionCommand command(
            ChecklistCareContextType contextType,
            UUID contextId,
            UUID contextOwner,
            List<ChecklistDistributionRecipient> recipients,
            UUID careGroupId) {
        var anchor = LocalDate.of(2026, 1, 1);
        var substage = ChecklistLifecycleEligibilityValue.builder()
                .stage(ContentStage.PREGNANCY.name())
                .anchorType(ChecklistAnchorType.LMP)
                .rangeUnit(ChecklistRangeUnit.DAY)
                .startInclusive(0)
                .endInclusive(7)
                .active(true)
                .build();
        return new ChecklistDistributionCommand(
                UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"),
                ChecklistDistributionTestFactory.TEMPLATE_VERSION_ID,
                careGroupId,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                contextType,
                contextId,
                contextOwner,
                ContentStage.PREGNANCY,
                substage,
                new ChecklistLifecycleDates(anchor, anchor.plusWeeks(40), null, null),
                anchor,
                ZoneId.of("UTC"),
                recipients,
                List.of(
                        new ChecklistDistributionItem(ChecklistDistributionTestFactory.ITEM_VERSION_ID,
                                "Task A", 1, true, ChecklistTargetSubject.MOTHER,
                                ChecklistAnchorType.LMP, 0),
                        new ChecklistDistributionItem(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                                "Task B", 2, false, ChecklistTargetSubject.MOTHER,
                                ChecklistAnchorType.LMP, 1)),
                 UUID.fromString("12345678-1234-1234-1234-123456789abc"),
                 1L);
    }

    private static ChecklistInstance matchingInstance(ChecklistDistributionCommand command, UUID id) {
        return ChecklistInstance.builder()
                .id(id)
                .distributionKey("will-be-matched-by-service")
                .templateLineageId(command.templateLineageId())
                .templateVersionId(command.templateVersionId())
                .recipientUserId(ChecklistDistributionTestFactory.RECIPIENT_ID)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careGroupId(null)
                .careContextType(command.contextType())
                .careContextId(command.contextId())
                .contextOwnerUserId(command.contextOwnerUserId())
                .origin(com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE)
                .gestationalDatingRevision(command.gestationalDatingRevision())
                .windowStart(LocalDate.of(2026, 1, 1))
                .windowEnd(LocalDate.of(2026, 1, 8))
                .status(ChecklistInstanceStatus.PENDING)
                .build();
    }

    private static ChecklistInstance familyInstance(ChecklistDistributionCommand command, UUID familyUserId, UUID id) {
        return ChecklistInstance.builder()
                .id(id)
                .distributionKey("will-be-matched-by-service")
                .templateLineageId(command.templateLineageId())
                .templateVersionId(command.templateVersionId())
                .recipientUserId(familyUserId)
                .recipientRole(ChecklistRecipientRole.FAMILY)
                .careGroupId(command.careGroupId())
                .careContextType(command.contextType())
                .careContextId(command.contextId())
                .contextOwnerUserId(command.contextOwnerUserId())
                .origin(com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE)
                .windowStart(LocalDate.of(2026, 1, 1))
                .windowEnd(LocalDate.of(2026, 1, 8))
                .status(ChecklistInstanceStatus.PENDING)
                .build();
    }
}
