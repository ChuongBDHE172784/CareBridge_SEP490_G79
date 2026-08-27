package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.today.provider.CareTaskTodayTaskProvider;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareTaskTodayTaskProviderTest {
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");

    @Test
    void archivedGroupTasksAreExcludedFromToday() {
        Fixture fixture = fixture(CareGroupStatus.ARCHIVED, JOURNEY_ID, JOURNEY_ID);

        assertThat(fixture.provider().findAuthorizedTasks(ACTOR)).isEmpty();
    }

    @Test
    void taskWithoutExplicitContextIsExcludedInsteadOfFollowingGroupFallback() {
        Fixture fixture = fixture(CareGroupStatus.ACTIVE, JOURNEY_ID, null);

        assertThat(fixture.provider().findAuthorizedTasks(ACTOR)).isEmpty();
    }

    @Test
    void batchLoadsGroupsAndAuthorizesOncePerGroup() {
        CareTaskRepository tasks = mock(CareTaskRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy policy = mock(CareGroupAuthorizationPolicy.class);
        CareTask first = task(TASK_ID);
        CareTask second = task(UUID.fromString("00000000-0000-0000-0000-000000000402"));
        CareGroup group = CareGroup.builder()
                .id(GROUP_ID)
                .ownerUserId(UUID.fromString("00000000-0000-0000-0000-000000000102"))
                .groupName("Care group")
                .status(CareGroupStatus.ACTIVE)
                .linkedJourneyId(JOURNEY_ID)
                .build();
        when(tasks.findByAssignedTo(ACTOR)).thenReturn(List.of(first, second));
        when(groups.findAllById(any())).thenReturn(List.of(group));
        when(policy.hasPermission(GROUP_ID, ACTOR, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        when(policy.hasPermission(GROUP_ID, ACTOR, PermissionFlag.CHECKLIST_COMPLETE)).thenReturn(true);

        assertThat(new CareTaskTodayTaskProvider(tasks, groups, policy).findAuthorizedTasks(ACTOR))
                .hasSize(2);

        verify(groups, times(1)).findAllById(any());
        verify(groups, never()).findById(any(UUID.class));
        verify(policy, times(1)).hasPermission(GROUP_ID, ACTOR, PermissionFlag.CHECKLIST_VIEW);
        verify(policy, times(1)).hasPermission(GROUP_ID, ACTOR, PermissionFlag.CHECKLIST_COMPLETE);
    }

    private static Fixture fixture(CareGroupStatus status, UUID groupJourneyId, UUID taskJourneyId) {
        CareTaskRepository tasks = mock(CareTaskRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy policy = mock(CareGroupAuthorizationPolicy.class);
        CareTask task = CareTask.builder()
                .id(TASK_ID)
                .careGroupId(GROUP_ID)
                .assignedTo(ACTOR)
                .title("Care task")
                .status(CareTaskStatus.OPEN)
                .targetSubject(ChecklistTargetSubject.MOTHER)
                .journeyId(taskJourneyId)
                .build();
        CareGroup group = CareGroup.builder()
                .id(GROUP_ID)
                .ownerUserId(ACTOR)
                .groupName("Care group")
                .status(status)
                .linkedJourneyId(groupJourneyId)
                .build();
        when(tasks.findByAssignedTo(ACTOR)).thenReturn(List.of(task));
        when(groups.findAllById(any())).thenReturn(List.of(group));
        return new Fixture(new CareTaskTodayTaskProvider(tasks, groups, policy));
    }

    private static CareTask task(UUID id) {
        return CareTask.builder()
                .id(id)
                .careGroupId(GROUP_ID)
                .assignedTo(ACTOR)
                .title("Care task")
                .status(CareTaskStatus.OPEN)
                .targetSubject(ChecklistTargetSubject.MOTHER)
                .journeyId(JOURNEY_ID)
                .build();
    }

    private record Fixture(CareTaskTodayTaskProvider provider) {
    }
}
