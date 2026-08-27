package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.RequiredAuditEvent;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.provider.CareTaskActionHandler;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CareTaskActionHandlerTest {

    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");

    private CareTaskRepository taskRepository;
    private CareGroupRepository groupRepository;
    private CareGroupAuthorizationPolicy authorizationPolicy;
    private AuditService auditService;
    private CareTaskActionHandler handler;

    @BeforeEach
    void setUp() {
        taskRepository = mock(CareTaskRepository.class);
        groupRepository = mock(CareGroupRepository.class);
        authorizationPolicy = mock(CareGroupAuthorizationPolicy.class);
        auditService = mock(AuditService.class);
        handler = new CareTaskActionHandler(
                taskRepository, groupRepository, authorizationPolicy, auditService);
    }

    @Test
    void chk028_archivedGroupCannotAuthorizeAnAssignedCareTask() {
        CareTask task = task(JOURNEY_ID);
        CareGroup group = group(CareGroupStatus.ARCHIVED, JOURNEY_ID);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        assertTaskNotFound(() -> handler.authorize(ACTOR, TASK_ID));

        verify(authorizationPolicy, never()).hasPermission(eq(GROUP_ID), eq(ACTOR), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void chk028_staleCareTaskContextCannotFollowAGroupRelink() {
        CareTask task = task(JOURNEY_ID);
        CareGroup group = group(CareGroupStatus.ACTIVE, UUID.randomUUID());
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        assertTaskNotFound(() -> handler.authorize(ACTOR, TASK_ID));
    }

    @Test
    void chk028_taskWithoutExplicitContextCannotAuthorizeAgainstGroupFallback() {
        CareTask task = task(null);
        CareGroup group = group(CareGroupStatus.ACTIVE, JOURNEY_ID);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        assertTaskNotFound(() -> handler.authorize(ACTOR, TASK_ID));
    }

    @Test
    void chk028_taskWithTwoExplicitContextsCannotAuthorize() {
        CareTask task = task(JOURNEY_ID);
        task.setBabyId(UUID.randomUUID());
        CareGroup group = group(CareGroupStatus.ACTIVE, JOURNEY_ID);
        group.setLinkedBabyProfileId(task.getBabyId());
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        assertTaskNotFound(() -> handler.authorize(ACTOR, TASK_ID));
    }

    @Test
    void chk037_careTaskActionAuditsControlledReasonAndFacadeCorrelation() {
        CareTask task = task(JOURNEY_ID);
        CareGroup group = group(CareGroupStatus.ACTIVE, JOURNEY_ID);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(taskRepository.save(task)).thenReturn(task);
        var authorized = handler.authorize(ACTOR, TASK_ID);
        UUID correlationId = UUID.randomUUID();
        Instant appliedAt = Instant.parse("2026-08-03T12:00:00Z");

        handler.apply(authorized, ACTOR, TaskAction.COMPLETE, null, appliedAt, correlationId);

        ArgumentCaptor<RequiredAuditEvent> event = ArgumentCaptor.forClass(RequiredAuditEvent.class);
        verify(auditService).logRequired(event.capture());
        assertThat(event.getValue().action()).isEqualTo(AuditAction.CARE_TASK_STATUS_UPDATED);
        assertThat(event.getValue().actorUserId()).isEqualTo(ACTOR);
        assertThat(event.getValue().resourceId()).isEqualTo(TASK_ID);
        assertThat(event.getValue().beforePayload()).containsEntry("action", "COMPLETE")
                .containsEntry("status", "PENDING");
        assertThat(event.getValue().afterPayload()).containsEntry("status", "COMPLETED");
        assertThat(event.getValue().reasonCode()).isEqualTo("USER_ACTION");
        assertThat(event.getValue().correlationId()).isEqualTo(correlationId);
    }

    private static CareTask task(UUID journeyId) {
        return CareTask.builder()
                .id(TASK_ID)
                .careGroupId(GROUP_ID)
                .assignedTo(ACTOR)
                .targetSubject(ChecklistTargetSubject.MOTHER)
                .journeyId(journeyId)
                .status(CareTaskStatus.OPEN)
                .title("Current-context task")
                .build();
    }

    private static CareGroup group(CareGroupStatus status, UUID journeyId) {
        return CareGroup.builder()
                .id(GROUP_ID)
                .ownerUserId(ACTOR)
                .groupName("Care group")
                .linkedJourneyId(journeyId)
                .status(status)
                .build();
    }

    private static void assertTaskNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
    }
}
