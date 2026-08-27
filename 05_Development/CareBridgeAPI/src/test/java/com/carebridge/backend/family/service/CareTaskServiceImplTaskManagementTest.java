package com.carebridge.backend.family.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.CancelFamilyTaskResponse;
import com.carebridge.backend.family.dto.CareTaskDetailResponse;
import com.carebridge.backend.family.dto.UpdateFamilyTaskRequest;
import com.carebridge.backend.family.dto.UpdateFamilyTaskResponse;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.impl.CareTaskServiceImpl;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.carebridge.backend.family.CareGroupTestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareTaskServiceImplTaskManagementTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private CareTaskRepository taskRepository;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock private FcmService fcmService;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserRepository userRepository;
    @InjectMocks private CareTaskServiceImpl service;

    @Test
    void getTaskDetail_acceptedMember_returnsDtoWithoutEntityExposure() {
        CareTask task = makeCareTask();
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(makeCareGroup(g -> g.setId(GROUP_ID))));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_ID, MEMBER_ID, InviteStatus.ACCEPTED))
                .thenReturn(true);
        when(taskRepository.findByIdAndCareGroupId(task.getId(), GROUP_ID)).thenReturn(Optional.of(task));

        CareTaskDetailResponse response = service.getTaskDetail(GROUP_ID, task.getId(), MEMBER_ID);

        assertThat(response.getCareTaskId()).isEqualTo(task.getId());
        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getAssignedTo()).isEqualTo(ASSIGNEE_ID);
    }

    @Test
    void updateFamilyTask_ownerUpdatesOpenTask_mutatesContentOnly() {
        CareTask task = makeCareTask();
        UpdateFamilyTaskRequest request = new UpdateFamilyTaskRequest();
        request.setTitle("Updated task");
        request.setDueAt(Instant.now().plus(2, ChronoUnit.DAYS));
        when(authorizationPolicy.canUpdateTask(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(taskRepository.findByIdAndCareGroupId(task.getId(), GROUP_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        UpdateFamilyTaskResponse response = service.updateFamilyTask(GROUP_ID, task.getId(), request, OWNER_ID);

        assertThat(response.getTitle()).isEqualTo("Updated task");
        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(task.getCompletedAt()).isNull();
        verify(auditService).log(eq(AuditAction.CARE_TASK_UPDATED), eq(OWNER_ID),
                eq("CareTask"), eq(task.getId().toString()), anyString());
    }

    @Test
    void updateFamilyTask_doneTask_throwsFam073() {
        CareTask task = makeCareTask(t -> t.setStatus(CareTaskStatus.DONE));
        UpdateFamilyTaskRequest request = new UpdateFamilyTaskRequest();
        request.setTitle("Updated task");
        when(authorizationPolicy.canUpdateTask(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(taskRepository.findByIdAndCareGroupId(task.getId(), GROUP_ID)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.updateFamilyTask(GROUP_ID, task.getId(), request, OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("FAM-073");
                });
        verify(taskRepository, never()).save(any());
    }

    @Test
    void cancelFamilyTask_ownerCancelsOpenTask_setsCancelledAndPublishesEvent() {
        CareTask task = makeCareTask();
        when(authorizationPolicy.canCancelTask(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(taskRepository.findByIdAndCareGroupId(task.getId(), GROUP_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        CancelFamilyTaskResponse response = service.cancelFamilyTask(GROUP_ID, task.getId(), OWNER_ID);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(task.getTitle()).isEqualTo("Check medication");
        verify(auditService).log(eq(AuditAction.CARE_TASK_CANCELLED), eq(OWNER_ID),
                eq("CareTask"), eq(task.getId().toString()), contains("cancelled"));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void cancelFamilyTask_alreadyCancelled_throwsFam081AndDoesNotPublish() {
        CareTask task = makeCareTask(t -> t.setStatus(CareTaskStatus.CANCELLED));
        when(authorizationPolicy.canCancelTask(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(taskRepository.findByIdAndCareGroupId(task.getId(), GROUP_ID)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.cancelFamilyTask(GROUP_ID, task.getId(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-081"));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
