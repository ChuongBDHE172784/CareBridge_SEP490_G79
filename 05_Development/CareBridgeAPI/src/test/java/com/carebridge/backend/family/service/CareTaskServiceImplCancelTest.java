package com.carebridge.backend.family.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.CancelFamilyTaskResponse;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.event.CareTaskCancelled;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.family.service.impl.CareTaskServiceImpl;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CareTaskServiceImplCancelTest {

    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private CareTaskRepository taskRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;

    @InjectMocks private CareTaskServiceImpl service;

    static class CareTaskCancelTestFactory {
        static final UUID GROUP_A = UUID.fromString("a0a0a0a0-0000-4b1b-9a3d-000000000001");
        static final UUID GROUP_B = UUID.fromString("b0b0b0b0-0000-4b1b-9a3d-000000000099");
        static final UUID OWNER_USER   = UUID.fromString("11111111-0000-4b1b-9a3d-000000000001");
        static final UUID MEMBER_USER  = UUID.fromString("22222222-0000-4b1b-9a3d-000000000002");
        static final UUID VIEWER_USER  = UUID.fromString("33333333-0000-4b1b-9a3d-000000000003");
        static final UUID NONMEMBER_USER  = UUID.fromString("44444444-0000-4b1b-9a3d-000000000004");
        static final UUID TASK_OPEN    = UUID.fromString("c1a2b3c4-1111-4b1b-9a3d-000000000010");

        static CareTask makeTask(Consumer<CareTask> overrides) {
            CareTask task = new CareTask();
            task.setId(TASK_OPEN);
            task.setCareGroupId(GROUP_A);
            task.setAssignedBy(OWNER_USER);
            task.setAssignedTo(MEMBER_USER);
            task.setTitle("Mua thuốc định kỳ");
            task.setDescription("Original description");
            task.setDueAt(Instant.now().plus(7, ChronoUnit.DAYS));
            task.setStatus(CareTaskStatus.OPEN);
            task.setCompletedAt(null);
            overrides.accept(task);
            return task;
        }

        static CareGroupMember makeMember(UUID userId, GroupMemberRole role, InviteStatus status) {
            CareGroupMember m = new CareGroupMember();
            m.setId(UUID.randomUUID());
            m.setCareGroupId(GROUP_A);
            m.setUserId(userId);
            m.setMemberRole(role);
            m.setInviteStatus(status);
            return m;
        }
    }

    private void arrangeOwner() {
        when(authorizationPolicy.canCancelTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.OWNER_USER))
                .thenReturn(true);
    }

    @Test
    void FAM223_TC_001_ownerCancelsOpenTask_success() {
        arrangeOwner();
        CareTask task = CareTaskCancelTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        CancelFamilyTaskResponse response = service.cancelFamilyTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.OWNER_USER);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        verify(taskRepository).save(any(CareTask.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void FAM223_TC_002_ownerCancelsInProgressTask_success() {
        arrangeOwner();
        CareTask task = CareTaskCancelTestFactory.makeTask(t -> t.setStatus(CareTaskStatus.IN_PROGRESS));
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        CancelFamilyTaskResponse response = service.cancelFamilyTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.OWNER_USER);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void FAM223_TC_003_rejectCancelOnDoneTask() {
        arrangeOwner();
        CareTask task = CareTaskCancelTestFactory.makeTask(t -> t.setStatus(CareTaskStatus.DONE));
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.cancelFamilyTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-080"));
        
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void FAM223_TC_004_rejectRecancelOnCancelledTask() {
        arrangeOwner();
        CareTask task = CareTaskCancelTestFactory.makeTask(t -> t.setStatus(CareTaskStatus.CANCELLED));
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.cancelFamilyTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-081"));
        
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void FAM223_TC_005_nonOwnerAcceptedMemberRejected() {
        when(authorizationPolicy.canCancelTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.MEMBER_USER))
                .thenReturn(false);

        assertThatThrownBy(() -> service.cancelFamilyTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.MEMBER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-079"));
        
        verifyNoInteractions(taskRepository);
    }

    @Test
    void FAM223_TC_006_007_viewerAndNonMemberRejectedBeforeTaskLookup() {
        for (UUID callerId : Arrays.asList(
                CareTaskCancelTestFactory.VIEWER_USER,
                CareTaskCancelTestFactory.NONMEMBER_USER)) {
            when(authorizationPolicy.canCancelTask(CareTaskCancelTestFactory.GROUP_A, callerId))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.cancelFamilyTask(
                    CareTaskCancelTestFactory.GROUP_A,
                    CareTaskCancelTestFactory.TASK_OPEN,
                    callerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-079"));
        }

        verifyNoInteractions(taskRepository);
    }

    @Test
    void FAM223_TC_008_taskNotFound() {
        arrangeOwner();
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelFamilyTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-033"));
    }

    @Test
    void FAM223_TC_009_wrongGroupTaskUsesScopedLookup() {
        arrangeOwner();
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelFamilyTask(
                CareTaskCancelTestFactory.GROUP_A,
                CareTaskCancelTestFactory.TASK_OPEN,
                CareTaskCancelTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-033"));

        verify(taskRepository).findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A);
        verify(taskRepository, never()).findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_B);
    }

    @Test
    void FAM223_TC_010_eventPayloadCorrectness() {
        arrangeOwner();
        CareTask task = CareTaskCancelTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        service.cancelFamilyTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.OWNER_USER);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOfSatisfying(CareTaskCancelled.class, cancelled -> {
                    assertThat(cancelled.payload().careTaskId()).isEqualTo(CareTaskCancelTestFactory.TASK_OPEN);
                    assertThat(cancelled.payload().careGroupId()).isEqualTo(CareTaskCancelTestFactory.GROUP_A);
                    assertThat(cancelled.payload().assignedTo()).isEqualTo(CareTaskCancelTestFactory.MEMBER_USER);
                    assertThat(cancelled.payload().assignedBy()).isEqualTo(CareTaskCancelTestFactory.OWNER_USER);
                    assertThat(cancelled.payload().cancelledBy()).isEqualTo(CareTaskCancelTestFactory.OWNER_USER);
                    assertThat(cancelled.payload().title()).isEqualTo(task.getTitle());
                });
    }

    @Test
    void FAM223_TC_011_012_noEventPublishedWhenCancelRejected() {
        arrangeOwner();
        CareTask doneTask = CareTaskCancelTestFactory.makeTask(t -> t.setStatus(CareTaskStatus.DONE));
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.of(doneTask));

        assertThatThrownBy(() -> service.cancelFamilyTask(
                CareTaskCancelTestFactory.GROUP_A,
                CareTaskCancelTestFactory.TASK_OPEN,
                CareTaskCancelTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-080"));

        verify(eventPublisher, never()).publishEvent(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void FAM223_TC_013_contentUnchanged() {
        arrangeOwner();
        CareTask task = CareTaskCancelTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        
        ArgumentCaptor<CareTask> taskCaptor = ArgumentCaptor.forClass(CareTask.class);
        when(taskRepository.save(taskCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        service.cancelFamilyTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.OWNER_USER);

        CareTask savedTask = taskCaptor.getValue();
        assertThat(savedTask.getStatus()).isEqualTo(CareTaskStatus.CANCELLED);
        assertThat(savedTask.getTitle()).isEqualTo("Mua thuốc định kỳ");
        assertThat(savedTask.getDescription()).isEqualTo("Original description");
        assertThat(savedTask.getAssignedBy()).isEqualTo(CareTaskCancelTestFactory.OWNER_USER);
        assertThat(savedTask.getAssignedTo()).isEqualTo(CareTaskCancelTestFactory.MEMBER_USER);
        assertThat(savedTask.getCompletedAt()).isNull();
    }

    @Test
    void FAM223_TC_014_updatedAtFromSavedTaskIsReturned() {
        arrangeOwner();
        Instant updatedAt = Instant.parse("2026-07-10T01:00:00Z");
        CareTask task = CareTaskCancelTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> {
            CareTask saved = i.getArgument(0);
            saved.setUpdatedAt(updatedAt);
            return saved;
        });

        CancelFamilyTaskResponse response = service.cancelFamilyTask(
                CareTaskCancelTestFactory.GROUP_A,
                CareTaskCancelTestFactory.TASK_OPEN,
                CareTaskCancelTestFactory.OWNER_USER);

        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void FAM223_TC_015_authBeforeLoad() {
        when(authorizationPolicy.canCancelTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.MEMBER_USER))
                .thenReturn(false);

        assertThatThrownBy(() -> service.cancelFamilyTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.MEMBER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-079"));
        
        verifyNoInteractions(taskRepository);
    }

    @Test
    void FAM223_TC_016_rowRetainedNoDelete() {
        arrangeOwner();
        CareTask task = CareTaskCancelTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        service.cancelFamilyTask(CareTaskCancelTestFactory.GROUP_A, CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.OWNER_USER);

        verify(taskRepository, never()).delete(any());
        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    void FAM223_TC_017_responseDtoContainsOnlyCancelFields() {
        assertThat(Arrays.stream(CancelFamilyTaskResponse.class.getDeclaredFields()).map(java.lang.reflect.Field::getName))
                .containsExactlyInAnyOrder(
                        "careTaskId",
                        "careGroupId",
                        "assignedTo",
                        "assignedBy",
                        "title",
                        "status",
                        "updatedAt");
    }

    @Test
    void FAM223_auditLogIsWrittenOnSuccessfulCancel() {
        arrangeOwner();
        CareTask task = CareTaskCancelTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskCancelTestFactory.TASK_OPEN, CareTaskCancelTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        service.cancelFamilyTask(
                CareTaskCancelTestFactory.GROUP_A,
                CareTaskCancelTestFactory.TASK_OPEN,
                CareTaskCancelTestFactory.OWNER_USER);

        verify(auditService).log(any(), eq(CareTaskCancelTestFactory.OWNER_USER),
                eq("CareTask"), eq(CareTaskCancelTestFactory.TASK_OPEN.toString()), eq("task cancelled"));
    }
}
