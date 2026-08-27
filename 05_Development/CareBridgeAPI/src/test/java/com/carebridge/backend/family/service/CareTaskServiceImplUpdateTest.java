package com.carebridge.backend.family.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.event.CareTaskUpdated;
import com.carebridge.backend.family.dto.UpdateFamilyTaskResponse;
import com.carebridge.backend.family.dto.UpdateFamilyTaskRequest;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CareTaskServiceImplUpdateTest {

    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private CareTaskRepository taskRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;

    @InjectMocks private CareTaskServiceImpl service;

    static class CareTaskUpdateTestFactory {
        static final UUID GROUP_A = UUID.fromString("a0a0a0a0-0000-4b1b-9a3d-000000000001");
        static final UUID GROUP_B = UUID.fromString("b0b0b0b0-0000-4b1b-9a3d-000000000099");
        static final UUID OWNER_USER   = UUID.fromString("11111111-0000-4b1b-9a3d-000000000001");
        static final UUID MEMBER_USER  = UUID.fromString("22222222-0000-4b1b-9a3d-000000000002");
        static final UUID VIEWER_USER  = UUID.fromString("33333333-0000-4b1b-9a3d-000000000003");
        static final UUID PENDING_USER = UUID.fromString("44444444-0000-4b1b-9a3d-000000000004");
        static final UUID TASK_OPEN    = UUID.fromString("c1a2b3c4-1111-4b1b-9a3d-000000000010");

        static CareTask makeTask(Consumer<CareTask> overrides) {
            CareTask task = new CareTask();
            task.setId(TASK_OPEN);
            task.setCareGroupId(GROUP_A);
            task.setAssignedBy(OWNER_USER);
            task.setAssignedTo(MEMBER_USER);
            task.setTitle("Original title");
            task.setDescription("Original description");
            task.setDueAt(Instant.now().plus(7, ChronoUnit.DAYS));
            task.setStatus(CareTaskStatus.OPEN);
            task.setCompletedAt(null);
            overrides.accept(task);
            return task;
        }

        static CareGroupMember makeMember(UUID userId, GroupMemberRole role, InviteStatus status, UUID groupId) {
            CareGroupMember m = new CareGroupMember();
            m.setId(UUID.randomUUID());
            m.setCareGroupId(groupId);
            m.setUserId(userId);
            m.setMemberRole(role);
            m.setInviteStatus(status);
            return m;
        }

        static UpdateFamilyTaskRequest makeRequest(Consumer<UpdateFamilyTaskRequest> overrides) {
            UpdateFamilyTaskRequest req = new UpdateFamilyTaskRequest();
            overrides.accept(req);
            return req;
        }
    }

    private void arrangeOwner() {
        when(authorizationPolicy.canUpdateTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.OWNER_USER))
                .thenReturn(true);
    }

    @Test
    void FAM222_TC_001_ownerUpdatesTitle_success() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("New title"));
        UpdateFamilyTaskResponse response = service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER);

        assertThat(response.getTitle()).isEqualTo("New title");
        assertThat(response.getStatus()).isEqualTo("OPEN");
        verify(taskRepository).save(any(CareTask.class));
    }

    @Test
    void FAM222_TC_002_ownerUpdatesDueAt_success() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        Instant future = Instant.now().plus(10, ChronoUnit.DAYS);
        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setDueAt(future));
        UpdateFamilyTaskResponse response = service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER);

        assertThat(response.getDueAt()).isEqualTo(future);
    }

    @Test
    void FAM222_TC_003_ownerUpdatesAssignee_success() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));
        
        UUID newAssignee = UUID.randomUUID();
        when(memberRepository.findByIdAndCareGroupId(newAssignee, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(CareTaskUpdateTestFactory.makeMember(CareTaskUpdateTestFactory.MEMBER_USER, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED, CareTaskUpdateTestFactory.GROUP_A)));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setAssigneeMemberId(newAssignee));
        UpdateFamilyTaskResponse response = service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER);

        assertThat(response.getAssignedTo()).isEqualTo(CareTaskUpdateTestFactory.MEMBER_USER);
    }

    @Test
    void FAM222_TC_004_ownerUpdatesDescription_success() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setDescription("Updated notes"));
        UpdateFamilyTaskResponse response = service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER);

        assertThat(response.getDescription()).isEqualTo("Updated notes");
    }

    @Test
    void FAM222_TC_005_ownerUpdatesMultipleFields_success() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        UUID newAssigneeMemberRowId = UUID.randomUUID();
        UUID newAssigneeUserId = UUID.randomUUID();
        Instant future = Instant.now().plus(12, ChronoUnit.DAYS);
        when(memberRepository.findByIdAndCareGroupId(newAssigneeMemberRowId, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(CareTaskUpdateTestFactory.makeMember(
                        newAssigneeUserId, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED,
                        CareTaskUpdateTestFactory.GROUP_A)));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> {
            r.setTitle("Updated title");
            r.setDescription("Updated description");
            r.setDueAt(future);
            r.setAssigneeMemberId(newAssigneeMemberRowId);
        });

        UpdateFamilyTaskResponse response = service.updateFamilyTask(
                CareTaskUpdateTestFactory.GROUP_A,
                CareTaskUpdateTestFactory.TASK_OPEN,
                req,
                CareTaskUpdateTestFactory.OWNER_USER);

        assertThat(response.getTitle()).isEqualTo("Updated title");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.getDueAt()).isEqualTo(future);
        assertThat(response.getAssignedTo()).isEqualTo(newAssigneeUserId);
    }

    @Test
    void FAM222_TC_006_ownerUpdatesInProgressTask_success() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> t.setStatus(CareTaskStatus.IN_PROGRESS));
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("Updated"));
        UpdateFamilyTaskResponse response = service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER);

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void FAM222_TC_007_rejectUpdateOnDoneTask() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> t.setStatus(CareTaskStatus.DONE));
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("Updated"));
        assertThatThrownBy(() -> service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-073"));
        
        verify(taskRepository, never()).save(any());
    }

    @Test
    void FAM222_TC_008_rejectUpdateOnCancelledTask() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> t.setStatus(CareTaskStatus.CANCELLED));
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("Updated"));
        assertThatThrownBy(() -> service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-073"));
    }

    @Test
    void FAM222_TC_009_nonOwnerAcceptedMemberRejected() {
        when(authorizationPolicy.canUpdateTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.MEMBER_USER))
                .thenReturn(false);

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("Updated"));
        assertThatThrownBy(() -> service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.MEMBER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-072"));
    }

    @Test
    void FAM222_TC_010_011_viewerAndNonMemberRejectedBeforeTaskLookup() {
        for (UUID callerId : List.of(CareTaskUpdateTestFactory.VIEWER_USER, UUID.randomUUID())) {
            when(authorizationPolicy.canUpdateTask(CareTaskUpdateTestFactory.GROUP_A, callerId))
                    .thenReturn(false);

            UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("Updated"));
            assertThatThrownBy(() -> service.updateFamilyTask(
                    CareTaskUpdateTestFactory.GROUP_A,
                    CareTaskUpdateTestFactory.TASK_OPEN,
                    req,
                    callerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-072"));
        }
        verifyNoInteractions(taskRepository);
    }

    @Test
    void FAM222_TC_012_newAssigneeIsPending_rejected() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        
        UUID newAssignee = UUID.randomUUID();
        when(memberRepository.findByIdAndCareGroupId(newAssignee, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(CareTaskUpdateTestFactory.makeMember(CareTaskUpdateTestFactory.PENDING_USER, GroupMemberRole.MEMBER, InviteStatus.PENDING, CareTaskUpdateTestFactory.GROUP_A)));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setAssigneeMemberId(newAssignee));
        assertThatThrownBy(() -> service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-074"));
    }

    @Test
    void FAM222_TC_013_newAssigneeDifferentGroup_rejected() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        
        UUID newAssignee = UUID.randomUUID();
        // Member belongs to GROUP_B
        when(memberRepository.findByIdAndCareGroupId(newAssignee, CareTaskUpdateTestFactory.GROUP_A)).thenReturn(Optional.empty());

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setAssigneeMemberId(newAssignee));
        assertThatThrownBy(() -> service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-074"));
    }

    @Test
    void FAM222_TC_014_newAssigneeIdDoesNotExist_rejected() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        UUID unknownMemberId = UUID.randomUUID();
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(memberRepository.findByIdAndCareGroupId(unknownMemberId, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.empty());

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setAssigneeMemberId(unknownMemberId));
        assertThatThrownBy(() -> service.updateFamilyTask(
                CareTaskUpdateTestFactory.GROUP_A,
                CareTaskUpdateTestFactory.TASK_OPEN,
                req,
                CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-074"));
    }

    @Test
    void FAM222_TC_015_blankTitle_rejected() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("   "));
        assertThatThrownBy(() -> service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-075"));
    }

    @Test
    void FAM222_TC_016_titleLongerThan255_rejected() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("x".repeat(256)));
        assertThatThrownBy(() -> service.updateFamilyTask(
                CareTaskUpdateTestFactory.GROUP_A,
                CareTaskUpdateTestFactory.TASK_OPEN,
                req,
                CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-075"));
    }

    @Test
    void FAM222_TC_017_dueAtInPast_rejected() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setDueAt(Instant.now().minus(1, ChronoUnit.DAYS)));
        assertThatThrownBy(() -> service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-075"));
    }

    @Test
    void FAM222_TC_018_dueAtEqualToNowOrPastBoundary_rejected() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setDueAt(Instant.now()));
        assertThatThrownBy(() -> service.updateFamilyTask(
                CareTaskUpdateTestFactory.GROUP_A,
                CareTaskUpdateTestFactory.TASK_OPEN,
                req,
                CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-075"));
    }

    @Test
    void FAM222_TC_019_emptyPayload_rejected() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));

        UpdateFamilyTaskRequest req = new UpdateFamilyTaskRequest(); // All null
        assertThatThrownBy(() -> service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-076"));
    }

    @Test
    void FAM222_TC_020_statusCannotBeMutatedThroughUpdateRequest() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> t.setStatus(CareTaskStatus.OPEN));
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("Updated title"));
        UpdateFamilyTaskResponse response = service.updateFamilyTask(
                CareTaskUpdateTestFactory.GROUP_A,
                CareTaskUpdateTestFactory.TASK_OPEN,
                req,
                CareTaskUpdateTestFactory.OWNER_USER);

        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(task.getStatus()).isEqualTo(CareTaskStatus.OPEN);
    }

    @Test
    void FAM222_TC_021_taskNotFound_rejected() {
        arrangeOwner();
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.empty());

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("New"));
        assertThatThrownBy(() -> service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, req, CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-033"));
    }

    @Test
    void FAM222_TC_022_wrongGroupTaskUsesScopedLookup() {
        arrangeOwner();
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.empty());

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("New"));
        assertThatThrownBy(() -> service.updateFamilyTask(
                CareTaskUpdateTestFactory.GROUP_A,
                CareTaskUpdateTestFactory.TASK_OPEN,
                req,
                CareTaskUpdateTestFactory.OWNER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-033"));

        verify(taskRepository).findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A);
    }

    @Test
    void FAM222_TC_023_completedAtUnchangedAfterSuccessfulUpdate() {
        arrangeOwner();
        Instant completedAt = Instant.parse("2026-07-10T01:00:00Z");
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> t.setCompletedAt(completedAt));
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setDescription("Updated"));
        UpdateFamilyTaskResponse response = service.updateFamilyTask(
                CareTaskUpdateTestFactory.GROUP_A,
                CareTaskUpdateTestFactory.TASK_OPEN,
                req,
                CareTaskUpdateTestFactory.OWNER_USER);

        assertThat(response.getCompletedAt()).isEqualTo(completedAt);
        assertThat(task.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void FAM222_TC_024_descriptionOmittedVsCleared() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> t.setDescription("Old"));
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        // Subcase A: Omitted
        UpdateFamilyTaskRequest reqA = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("New title"));
        UpdateFamilyTaskResponse respA = service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, reqA, CareTaskUpdateTestFactory.OWNER_USER);
        assertThat(respA.getDescription()).isEqualTo("Old");

        // Subcase B: Cleared
        UpdateFamilyTaskRequest reqB = CareTaskUpdateTestFactory.makeRequest(r -> r.setDescription(""));
        UpdateFamilyTaskResponse respB = service.updateFamilyTask(CareTaskUpdateTestFactory.GROUP_A, CareTaskUpdateTestFactory.TASK_OPEN, reqB, CareTaskUpdateTestFactory.OWNER_USER);
        assertThat(respB.getDescription()).isEmpty();
    }

    @Test
    void FAM222_eventAndAuditContainChangedFields() {
        arrangeOwner();
        CareTask task = CareTaskUpdateTestFactory.makeTask(t -> {});
        when(taskRepository.findByIdAndCareGroupId(CareTaskUpdateTestFactory.TASK_OPEN, CareTaskUpdateTestFactory.GROUP_A))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(i -> i.getArgument(0));

        UpdateFamilyTaskRequest req = CareTaskUpdateTestFactory.makeRequest(r -> r.setTitle("Updated"));
        service.updateFamilyTask(
                CareTaskUpdateTestFactory.GROUP_A,
                CareTaskUpdateTestFactory.TASK_OPEN,
                req,
                CareTaskUpdateTestFactory.OWNER_USER);

        verify(auditService).log(any(), eq(CareTaskUpdateTestFactory.OWNER_USER),
                eq("CareTask"), eq(CareTaskUpdateTestFactory.TASK_OPEN.toString()), eq("title"));
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOfSatisfying(CareTaskUpdated.class, updated -> {
                    assertThat(updated.payload().careTaskId()).isEqualTo(CareTaskUpdateTestFactory.TASK_OPEN);
                    assertThat(updated.payload().changedFields()).isEqualTo(List.of("title"));
                });
    }
}
