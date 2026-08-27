package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.service.ChecklistV2CompatibilityMutationService;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.common.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistV2CompatibilityMutationServiceTest {
    @Mock private ChecklistTaskInstanceRepository taskRepository;
    @Mock private ChecklistInstanceRepository instanceRepository;
    @Mock private UnifiedTaskAccessPolicy accessPolicy;
    @Mock private ChecklistAuditWriter auditWriter;
    @Mock private EntityManager entityManager;
    @Spy private UnifiedTaskMutationPolicy mutationPolicy = new UnifiedTaskMutationPolicy();
    private ChecklistV2CompatibilityMutationService service;
    private UUID actorId;
    private UUID taskId;
    private ChecklistInstance instance;
    private ChecklistTaskInstance task;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        task = ChecklistTaskInstance.builder().id(taskId).checklistInstanceId(instanceId)
                .status(ChecklistTaskStatus.PENDING).build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        instance = ChecklistInstance.builder().id(instanceId).origin(ChecklistOrigin.SYSTEM_TEMPLATE).build();
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        service = new ChecklistV2CompatibilityMutationService(
                taskRepository, instanceRepository, accessPolicy, mutationPolicy, auditWriter, entityManager);
    }

    @Test
    void authorizedSystemTaskUsesProductionMutationPolicyAndReturnsImmutable() {
        when(accessPolicy.canView(instance, actorId)).thenReturn(true);

        assertImmutable(() -> service.rejectUpdate(taskId, actorId));
        assertImmutable(() -> service.rejectDelete(taskId, actorId));

        verify(mutationPolicy, org.mockito.Mockito.times(2)).requireMutable(ChecklistOrigin.SYSTEM_TEMPLATE);
    }

    @Test
    void unauthorizedSystemTaskIsIndistinguishableFromMissing() {
        when(accessPolicy.canView(instance, actorId)).thenReturn(false);

        assertThatThrownBy(() -> service.rejectUpdate(taskId, actorId))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getHttpStatus().value()).isEqualTo(404);
                    assertThat(error.getCode()).isEqualTo("TASK_NOT_FOUND");
                });
        verify(mutationPolicy, never()).requireMutable(ChecklistOrigin.SYSTEM_TEMPLATE);
    }

    @Test
    void authorizedUserCreatedTaskDoesNotReviveLegacyMutation() {
        instance.setOrigin(ChecklistOrigin.USER_CREATED);
        when(accessPolicy.canView(instance, actorId)).thenReturn(true);

        assertThatThrownBy(() -> service.rejectDelete(taskId, actorId))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getHttpStatus().value()).isEqualTo(410);
                    assertThat(error.getCode()).isEqualTo("CHECKLIST_LEGACY_ROUTE_RETIRED");
                });
    }

    @Test
    void deleteCancelsUserCreatedTaskAndParentWithoutPhysicalDelete() {
        instance.setOrigin(ChecklistOrigin.USER_CREATED);
        instance.setStatus(ChecklistInstanceStatus.IN_PROGRESS);
        instance.setRecipientUserId(actorId);
        instance.setRecipientRole(ChecklistRecipientRole.MOTHER);
        instance.setCareContextType(ChecklistCareContextType.JOURNEY);
        instance.setCareContextId(UUID.randomUUID());
        instance.setContextOwnerUserId(actorId);
        when(accessPolicy.canView(instance, actorId)).thenReturn(true);
        when(instanceRepository.findForUpdateById(instance.getId())).thenReturn(Optional.of(instance));
        when(taskRepository.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instance.getId()))
                .thenReturn(java.util.List.of(task));

        service.delete(taskId, actorId);

        assertThat(task.getStatus()).isEqualTo(ChecklistTaskStatus.CANCELLED);
        assertThat(task.getActionReasonCode()).isEqualTo("USER_DELETED");
        assertThat(instance.getStatus()).isEqualTo(ChecklistInstanceStatus.CANCELLED);
        verify(taskRepository).save(task);
        verify(instanceRepository).save(instance);
        verify(auditWriter, org.mockito.Mockito.times(2)).write(any());
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void deleteSystemTaskRejectsBeforeLockingOrPersistenceMutation() {
        when(accessPolicy.canView(instance, actorId)).thenReturn(true);

        assertImmutable(() -> service.delete(taskId, actorId));

        verify(instanceRepository, never()).acquireDistributionKeyLock(any());
        verify(taskRepository, never()).save(any());
        verify(instanceRepository, never()).save(any());
        verifyNoInteractions(auditWriter);
    }

    private static void assertImmutable(org.assertj.core.api.ThrowableAssert.ThrowingCallable invocation) {
        assertThatThrownBy(invocation).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getHttpStatus().value()).isEqualTo(409);
            assertThat(error.getCode()).isEqualTo("SYSTEM_TASK_IMMUTABLE");
        });
    }
}
