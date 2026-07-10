package com.carebridge.backend.family.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.CareGroupTestFactory;
import com.carebridge.backend.family.dto.AssignFamilyTaskRequest;
import com.carebridge.backend.family.dto.AssignFamilyTaskResponse;
import com.carebridge.backend.family.dto.CareTaskDto;
import com.carebridge.backend.family.dto.CareTasksResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.event.FamilyTaskAssigned;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.impl.CareTaskServiceImpl;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.service.FcmService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.carebridge.backend.family.CareGroupTestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareTaskServiceImplTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private CareTaskRepository taskRepository;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock private FcmService fcmService;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private CareTaskServiceImpl service;

    // ── Happy-path mock helpers ───────────────────────────────────────────────

    private void stubHappyPath() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> {
            g.setId(GROUP_ID);
            g.setOwnerUserId(OWNER_ID);
        });
        CareGroupMember assigneeMember = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(ASSIGNEE_ID);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        CareTask savedTask = CareGroupTestFactory.makeCareTask();

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID))
                .thenReturn(Optional.of(assigneeMember));
        when(taskRepository.save(any(CareTask.class))).thenReturn(savedTask);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenReturn(List.of());
    }

    // ── FAM73-TC-001: Happy path — Owner assigns task to ACCEPTED member ──────

    @Test
    void assignFamilyTask_ownerAssignsToAcceptedMember_returnsResponse() {
        stubHappyPath();
        AssignFamilyTaskRequest req = makeAssignFamilyTaskRequest();

        AssignFamilyTaskResponse response = service.assignFamilyTask(GROUP_ID, req, OWNER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getAssignedTo()).isEqualTo(ASSIGNEE_ID);
        assertThat(response.getAssignedBy()).isEqualTo(OWNER_ID);
        verify(taskRepository).save(argThat(t -> t.getStatus() == CareTaskStatus.OPEN));
    }

    // ── FAM73-TC-002: Care group not found → 404 FAM-005 ─────────────────────

    @Test
    void assignFamilyTask_groupNotFound_throws404Fam005() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignFamilyTask(GROUP_ID, makeAssignFamilyTaskRequest(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("FAM-005");
                });
        verify(taskRepository, never()).save(any());
    }

    // ── FAM73-TC-003: Caller is MEMBER (not OWNER) → 403 FAM-031 ─────────────

    @Test
    void assignFamilyTask_callerNotOwner_throws403Fam031() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.assignFamilyTask(GROUP_ID, makeAssignFamilyTaskRequest(), MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("FAM-031");
                });
        verify(taskRepository, never()).save(any());
    }

    // ── FAM73-TC-005: Assignee is PENDING → 409 FAM-030 ─────────────────────

    @Test
    void assignFamilyTask_assigneePending_throws409Fam030() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        CareGroupMember pendingMember = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(ASSIGNEE_ID);
            m.setInviteStatus(InviteStatus.PENDING);
        });
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID))
                .thenReturn(Optional.of(pendingMember));

        assertThatThrownBy(() -> service.assignFamilyTask(GROUP_ID, makeAssignFamilyTaskRequest(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-030"));
        verify(taskRepository, never()).save(any());
    }

    // ── FAM73-TC-006: Assignee is REVOKED → 409 FAM-030 ─────────────────────

    @Test
    void assignFamilyTask_assigneeRevoked_throws409Fam030() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        CareGroupMember revokedMember = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(ASSIGNEE_ID);
            m.setInviteStatus(InviteStatus.REVOKED);
        });
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID))
                .thenReturn(Optional.of(revokedMember));

        assertThatThrownBy(() -> service.assignFamilyTask(GROUP_ID, makeAssignFamilyTaskRequest(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-030"));
    }

    // ── FAM73-TC-007: Assignee not in group at all → 409 FAM-030 ─────────────

    @Test
    void assignFamilyTask_assigneeNotMember_throws409Fam030() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignFamilyTask(GROUP_ID, makeAssignFamilyTaskRequest(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-030"));
    }

    // ── FAM73-TC-008: Due date in the past → 400 FAM-032 ────────────────────

    @Test
    void assignFamilyTask_pastDueDate_throws400Fam032() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        CareGroupMember assignee = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(ASSIGNEE_ID);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID))
                .thenReturn(Optional.of(assignee));

        AssignFamilyTaskRequest req = makeAssignFamilyTaskRequest(
                r -> r.setDueAt(Instant.now().minus(1, ChronoUnit.DAYS)));

        assertThatThrownBy(() -> service.assignFamilyTask(GROUP_ID, req, OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getCode()).isEqualTo("FAM-032");
                });
        verify(taskRepository, never()).save(any());
    }

    // ── FAM73-TC-009: Due date exactly = now (boundary) → 400 FAM-032 ────────

    @Test
    void assignFamilyTask_dueAtExactlyNow_throws400Fam032() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        CareGroupMember assignee = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(ASSIGNEE_ID);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID))
                .thenReturn(Optional.of(assignee));

        // Capture "now" — at evaluation time T <= Instant.now(), boundary is exclusive
        Instant boundary = Instant.now();
        AssignFamilyTaskRequest req = makeAssignFamilyTaskRequest(r -> r.setDueAt(boundary));

        assertThatThrownBy(() -> service.assignFamilyTask(GROUP_ID, req, OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-032"));
    }

    // ── FAM73-TC-010: Due date 1 second in future (boundary — PASS) ──────────

    @Test
    void assignFamilyTask_dueAt1SecondFuture_taskCreated() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        CareGroupMember assignee = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(ASSIGNEE_ID);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        CareTask savedTask = CareGroupTestFactory.makeCareTask();
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID))
                .thenReturn(Optional.of(assignee));
        when(taskRepository.save(any())).thenReturn(savedTask);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenReturn(List.of());

        AssignFamilyTaskRequest req = makeAssignFamilyTaskRequest(
                r -> r.setDueAt(Instant.now().plusSeconds(1)));

        AssignFamilyTaskResponse response = service.assignFamilyTask(GROUP_ID, req, OWNER_ID);

        assertThat(response).isNotNull();
    }

    // ── FAM73-TC-013: Owner self-assigns → allowed ────────────────────────────

    @Test
    void assignFamilyTask_ownerSelfAssigns_taskCreated() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> {
            g.setId(GROUP_ID);
            g.setOwnerUserId(OWNER_ID);
        });
        CareGroupMember ownerMember = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(OWNER_ID);
            m.setMemberRole(GroupMemberRole.OWNER);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        CareTask savedTask = CareGroupTestFactory.makeCareTask(t -> t.setAssignedTo(OWNER_ID));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, OWNER_ID))
                .thenReturn(Optional.of(ownerMember));
        when(taskRepository.save(any())).thenReturn(savedTask);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenReturn(List.of());

        AssignFamilyTaskRequest req = makeAssignFamilyTaskRequest(r -> r.setAssigneeMemberId(OWNER_ID));

        AssignFamilyTaskResponse response = service.assignFamilyTask(GROUP_ID, req, OWNER_ID);

        assertThat(response).isNotNull();
    }

    // ── FAM73-TC-014: FCM failure does NOT rollback task ──────────────────────

    @Test
    void assignFamilyTask_fcmThrows_taskStillPersisted() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        CareGroupMember assignee = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(ASSIGNEE_ID);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        CareTask savedTask = CareGroupTestFactory.makeCareTask();

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID))
                .thenReturn(Optional.of(assignee));
        when(taskRepository.save(any())).thenReturn(savedTask);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any()))
                .thenReturn(List.of(CareGroupTestFactory.makeDeviceToken(ASSIGNEE_ID)));
        when(fcmService.sendToToken(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("FCM unavailable"));

        // Must NOT throw — FCM failure is non-blocking per ADR-FAM-031
        AssignFamilyTaskResponse response = service.assignFamilyTask(GROUP_ID, makeAssignFamilyTaskRequest(), OWNER_ID);

        assertThat(response).isNotNull();
        verify(taskRepository).save(any()); // task was still saved
    }

    // ── FAM73-TC-015: Successful assign publishes FamilyTaskAssigned event ────

    @Test
    void assignFamilyTask_success_publishesFamilyTaskAssignedEvent() {
        stubHappyPath();
        AssignFamilyTaskRequest req = makeAssignFamilyTaskRequest();

        service.assignFamilyTask(GROUP_ID, req, OWNER_ID);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        Object event = eventCaptor.getValue();
        assertThat(event).isInstanceOf(FamilyTaskAssigned.class);
        FamilyTaskAssigned evt = (FamilyTaskAssigned) event;
        assertThat(evt.payload().careGroupId()).isEqualTo(GROUP_ID);
        assertThat(evt.payload().assignedBy()).isEqualTo(OWNER_ID);
        assertThat(evt.payload().assignedTo()).isEqualTo(ASSIGNEE_ID);
    }

    // ── FAM73-TC-016: Successful assign writes CARE_TASK_ASSIGNED audit ───────

    @Test
    void assignFamilyTask_success_writesAuditLog() {
        stubHappyPath();

        service.assignFamilyTask(GROUP_ID, makeAssignFamilyTaskRequest(), OWNER_ID);

        verify(auditService).log(
                eq(AuditAction.CARE_TASK_ASSIGNED),
                eq(OWNER_ID),
                eq("CareTask"),
                anyString(),
                anyString()
        );
    }

    // ── FAM73-TC-017: listTasks returns tasks for ACCEPTED member ─────────────

    @Test
    void listTasks_acceptedMember_returnsTaskList() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        CareTask task1 = CareGroupTestFactory.makeCareTask();
        CareTask task2 = CareGroupTestFactory.makeCareTask(t -> t.setTitle("Second Task"));

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, MEMBER_ID, InviteStatus.ACCEPTED)).thenReturn(true);
        when(taskRepository.findByCareGroupId(GROUP_ID)).thenReturn(List.of(task1, task2));

        CareTasksResponse response = service.listTasks(GROUP_ID, MEMBER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getTotalTasks()).isEqualTo(2);
        assertThat(response.getTasks()).hasSize(2);
    }

    // ── FAM73-TC-018: listTasks denied for non-member → 403 FAM-003 ──────────

    @Test
    void listTasks_nonMember_throws403Fam003() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        UUID strangerUserId = UUID.randomUUID();
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, strangerUserId, InviteStatus.ACCEPTED)).thenReturn(false);

        assertThatThrownBy(() -> service.listTasks(GROUP_ID, strangerUserId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("FAM-003");
                });
    }

    // ── FAM73-TC-019: listTasks on empty group → 200 with empty list ──────────

    @Test
    void listTasks_noTasksInGroup_returnsEmptyList() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, MEMBER_ID, InviteStatus.ACCEPTED)).thenReturn(true);
        when(taskRepository.findByCareGroupId(GROUP_ID)).thenReturn(List.of());

        CareTasksResponse response = service.listTasks(GROUP_ID, MEMBER_ID);

        assertThat(response.getTotalTasks()).isZero();
        assertThat(response.getTasks()).isNotNull().isEmpty();
    }

    // ── FAM73-TC-020: Service never returns raw CareTask entity ───────────────

    @Test
    void assignFamilyTask_response_isDtoNotEntity() {
        stubHappyPath();

        Object result = service.assignFamilyTask(GROUP_ID, makeAssignFamilyTaskRequest(), OWNER_ID);

        assertThat(result).isInstanceOf(AssignFamilyTaskResponse.class);
        assertThat(result).isNotInstanceOf(CareTask.class);
    }

    @Test
    void listTasks_response_isDtoNotEntity() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, MEMBER_ID, InviteStatus.ACCEPTED)).thenReturn(true);
        when(taskRepository.findByCareGroupId(GROUP_ID)).thenReturn(List.of());

        Object result = service.listTasks(GROUP_ID, MEMBER_ID);

        assertThat(result).isInstanceOf(CareTasksResponse.class);
        assertThat(result).isNotInstanceOf(CareTask.class);
        if (result instanceof CareTasksResponse r) {
            assertThat(r.getTasks()).isNotNull();
            r.getTasks().forEach(t -> assertThat(t).isInstanceOf(CareTaskDto.class));
        }
    }
}
