package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
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
class UserChecklistItemSystemTaskMutationTest {

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
    void updateSystemTaskRejectsEvenOrderOnlyMutation() {
        when(accessPolicy.canView(instance, actorId)).thenReturn(true);

        assertThatThrownBy(() -> service.rejectUpdate(taskId, actorId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("SYSTEM_TASK_IMMUTABLE");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(409);
                });

        verify(mutationPolicy).requireMutable(ChecklistOrigin.SYSTEM_TEMPLATE);
        verify(taskRepository, never()).save(task);
    }

    @Test
    void deleteSystemTaskRejectsBeforePersistenceMutation() {
        when(accessPolicy.canView(instance, actorId)).thenReturn(true);

        assertThatThrownBy(() -> service.rejectDelete(taskId, actorId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("SYSTEM_TASK_IMMUTABLE");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(409);
                });

        verify(mutationPolicy).requireMutable(ChecklistOrigin.SYSTEM_TEMPLATE);
        verify(taskRepository, never()).save(task);
    }
}
