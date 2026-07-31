package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.service.ChecklistV2CompatibilityMutationService;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.common.exception.BusinessException;
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
    @Spy private UnifiedTaskMutationPolicy mutationPolicy = new UnifiedTaskMutationPolicy();
    private ChecklistV2CompatibilityMutationService service;
    private UUID actorId;
    private UUID taskId;
    private ChecklistInstance instance;

    @BeforeEach
    void setUp() {
        service = new ChecklistV2CompatibilityMutationService(
                taskRepository, instanceRepository, accessPolicy, mutationPolicy);
        actorId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(
                ChecklistTaskInstance.builder().id(taskId).checklistInstanceId(instanceId).build()));
        instance = ChecklistInstance.builder().id(instanceId).origin(ChecklistOrigin.SYSTEM_TEMPLATE).build();
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
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

    private static void assertImmutable(org.assertj.core.api.ThrowableAssert.ThrowingCallable invocation) {
        assertThatThrownBy(invocation).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getHttpStatus().value()).isEqualTo(409);
            assertThat(error.getCode()).isEqualTo("SYSTEM_TASK_IMMUTABLE");
        });
    }
}
