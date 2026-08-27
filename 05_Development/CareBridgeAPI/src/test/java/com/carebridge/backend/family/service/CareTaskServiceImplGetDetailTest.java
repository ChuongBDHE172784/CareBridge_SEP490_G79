package com.carebridge.backend.family.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.CareTaskDetailResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.impl.CareTaskServiceImpl;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareTaskServiceImplGetDetailTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private CareTaskRepository taskRepository;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock private UserRepository userRepository;

    @InjectMocks private CareTaskServiceImpl service;

    private static final UUID GROUP_ID = UUID.fromString("a0a0a0a0-0000-4b1b-9a3d-000000000001");
    private static final UUID OTHER_GROUP_ID = UUID.fromString("a0a0a0a0-0000-4b1b-9a3d-000000000099");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID VIEWER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PENDING_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID REVOKED_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID NONMEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID TASK_ID = UUID.fromString("c1a2b3c4-1111-4b1b-9a3d-000000000010");
    private static final UUID UNKNOWN_TASK_ID = UUID.fromString("c1a2b3c4-1111-4b1b-9a3d-000000000099");

    private void arrangeAccepted(UUID callerId, CareTask task) {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_ID, callerId, InviteStatus.ACCEPTED))
                .thenReturn(true);
        when(taskRepository.findByIdAndCareGroupId(task.getId(), GROUP_ID)).thenReturn(Optional.of(task));
    }

    private static CareGroup group() {
        return CareGroup.builder().id(GROUP_ID).groupName("Synthetic group").build();
    }

    private static CareTask task(Consumer<CareTask> overrides) {
        CareTask task = CareTask.builder()
                .id(TASK_ID)
                .careGroupId(GROUP_ID)
                .assignedBy(OWNER_ID)
                .assignedTo(MEMBER_ID)
                .title("Prepare birth bag")
                .description("Synthetic task notes")
                .dueAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .status(CareTaskStatus.OPEN)
                .completedAt(null)
                .build();
        overrides.accept(task);
        return task;
    }

    private static User user(UUID id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase().replace(" ", "") + "@example.test");
        user.setPhone("0900000000");
        return user;
    }

    @Test
    void FAM221_TC_001_002_003_004_005_acceptedMembersCanViewTaskDetail() {
        for (UUID callerId : Arrays.asList(OWNER_ID, MEMBER_ID, VIEWER_ID)) {
            CareTask task = task(t -> {});
            arrangeAccepted(callerId, task);

            CareTaskDetailResponse response = service.getTaskDetail(GROUP_ID, TASK_ID, callerId);

            assertThat(response.getCareTaskId()).isEqualTo(TASK_ID);
            assertThat(response.getCareGroupId()).isEqualTo(GROUP_ID);
            assertThat(response.getTitle()).isEqualTo("Prepare birth bag");
            assertThat(response.getDescription()).isEqualTo("Synthetic task notes");
            assertThat(response.getAssignedBy()).isEqualTo(OWNER_ID);
            assertThat(response.getAssignedTo()).isEqualTo(MEMBER_ID);
            assertThat(response.getStatus()).isEqualTo("OPEN");
        }
    }

    @Test
    void FAM221_TC_006_007_008_nonAcceptedCallersAreDeniedBeforeTaskLoad() {
        for (UUID callerId : Arrays.asList(NONMEMBER_ID, PENDING_ID, REVOKED_ID)) {
            when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
            when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_ID, callerId, InviteStatus.ACCEPTED))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.getTaskDetail(GROUP_ID, TASK_ID, callerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(be.getCode()).isEqualTo("FAM-068");
                    });
        }
        verifyNoInteractions(taskRepository);
    }

    @Test
    void FAM221_TC_009_groupNotFoundReturnsFam005() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTaskDetail(GROUP_ID, TASK_ID, OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("FAM-005");
                });

        verifyNoInteractions(memberRepository);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void FAM221_TC_010_taskNotFoundReturnsFam033() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_ID, OWNER_ID, InviteStatus.ACCEPTED))
                .thenReturn(true);
        when(taskRepository.findByIdAndCareGroupId(UNKNOWN_TASK_ID, GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTaskDetail(GROUP_ID, UNKNOWN_TASK_ID, OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-033"));
    }

    @Test
    void FAM221_TC_011_wrongGroupTaskReturnsScopedNotFound() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_ID, OWNER_ID, InviteStatus.ACCEPTED))
                .thenReturn(true);
        when(taskRepository.findByIdAndCareGroupId(TASK_ID, GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTaskDetail(GROUP_ID, TASK_ID, OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-033"));

        verify(taskRepository).findByIdAndCareGroupId(TASK_ID, GROUP_ID);
        verify(taskRepository, never()).findByIdAndCareGroupId(TASK_ID, OTHER_GROUP_ID);
    }

    @Test
    void FAM221_TC_013_014_015_responseExcludesUnsupportedFields() {
        CareTask task = task(t -> {});
        arrangeAccepted(OWNER_ID, task);

        CareTaskDetailResponse response = service.getTaskDetail(GROUP_ID, TASK_ID, OWNER_ID);

        assertThat(Arrays.stream(response.getClass().getDeclaredFields()).map(Field::getName))
                .doesNotContain("priority", "checklist", "subItems", "activityHistory");
    }

    @Test
    void FAM221_TC_016_017_completedAtFollowsPersistedTaskState() {
        CareTask openTask = task(t -> t.setStatus(CareTaskStatus.IN_PROGRESS));
        arrangeAccepted(MEMBER_ID, openTask);
        assertThat(service.getTaskDetail(GROUP_ID, TASK_ID, MEMBER_ID).getCompletedAt()).isNull();

        Instant completedAt = Instant.parse("2026-07-10T01:00:00Z");
        CareTask doneTask = task(t -> {
            t.setStatus(CareTaskStatus.DONE);
            t.setCompletedAt(completedAt);
        });
        arrangeAccepted(OWNER_ID, doneTask);
        assertThat(service.getTaskDetail(GROUP_ID, TASK_ID, OWNER_ID).getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void FAM221_TC_018_statusUsesCanonicalEnumNames() {
        for (CareTaskStatus status : CareTaskStatus.values()) {
            CareTask task = task(t -> t.setStatus(status));
            arrangeAccepted(OWNER_ID, task);

            assertThat(service.getTaskDetail(GROUP_ID, TASK_ID, OWNER_ID).getStatus()).isEqualTo(status.name());
        }
    }

    @Test
    void FAM221_TC_019_resolvesDisplayNamesWithoutEmailOrPhone() {
        CareTask task = task(t -> {});
        arrangeAccepted(OWNER_ID, task);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user(OWNER_ID, "Mother One")));
        when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(user(MEMBER_ID, "Family Two")));

        CareTaskDetailResponse response = service.getTaskDetail(GROUP_ID, TASK_ID, OWNER_ID);

        assertThat(response.getAssignedByName()).isEqualTo("Mother One");
        assertThat(response.getAssignedToName()).isEqualTo("Family Two");
        assertThat(response.toString()).doesNotContain("@example.test", "0900000000");
    }

    @Test
    void FAM221_TC_020_nullAssignerAndAssigneeAreHandledWithoutUserLookup() {
        CareTask task = task(t -> {
            t.setAssignedBy(null);
            t.setAssignedTo(null);
        });
        arrangeAccepted(OWNER_ID, task);

        CareTaskDetailResponse response = service.getTaskDetail(GROUP_ID, TASK_ID, OWNER_ID);

        assertThat(response.getAssignedBy()).isNull();
        assertThat(response.getAssignedByName()).isNull();
        assertThat(response.getAssignedTo()).isNull();
        assertThat(response.getAssignedToName()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void FAM221_TC_021_membershipDenialPrecedesTaskLookupForUnknownTask() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(GROUP_ID, NONMEMBER_ID, InviteStatus.ACCEPTED))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getTaskDetail(GROUP_ID, UNKNOWN_TASK_ID, NONMEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-068"));

        verifyNoInteractions(taskRepository);
    }
}
