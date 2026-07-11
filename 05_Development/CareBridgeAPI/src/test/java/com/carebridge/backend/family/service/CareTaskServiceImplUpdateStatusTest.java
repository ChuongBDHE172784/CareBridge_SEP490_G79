package com.carebridge.backend.family.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.UpdateTaskStatusRequest;
import com.carebridge.backend.family.dto.UpdateTaskStatusResponse;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.impl.CareTaskServiceImpl;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.service.FcmService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UC-85: Update Assigned Task Status — service layer unit tests.
 * Tests CareTaskServiceImpl.updateTaskStatus() with Mockito.
 * Oracle: ADR-FAM-005 (FSM), ADR-FAM-006 (assignee-only auth).
 * Error codes: FAM-005 (group not found), FAM-033 (task not found),
 *              FAM-034 (not assignee), FAM-035 (invalid status), FAM-023 (bad FSM transition).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CareTaskServiceImplUpdateStatusTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private CareTaskRepository taskRepository;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock private FcmService fcmService;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CareTaskServiceImpl service;

    private static final UUID GROUP_ID   = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID TASK_ID    = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID ASSIGNEE   = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID OTHER_USER = UUID.fromString("dddddddd-0000-0000-0000-000000000004");

    private CareGroup makeGroup() {
        CareGroup g = new CareGroup();
        g.setId(GROUP_ID);
        g.setOwnerUserId(OTHER_USER);
        g.setGroupName("Test Group");
        g.setStatus(CareGroupStatus.ACTIVE);
        return g;
    }

    private CareTask makeTask(CareTaskStatus status) {
        CareTask task = CareTask.builder()
                .id(TASK_ID)
                .careGroupId(GROUP_ID)
                .assignedBy(OTHER_USER)
                .assignedTo(ASSIGNEE)
                .title("Take medication")
                .status(status)
                .build();
        return task;
    }

    private UpdateTaskStatusRequest makeRequest(String status) {
        UpdateTaskStatusRequest req = new UpdateTaskStatusRequest();
        req.setStatus(status);
        return req;
    }

    private void stubGroupAndTask(CareTask task) {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(makeGroup()));
        when(taskRepository.findByIdAndCareGroupId(TASK_ID, GROUP_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(CareTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenReturn(java.util.List.of());
    }

    // ─── FAM-UC85-TC-001: OPEN → IN_PROGRESS (valid) ────────────────────────

    @Test
    void updateStatus_openToInProgress_succeeds() {
        CareTask task = makeTask(CareTaskStatus.OPEN);
        stubGroupAndTask(task);

        UpdateTaskStatusResponse response = service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("IN_PROGRESS"), ASSIGNEE);

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getCompletedAt()).isNull();
        verify(taskRepository).save(argThat(t -> t.getStatus() == CareTaskStatus.IN_PROGRESS));
    }

    // ─── FAM-UC85-TC-002: OPEN → DONE → completedAt set ────────────────────

    @Test
    void updateStatus_openToDone_setsCompletedAt() {
        CareTask task = makeTask(CareTaskStatus.OPEN);
        stubGroupAndTask(task);

        UpdateTaskStatusResponse response = service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("DONE"), ASSIGNEE);

        assertThat(response.getStatus()).isEqualTo("DONE");
        verify(taskRepository).save(argThat(t ->
                t.getStatus() == CareTaskStatus.DONE && t.getCompletedAt() != null));
    }

    // ─── FAM-UC85-TC-003: OPEN → NEEDS_SUPPORT (valid) ─────────────────────

    @Test
    void updateStatus_openToNeedsSupport_succeeds() {
        CareTask task = makeTask(CareTaskStatus.OPEN);
        stubGroupAndTask(task);

        UpdateTaskStatusResponse response = service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("NEEDS_SUPPORT"), ASSIGNEE);

        assertThat(response.getStatus()).isEqualTo("NEEDS_SUPPORT");
    }

    // ─── FAM-UC85-TC-004: IN_PROGRESS → DONE → completedAt set ─────────────

    @Test
    void updateStatus_inProgressToDone_setsCompletedAt() {
        CareTask task = makeTask(CareTaskStatus.IN_PROGRESS);
        stubGroupAndTask(task);

        service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("DONE"), ASSIGNEE);

        verify(taskRepository).save(argThat(t ->
                t.getStatus() == CareTaskStatus.DONE && t.getCompletedAt() != null));
    }

    // ─── FAM-UC85-TC-008: DONE → IN_PROGRESS (invalid transition) → FAM-023 ─

    @Test
    void updateStatus_terminalToDone_throwsFam023() {
        CareTask task = makeTask(CareTaskStatus.DONE);
        stubGroupAndTask(task);

        assertThatThrownBy(() -> service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("IN_PROGRESS"), ASSIGNEE))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-023");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
        verify(taskRepository, never()).save(any());
    }

    // ─── FAM-UC85-TC-018: Non-assignee → FAM-034 403 ────────────────────────

    @Test
    void updateStatus_callerIsNotAssignee_throwsFam034() {
        CareTask task = makeTask(CareTaskStatus.OPEN);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(makeGroup()));
        when(taskRepository.findByIdAndCareGroupId(TASK_ID, GROUP_ID)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("IN_PROGRESS"), OTHER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-034");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    // ─── FAM-UC85-TC-022: Task not found → FAM-033 ──────────────────────────

    @Test
    void updateStatus_taskNotFound_throwsFam033() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(makeGroup()));
        when(taskRepository.findByIdAndCareGroupId(TASK_ID, GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("IN_PROGRESS"), ASSIGNEE))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-033"));
    }

    // ─── Group not found → FAM-005 ───────────────────────────────────────────

    @Test
    void updateStatus_groupNotFound_throwsFam005() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("IN_PROGRESS"), ASSIGNEE))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-005"));
    }

    // ─── FAM-UC85-TC-023: Invalid status string → FAM-035 400 ───────────────

    @Test
    void updateStatus_invalidStatusString_throwsFam035() {
        CareTask task = makeTask(CareTaskStatus.OPEN);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(makeGroup()));
        when(taskRepository.findByIdAndCareGroupId(TASK_ID, GROUP_ID)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("INVALID_STATUS_XYZ"), ASSIGNEE))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-035");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    // ─── Audit log emitted on success ────────────────────────────────────────

    @Test
    void updateStatus_successful_emitsAuditLog() {
        CareTask task = makeTask(CareTaskStatus.OPEN);
        stubGroupAndTask(task);

        service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("IN_PROGRESS"), ASSIGNEE);

        verify(auditService).log(
                eq(AuditAction.CARE_TASK_STATUS_UPDATED),
                eq(ASSIGNEE),
                eq("CareTask"),
                eq(TASK_ID.toString()),
                anyString()
        );
    }

    // ─── FCM failure does not propagate ──────────────────────────────────────

    @Test
    void updateStatus_fcmThrows_doesNotFail() {
        CareTask task = makeTask(CareTaskStatus.OPEN);
        stubGroupAndTask(task);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any()))
                .thenThrow(new RuntimeException("FCM unavailable"));

        assertThatCode(() -> service.updateTaskStatus(GROUP_ID, TASK_ID, makeRequest("IN_PROGRESS"), ASSIGNEE))
                .doesNotThrowAnyException();
    }
}
