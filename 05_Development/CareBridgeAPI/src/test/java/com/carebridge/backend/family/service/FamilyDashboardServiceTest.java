package com.carebridge.backend.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.SharedDataResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.entity.SharedDataCategory;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FamilyDashboardServiceTest {

    @Mock
    private CareGroupMemberRepository memberRepository;
    @Mock
    private NotificationRecordRepository notificationRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock
    private ISharedDataService sharedDataService;
    @Mock
    private UserRepository userRepository;

    private final UUID userId = UUID.randomUUID();
    private final Map<UUID, List<CareTask>> tasksByGroup = new HashMap<>();
    private final List<Object> requesterQueryParameters = new ArrayList<>();
    private FamilyDashboardService service;

    @BeforeEach
    void setUp() {
        service = new FamilyDashboardService(
                memberRepository,
                notificationRepository,
                entityManager,
                authorizationPolicy,
                sharedDataService,
                userRepository);
        stubTaskQuery();
    }

    @Test
    void noAcceptedGroupReturnsEmptyDashboard() {
        when(memberRepository.findByUserIdAndInviteStatus(userId, InviteStatus.ACCEPTED))
                .thenReturn(List.of());

        var response = service.get(userId, null);

        assertThat(response.groups()).isEmpty();
        assertThat(response.selectedCareGroupId()).isNull();
        assertThat(response.selectedGroupDetail()).isNull();
        assertThat(response.globalAggregate().overdue()).isZero();
        assertThat(response.globalAggregate().alerts()).isZero();
    }

    @Test
    void oneAcceptedGroupReturnsSelectionAndDetail() {
        UUID groupId = UUID.randomUUID();
        CareGroupMember membership = membership(groupId, userId, Instant.parse("2026-07-01T00:00:00Z"));
        stubAcceptedGroups(List.of(membership), Map.of(groupId, group(groupId, "Gia đình A")));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(groupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(membership));

        var response = service.get(userId, null);

        assertThat(response.groups()).hasSize(1);
        assertThat(response.selectedCareGroupId()).isEqualTo(groupId);
        assertThat(response.selectedGroupDetail()).isNotNull();
        assertThat(response.selectedGroupDetail().careGroupId()).isEqualTo(groupId);
    }

    @Test
    void multipleGroupsDefaultToLatestTaskActivityBeforeMembershipJoinTime() {
        UUID activeGroupId = UUID.randomUUID();
        UUID newlyJoinedGroupId = UUID.randomUUID();
        CareGroupMember activeMembership = membership(
                activeGroupId, userId, Instant.parse("2026-01-01T00:00:00Z"));
        CareGroupMember newMembership = membership(
                newlyJoinedGroupId, userId, Instant.parse("2026-07-20T00:00:00Z"));
        CareTask recentlyUpdatedTask = task(
                activeGroupId,
                CareTaskStatus.OPEN,
                Instant.now().plusSeconds(3600),
                Instant.parse("2026-07-29T00:00:00Z"));
        tasksByGroup.put(activeGroupId, List.of(recentlyUpdatedTask));
        stubAcceptedGroups(
                List.of(newMembership, activeMembership),
                Map.of(
                        activeGroupId, group(activeGroupId, "A"),
                        newlyJoinedGroupId, group(newlyJoinedGroupId, "B")));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(
                        activeGroupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(activeMembership));

        var response = service.get(userId, null);

        assertThat(response.groups()).extracting(group -> group.id())
                .containsExactly(activeGroupId, newlyJoinedGroupId);
        assertThat(response.selectedCareGroupId()).isEqualTo(activeGroupId);
    }

    @Test
    void selectedGroupOutsideAcceptedMembershipIsForbidden() {
        UUID unavailableGroupId = UUID.randomUUID();
        when(memberRepository.findByUserIdAndInviteStatus(userId, InviteStatus.ACCEPTED))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.get(userId, unavailableGroupId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("accepted membership");
    }

    @Test
    void selectedDetailContainsOnlyRequesterTasksFromSelectedGroup() {
        UUID firstGroupId = UUID.randomUUID();
        UUID selectedGroupId = UUID.randomUUID();
        CareGroupMember firstMembership = membership(firstGroupId, userId, Instant.now().minusSeconds(100));
        CareGroupMember selectedMembership = membership(selectedGroupId, userId, Instant.now());
        CareTask firstTask = task(firstGroupId, CareTaskStatus.OPEN, Instant.now().plusSeconds(7200), Instant.now());
        CareTask selectedTask = task(
                selectedGroupId, CareTaskStatus.IN_PROGRESS, Instant.now().plusSeconds(3600), Instant.now());
        tasksByGroup.put(firstGroupId, List.of(firstTask));
        tasksByGroup.put(selectedGroupId, List.of(selectedTask));
        stubAcceptedGroups(
                List.of(firstMembership, selectedMembership),
                Map.of(
                        firstGroupId, group(firstGroupId, "A"),
                        selectedGroupId, group(selectedGroupId, "B")));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(
                        selectedGroupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(selectedMembership));

        var response = service.get(userId, selectedGroupId);

        assertThat(response.selectedGroupDetail().tasks())
                .extracting(task -> task.id())
                .containsExactly(selectedTask.getId());
        assertThat(response.selectedGroupDetail().tasks())
                .allMatch(task -> task.careGroupId().equals(selectedGroupId));
        assertThat(requesterQueryParameters).containsOnly(userId);
    }

    @Test
    void globalTaskAggregateCombinesAllAcceptedGroupsAndExcludesTerminalDueTasks() {
        UUID firstGroupId = UUID.randomUUID();
        UUID secondGroupId = UUID.randomUUID();
        Instant now = Instant.now();
        CareGroupMember firstMembership = membership(firstGroupId, userId, now.minusSeconds(10));
        CareGroupMember secondMembership = membership(secondGroupId, userId, now.minusSeconds(20));
        tasksByGroup.put(firstGroupId, List.of(
                task(firstGroupId, CareTaskStatus.OPEN, now.minusSeconds(60), now),
                task(firstGroupId, CareTaskStatus.DONE, now.plusSeconds(60), now)));
        tasksByGroup.put(secondGroupId, List.of(
                task(secondGroupId, CareTaskStatus.IN_PROGRESS, now.plusSeconds(3600), now)));
        stubAcceptedGroups(
                List.of(firstMembership, secondMembership),
                Map.of(
                        firstGroupId, group(firstGroupId, "A"),
                        secondGroupId, group(secondGroupId, "B")));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(
                        firstGroupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(firstMembership));

        var response = service.get(userId, firstGroupId);

        assertThat(response.globalAggregate().overdue()).isEqualTo(1);
        assertThat(response.globalAggregate().dueSoon()).isEqualTo(1);
        assertThat(response.globalAggregate().inProgress()).isEqualTo(1);
    }

    @Test
    void scopedAlertAggregateCombinesOnlyCareGroupScopedQueries() {
        UUID firstGroupId = UUID.randomUUID();
        UUID secondGroupId = UUID.randomUUID();
        CareGroupMember firstMembership = membership(firstGroupId, userId, Instant.now());
        CareGroupMember secondMembership = membership(secondGroupId, userId, Instant.now());
        stubAcceptedGroups(
                List.of(firstMembership, secondMembership),
                Map.of(
                        firstGroupId, group(firstGroupId, "A"),
                        secondGroupId, group(secondGroupId, "B")));
        allowAlerts(firstGroupId);
        allowAlerts(secondGroupId);
        when(notificationRepository.findByUserIdAndTypeAndCareGroupId(
                        userId, NotificationType.EMERGENCY, firstGroupId))
                .thenReturn(List.of(alert(firstGroupId), alert(firstGroupId)));
        when(notificationRepository.findByUserIdAndTypeAndCareGroupId(
                        userId, NotificationType.EMERGENCY, secondGroupId))
                .thenReturn(List.of(alert(secondGroupId)));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(
                        firstGroupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(firstMembership));

        var response = service.get(userId, firstGroupId);

        assertThat(response.globalAggregate().alerts()).isEqualTo(3);
        assertThat(response.groups()).extracting(group -> group.aggregate().alerts())
                .containsExactlyInAnyOrder(2L, 1L);
        assertThat(response.selectedGroupDetail().alerts())
                .allMatch(alert -> alert.careGroupId().equals(firstGroupId));
    }

    @Test
    void legacyNullScopeAlertIsExcludedBecauseDashboardNeverUsesUnscopedQuery() {
        UUID groupId = UUID.randomUUID();
        CareGroupMember membership = membership(groupId, userId, Instant.now());
        stubAcceptedGroups(List.of(membership), Map.of(groupId, group(groupId, "A")));
        allowAlerts(groupId);
        NotificationRecord scoped = alert(groupId);
        NotificationRecord legacy = alert(null);
        when(notificationRepository.findByUserIdAndTypeAndCareGroupId(
                        userId, NotificationType.EMERGENCY, groupId))
                .thenReturn(List.of(scoped));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(groupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(membership));

        var response = service.get(userId, groupId);

        assertThat(response.selectedGroupDetail().alerts())
                .extracting(alert -> alert.id())
                .containsExactly(scoped.getId())
                .doesNotContain(legacy.getId());
        verify(notificationRepository, never())
                .findByUserIdAndType(any(), any(), any());
    }

    @Test
    void alertsPermissionFalseReturnsNoAlertDataOrAggregate() {
        UUID groupId = UUID.randomUUID();
        CareGroupMember membership = membership(groupId, userId, Instant.now());
        stubAcceptedGroups(List.of(membership), Map.of(groupId, group(groupId, "A")));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(groupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(membership));

        var response = service.get(userId, groupId);

        assertThat(response.groups().get(0).permissionScope().alerts()).isFalse();
        assertThat(response.globalAggregate().alerts()).isZero();
        assertThat(response.selectedGroupDetail().alerts()).isEmpty();
        verify(notificationRepository, never())
                .findByUserIdAndTypeAndCareGroupId(any(), any(), any());
    }

    @Test
    void selectedMemberSummaryContainsOnlyAcceptedMembersAndBothRoleTypes() {
        UUID groupId = UUID.randomUUID();
        UUID acceptedUserId = UUID.randomUUID();
        UUID pendingUserId = UUID.randomUUID();
        CareGroupMember requester = membership(groupId, userId, Instant.now());
        CareGroupMember accepted = CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(groupId)
                .userId(acceptedUserId)
                .memberRole(GroupMemberRole.VIEWER)
                .familyRelationshipRole("OTHER")
                .customFamilyRelationshipRole("Dì")
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .build();
        CareGroupMember pending = CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(groupId)
                .userId(pendingUserId)
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.PENDING)
                .build();
        stubAcceptedGroups(List.of(requester), Map.of(groupId, group(groupId, "A")));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(groupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(accepted, pending));
        when(userRepository.findById(acceptedUserId))
                .thenReturn(Optional.of(User.builder().id(acceptedUserId).name("Nguyễn An").build()));

        var response = service.get(userId, groupId);

        assertThat(response.selectedGroupDetail().members()).hasSize(1);
        var member = response.selectedGroupDetail().members().get(0);
        assertThat(member.displayName()).isEqualTo("Nguyễn An");
        assertThat(member.systemRole()).isEqualTo("VIEWER");
        assertThat(member.relationshipRole()).isEqualTo("OTHER");
        assertThat(member.customRelationshipRole()).isEqualTo("Dì");
    }

    @Test
    void sharedDataSummaryUsesExistingPermissionCheckedService() {
        UUID groupId = UUID.randomUUID();
        CareGroupMember membership = membership(groupId, userId, Instant.now());
        stubAcceptedGroups(List.of(membership), Map.of(groupId, group(groupId, "A")));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(groupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(membership));
        lenient().when(authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.LOGS)).thenReturn(true);
        when(sharedDataService.getSharedData(
                        groupId, userId, SharedDataCategory.LOGS, 0, Integer.MAX_VALUE))
                .thenReturn(SharedDataResponse.builder()
                        .groupId(groupId)
                        .category("LOGS")
                        .items(List.of())
                        .build());

        var response = service.get(userId, groupId);

        assertThat(response.selectedGroupDetail().sharedDataSummary().categories())
                .anySatisfy(category -> {
                    assertThat(category.category()).isEqualTo("LOGS");
                    assertThat(category.permitted()).isTrue();
                    assertThat(category.itemCount()).isZero();
                });
        verify(sharedDataService)
                .getSharedData(groupId, userId, SharedDataCategory.LOGS, 0, Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    private void stubTaskQuery() {
        lenient().when(entityManager.createQuery(anyString(), eq(CareTask.class))).thenAnswer(invocation -> {
            TypedQuery<CareTask> query = mock(TypedQuery.class);
            AtomicReference<UUID> groupId = new AtomicReference<>();
            lenient().when(query.setParameter(anyString(), any())).thenAnswer(parameterInvocation -> {
                String name = parameterInvocation.getArgument(0);
                Object value = parameterInvocation.getArgument(1);
                if ("groupId".equals(name)) {
                    groupId.set((UUID) value);
                } else if ("userId".equals(name)) {
                    requesterQueryParameters.add(value);
                }
                return query;
            });
            lenient().when(query.getResultList())
                    .thenAnswer(ignored -> tasksByGroup.getOrDefault(groupId.get(), List.of()));
            return query;
        });
    }

    private void stubAcceptedGroups(List<CareGroupMember> memberships, Map<UUID, CareGroup> groups) {
        when(memberRepository.findByUserIdAndInviteStatus(userId, InviteStatus.ACCEPTED))
                .thenReturn(memberships);
        groups.forEach((groupId, group) -> when(entityManager.find(CareGroup.class, groupId)).thenReturn(group));
    }

    private void allowAlerts(UUID groupId) {
        lenient().when(authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.ALERTS)).thenReturn(true);
    }

    private CareGroupMember membership(UUID groupId, UUID memberUserId, Instant joinedAt) {
        return CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(groupId)
                .userId(memberUserId)
                .memberRole(GroupMemberRole.MEMBER)
                .familyRelationshipRole("GRANDMOTHER")
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(joinedAt)
                .build();
    }

    private CareGroup group(UUID groupId, String name) {
        return CareGroup.builder().id(groupId).groupName(name).build();
    }

    private CareTask task(UUID groupId, CareTaskStatus status, Instant dueAt, Instant updatedAt) {
        return CareTask.builder()
                .id(UUID.randomUUID())
                .careGroupId(groupId)
                .assignedTo(userId)
                .title("Nhiệm vụ thật")
                .status(status)
                .dueAt(dueAt)
                .createdAt(updatedAt.minusSeconds(60))
                .updatedAt(updatedAt)
                .build();
    }

    private NotificationRecord alert(UUID groupId) {
        return NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.EMERGENCY)
                .careGroupId(groupId)
                .title("Cảnh báo")
                .body("Nội dung")
                .createdAt(Instant.now())
                .build();
    }
}
