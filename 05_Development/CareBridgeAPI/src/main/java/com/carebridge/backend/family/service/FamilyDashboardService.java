package com.carebridge.backend.family.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.FamilyDashboardResponse;
import com.carebridge.backend.family.dto.SharedDataResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.entity.SharedDataCategory;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.security.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FamilyDashboardService {

    private static final int DASHBOARD_SHARED_DATA_PAGE_SIZE = Integer.MAX_VALUE;

    private final CareGroupMemberRepository memberRepository;
    private final NotificationRecordRepository notificationRepository;
    private final EntityManager entityManager;
    private final CareGroupAuthorizationPolicy authorizationPolicy;
    private final ISharedDataService sharedDataService;
    private final UserRepository userRepository;

    public FamilyDashboardService(
            CareGroupMemberRepository memberRepository,
            NotificationRecordRepository notificationRepository,
            EntityManager entityManager,
            CareGroupAuthorizationPolicy authorizationPolicy,
            ISharedDataService sharedDataService,
            UserRepository userRepository) {
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
        this.authorizationPolicy = authorizationPolicy;
        this.sharedDataService = sharedDataService;
        this.userRepository = userRepository;
    }

    public FamilyDashboardResponse get(UUID userId, UUID requestedCareGroupId) {
        Instant now = Instant.now();
        List<GroupContext> contexts = memberRepository
                .findByUserIdAndInviteStatus(userId, InviteStatus.ACCEPTED)
                .stream()
                .map(membership -> loadGroupContext(membership, userId, now))
                .filter(Objects::nonNull)
                .sorted(groupContextComparator())
                .toList();

        if (requestedCareGroupId != null
                && contexts.stream().noneMatch(context -> context.group().getId().equals(requestedCareGroupId))) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FAM-063",
                    "Care group is not an accepted membership.");
        }

        UUID selectedCareGroupId = requestedCareGroupId != null
                ? requestedCareGroupId
                : contexts.stream().findFirst().map(context -> context.group().getId()).orElse(null);

        List<FamilyDashboardResponse.Group> groups = contexts.stream()
                .map(this::toGroupResponse)
                .toList();
        FamilyDashboardResponse.Aggregate globalAggregate = aggregate(
                contexts.stream().flatMap(context -> context.tasks().stream()).toList(),
                contexts.stream().mapToLong(context -> context.alerts().size()).sum(),
                now);

        if (selectedCareGroupId == null) {
            return new FamilyDashboardResponse(groups, globalAggregate, null, null);
        }

        GroupContext selectedContext = contexts.stream()
                .filter(context -> context.group().getId().equals(selectedCareGroupId))
                .findFirst()
                .orElseThrow();
        return new FamilyDashboardResponse(
                groups,
                globalAggregate,
                selectedCareGroupId,
                toSelectedGroupDetail(selectedContext, userId));
    }

    private GroupContext loadGroupContext(CareGroupMember membership, UUID userId, Instant now) {
        CareGroup group = entityManager.find(CareGroup.class, membership.getCareGroupId());
        if (group == null) {
            return null;
        }

        List<CareTask> tasks = loadRequesterTasks(group.getId(), userId);
        FamilyDashboardResponse.Permission permissionScope = permissionScope(group.getId(), userId);
        List<FamilyDashboardResponse.Alert> alerts = permissionScope.alerts()
                ? loadScopedAlerts(group.getId(), userId)
                : List.of();
        Instant lastActivityAt = tasks.stream()
                .map(CareTask::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new GroupContext(
                membership,
                group,
                tasks,
                alerts,
                permissionScope,
                lastActivityAt,
                aggregate(tasks, alerts.size(), now));
    }

    private Comparator<GroupContext> groupContextComparator() {
        return Comparator
                .comparing(
                        GroupContext::lastActivityAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                        context -> context.membership().getJoinedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                        context -> context.group().getGroupName(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(context -> context.group().getId().toString());
    }

    private FamilyDashboardResponse.Group toGroupResponse(GroupContext context) {
        return new FamilyDashboardResponse.Group(
                context.group().getId(),
                context.group().getGroupName(),
                context.membership().getJoinedAt(),
                context.lastActivityAt(),
                context.membership().getFamilyRelationshipRole(),
                context.membership().getCustomFamilyRelationshipRole(),
                context.permissionScope(),
                context.aggregate());
    }

    private FamilyDashboardResponse.Detail toSelectedGroupDetail(GroupContext context, UUID userId) {
        UUID groupId = context.group().getId();
        List<FamilyDashboardResponse.Member> acceptedMembers = memberRepository
                .findByCareGroupIdAndInviteStatusIn(groupId, List.of(InviteStatus.ACCEPTED))
                .stream()
                .filter(member -> member.getInviteStatus() == InviteStatus.ACCEPTED)
                .map(this::toMemberResponse)
                .sorted(Comparator
                        .comparing(
                                FamilyDashboardResponse.Member::displayName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(member -> member.memberId().toString()))
                .toList();

        List<FamilyDashboardResponse.SharedDataCategory> sharedCategories = new ArrayList<>();
        sharedCategories.add(sharedCategory(
                groupId,
                userId,
                SharedDataCategory.CALENDAR,
                context.permissionScope().calendar()));
        sharedCategories.add(sharedCategory(
                groupId,
                userId,
                SharedDataCategory.LOGS,
                context.permissionScope().logs()));
        sharedCategories.add(new FamilyDashboardResponse.SharedDataCategory(
                SharedDataCategory.ALERTS.name(),
                context.permissionScope().alerts(),
                context.alerts().size()));
        int sharedItemCount = sharedCategories.stream()
                .mapToInt(FamilyDashboardResponse.SharedDataCategory::itemCount)
                .sum();

        return new FamilyDashboardResponse.Detail(
                groupId,
                context.tasks().stream().map(this::toTaskResponse).toList(),
                context.alerts(),
                acceptedMembers.size(),
                acceptedMembers,
                context.membership().getFamilyRelationshipRole(),
                context.membership().getCustomFamilyRelationshipRole(),
                context.permissionScope(),
                new FamilyDashboardResponse.SharedDataSummary(sharedItemCount, sharedCategories));
    }

    private FamilyDashboardResponse.Member toMemberResponse(CareGroupMember member) {
        String displayName = userRepository.findById(member.getUserId())
                .map(user -> {
                    if (user.getName() != null && !user.getName().isBlank()) {
                        return user.getName();
                    }
                    if (user.getPhone() != null && !user.getPhone().isBlank()) {
                        return user.getPhone();
                    }
                    return "Thành viên";
                })
                .orElse("Thành viên");
        return new FamilyDashboardResponse.Member(
                member.getId(),
                member.getUserId(),
                displayName,
                member.getMemberRole() == null ? null : member.getMemberRole().name(),
                member.getFamilyRelationshipRole(),
                member.getCustomFamilyRelationshipRole(),
                member.getJoinedAt());
    }

    private FamilyDashboardResponse.SharedDataCategory sharedCategory(
            UUID groupId,
            UUID userId,
            SharedDataCategory category,
            boolean permitted) {
        if (!permitted) {
            return new FamilyDashboardResponse.SharedDataCategory(category.name(), false, 0);
        }
        SharedDataResponse response = sharedDataService.getSharedData(
                groupId,
                userId,
                category,
                0,
                DASHBOARD_SHARED_DATA_PAGE_SIZE);
        return new FamilyDashboardResponse.SharedDataCategory(
                category.name(),
                true,
                response.getItems().size());
    }

    private List<FamilyDashboardResponse.Alert> loadScopedAlerts(UUID groupId, UUID userId) {
        return notificationRepository
                .findByUserIdAndTypeAndCareGroupId(userId, NotificationType.EMERGENCY, groupId)
                .stream()
                .sorted(Comparator.comparing(
                        NotificationRecord::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(record -> new FamilyDashboardResponse.Alert(
                        record.getId(),
                        groupId,
                        record.getTitle(),
                        record.getBody(),
                        record.getCreatedAt(),
                        record.isRead()))
                .toList();
    }

    private FamilyDashboardResponse.Permission permissionScope(UUID groupId, UUID userId) {
        boolean owner = authorizationPolicy.isOwner(groupId, userId);
        return new FamilyDashboardResponse.Permission(
                owner || authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.CALENDAR),
                owner || authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.LOGS),
                owner || authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.ALERTS),
                owner || authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.BABY_VIEW));
    }

    private List<CareTask> loadRequesterTasks(UUID groupId, UUID userId) {
        return entityManager
                .createQuery(
                        "select task from FamilyCareTask task "
                                + "where task.careGroupId = :groupId and task.assignedTo = :userId",
                        CareTask.class)
                .setParameter("groupId", groupId)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .sorted(Comparator
                        .comparing(CareTask::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CareTask::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(task -> task.getId().toString()))
                .toList();
    }

    private FamilyDashboardResponse.Task toTaskResponse(CareTask task) {
        return new FamilyDashboardResponse.Task(
                task.getId(),
                task.getCareGroupId(),
                task.getTitle(),
                task.getStatus().name(),
                task.getDueAt(),
                task.getUpdatedAt());
    }

    private FamilyDashboardResponse.Aggregate aggregate(
            List<CareTask> tasks,
            long alertCount,
            Instant now) {
        Instant dueSoonCutoff = now.plus(Duration.ofDays(2));
        long overdue = tasks.stream()
                .filter(this::isActive)
                .filter(task -> task.getDueAt() != null && task.getDueAt().isBefore(now))
                .count();
        long dueSoon = tasks.stream()
                .filter(this::isActive)
                .filter(task -> task.getDueAt() != null
                        && !task.getDueAt().isBefore(now)
                        && task.getDueAt().isBefore(dueSoonCutoff))
                .count();
        long inProgress = tasks.stream()
                .filter(task -> task.getStatus() == CareTaskStatus.IN_PROGRESS)
                .count();
        return new FamilyDashboardResponse.Aggregate(overdue, dueSoon, inProgress, alertCount);
    }

    private boolean isActive(CareTask task) {
        return task.getStatus() != CareTaskStatus.DONE
                && task.getStatus() != CareTaskStatus.CANCELLED;
    }

    private record GroupContext(
            CareGroupMember membership,
            CareGroup group,
            List<CareTask> tasks,
            List<FamilyDashboardResponse.Alert> alerts,
            FamilyDashboardResponse.Permission permissionScope,
            Instant lastActivityAt,
            FamilyDashboardResponse.Aggregate aggregate) {
    }
}
