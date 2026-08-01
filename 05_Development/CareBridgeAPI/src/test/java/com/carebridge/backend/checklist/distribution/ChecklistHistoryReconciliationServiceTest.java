package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChecklistHistoryReconciliationServiceTest {

    @Test
    void marksStaleParentHistoricalWithoutChangingChildEvidence() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID instanceId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID taskId = UUID.fromString("00000000-0000-0000-0000-000000000301");
        ChecklistInstance stale = parent(owner, instanceId);
        ChecklistTaskInstance completed = ChecklistTaskInstance.builder()
                .id(taskId)
                .checklistInstanceId(instanceId)
                .status(ChecklistTaskStatus.COMPLETED)
                .completedAt(Instant.parse("2026-08-01T01:00:00Z"))
                .build();
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistCurrentScopePolicy policy = mock(ChecklistCurrentScopePolicy.class);
        LocalDate effectiveDate = LocalDate.of(2026, 8, 1);
        when(instances.findByContextOwnerUserIdAndRecipientRoleAndOriginAndHistoricalAtIsNull(
                owner, ChecklistRecipientRole.MOTHER, ChecklistOrigin.SYSTEM_TEMPLATE))
                .thenReturn(List.of(stale));
        when(policy.isHistoryManaged(stale)).thenReturn(true);
        when(policy.isCurrent(stale, effectiveDate)).thenReturn(false);
        when(instances.findForUpdateById(instanceId)).thenReturn(Optional.of(stale));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instanceId))
                .thenReturn(List.of(completed));
        var service = new ChecklistHistoryReconciliationService(
                instances,
                tasks,
                policy,
                Clock.fixed(Instant.parse("2026-08-01T02:00:00Z"), ZoneOffset.UTC));

        int marked = service.reconcile(owner, effectiveDate, ZoneId.of("UTC"), UUID.randomUUID());

        assertThat(marked).isEqualTo(1);
        assertThat(stale.getHistoricalAt()).isEqualTo(Instant.parse("2026-08-01T02:00:00Z"));
        assertThat(stale.getHistoryReasonCode()).isEqualTo(
                ChecklistHistoryReconciliationService.HISTORY_REASON_CODE);
        assertThat(completed.getStatus()).isEqualTo(ChecklistTaskStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isEqualTo(Instant.parse("2026-08-01T01:00:00Z"));
        verify(instances).acquireDistributionKeyLock(anyString());
        verify(instances).save(stale);
        verify(tasks, never()).save(completed);
    }

    @Test
    void skipsAlreadyCurrentParent() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000102");
        ChecklistInstance current = parent(owner, UUID.fromString("00000000-0000-0000-0000-000000000202"));
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistCurrentScopePolicy policy = mock(ChecklistCurrentScopePolicy.class);
        LocalDate effectiveDate = LocalDate.of(2026, 8, 1);
        when(instances.findByContextOwnerUserIdAndRecipientRoleAndOriginAndHistoricalAtIsNull(
                owner, ChecklistRecipientRole.MOTHER, ChecklistOrigin.SYSTEM_TEMPLATE))
                .thenReturn(List.of(current));
        when(policy.isHistoryManaged(current)).thenReturn(true);
        when(policy.isCurrent(current, effectiveDate)).thenReturn(true);
        var service = new ChecklistHistoryReconciliationService(
                instances, tasks, policy, Clock.systemUTC());

        int marked = service.reconcile(owner, effectiveDate, ZoneId.of("UTC"), UUID.randomUUID());

        assertThat(marked).isZero();
        verify(instances, never()).acquireDistributionKeyLock(anyString());
        verify(tasks, never()).findAllForUpdateByChecklistInstanceIdOrderByTaskKey(current.getId());
        verify(instances, never()).save(current);
    }

    private static ChecklistInstance parent(UUID owner, UUID instanceId) {
        return ChecklistInstance.builder()
                .id(instanceId)
                .templateVersionId(UUID.fromString("00000000-0000-0000-0000-000000000401"))
                .recipientUserId(owner)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careGroupId(null)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(UUID.fromString("00000000-0000-0000-0000-000000000501"))
                .contextOwnerUserId(owner)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .status(ChecklistInstanceStatus.IN_PROGRESS)
                .build();
    }
}
