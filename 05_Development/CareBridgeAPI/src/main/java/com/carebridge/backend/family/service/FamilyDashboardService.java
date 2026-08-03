package com.carebridge.backend.family.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.FamilyDashboardResponse;
import com.carebridge.backend.family.dto.SharedDataResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.entity.SharedDataCategory;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.repository.HealthObservationRepository;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.ReminderRecurrenceService;
import com.carebridge.backend.security.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FamilyDashboardService {

    private static final int DASHBOARD_SHARED_DATA_PAGE_SIZE = Integer.MAX_VALUE;
    private static final ZoneId DASHBOARD_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CareGroupMemberRepository memberRepository;
    private final NotificationRecordRepository notificationRepository;
    private final EntityManager entityManager;
    private final CareGroupAuthorizationPolicy authorizationPolicy;
    private final ISharedDataService sharedDataService;
    private final UserRepository userRepository;
    private final ReminderRepository reminderRepository;
    private final ReminderRecurrenceService reminderRecurrenceService;
    private final MotherJourneyRepository journeyRepository;
    private final HealthObservationRepository observationRepository;

    public FamilyDashboardService(
            CareGroupMemberRepository memberRepository,
            NotificationRecordRepository notificationRepository,
            EntityManager entityManager,
            CareGroupAuthorizationPolicy authorizationPolicy,
            ISharedDataService sharedDataService,
            UserRepository userRepository,
            ReminderRepository reminderRepository,
            ReminderRecurrenceService reminderRecurrenceService,
            MotherJourneyRepository journeyRepository,
            HealthObservationRepository observationRepository) {
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
        this.authorizationPolicy = authorizationPolicy;
        this.sharedDataService = sharedDataService;
        this.userRepository = userRepository;
        this.reminderRepository = reminderRepository;
        this.reminderRecurrenceService = reminderRecurrenceService;
        this.journeyRepository = journeyRepository;
        this.observationRepository = observationRepository;
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
        if (group == null || group.getStatus() != CareGroupStatus.ACTIVE) {
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
                motherDisplayName(context.group().getOwnerUserId()),
                context.permissionScope().calendar()
                        ? loadMotherTodayReminders(context.group().getOwnerUserId())
                        : List.of(),
                context.alerts(),
                acceptedMembers.size(),
                acceptedMembers,
                context.membership().getFamilyRelationshipRole(),
                context.membership().getCustomFamilyRelationshipRole(),
                context.permissionScope(),
                new FamilyDashboardResponse.SharedDataSummary(sharedItemCount, sharedCategories),
                loadHealthMetricSummaries(context.group(), context.permissionScope()));
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
        boolean quickNotes = owner
                || authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.QUICK_NOTES);
        return new FamilyDashboardResponse.Permission(
                owner || authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.CALENDAR),
                owner || authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.LOGS),
                owner || authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.ALERTS),
                owner || authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.RECORDS),
                quickNotes,
                quickNotes && (owner || authorizationPolicy.hasPermission(
                        groupId, userId, PermissionFlag.QUICK_NOTE_WEIGHT)),
                quickNotes && (owner || authorizationPolicy.hasPermission(
                        groupId, userId, PermissionFlag.QUICK_NOTE_HYDRATION)),
                quickNotes && (owner || authorizationPolicy.hasPermission(
                        groupId, userId, PermissionFlag.QUICK_NOTE_EPDS)),
                quickNotes && (owner || authorizationPolicy.hasPermission(
                        groupId, userId, PermissionFlag.QUICK_NOTE_FETAL_MOVEMENT)),
                quickNotes && (owner || authorizationPolicy.hasPermission(
                        groupId, userId, PermissionFlag.QUICK_NOTE_BLOOD_PRESSURE)),
                quickNotes && (owner || authorizationPolicy.hasPermission(
                        groupId, userId, PermissionFlag.QUICK_NOTE_BLOOD_GLUCOSE)));
    }

    private List<FamilyDashboardResponse.HealthMetricSummary> loadHealthMetricSummaries(
            CareGroup group,
            FamilyDashboardResponse.Permission permission) {
        if (!permission.quickNotes()) {
            return List.of();
        }
        List<SharedMetric> permittedMetrics = sharedMetrics(permission);
        if (permittedMetrics.isEmpty()) {
            return List.of();
        }
        Optional<MotherJourney> journey = linkedOrCanonicalJourney(group);
        if (journey.isEmpty() || journey.get().getCareSubjectId() == null) {
            return permittedMetrics.stream().map(this::emptyHealthMetricSummary).toList();
        }

        UUID careSubjectId = journey.get().getCareSubjectId();
        List<String> canonicalCodes = permittedMetrics.stream()
                .map(SharedMetric::canonicalCode).distinct().toList();
        Map<String, HealthObservation> latestByCode = new LinkedHashMap<>();
        observationRepository.findLatestByMetricCodes(
                        careSubjectId, canonicalCodes, MetricStatus.ACTIVE)
                .forEach(item -> latestByCode.putIfAbsent(item.getMetricCode(), item));

        List<String> dailyCodes = permittedMetrics.stream()
                .filter(SharedMetric::dailyAggregate)
                .map(SharedMetric::canonicalCode)
                .toList();
        Map<String, List<HealthObservation>> todayByCode = new LinkedHashMap<>();
        if (!dailyCodes.isEmpty()) {
            LocalDate today = LocalDate.now(DASHBOARD_TIMEZONE);
            Instant start = today.atStartOfDay(DASHBOARD_TIMEZONE).toInstant();
            Instant end = today.plusDays(1).atStartOfDay(DASHBOARD_TIMEZONE).toInstant().minusNanos(1);
            observationRepository.findTrendByMetricCodes(
                            careSubjectId, dailyCodes, MetricStatus.ACTIVE, start, end)
                    .forEach(item -> todayByCode
                            .computeIfAbsent(item.getMetricCode(), ignored -> new ArrayList<>())
                            .add(item));
        }

        return permittedMetrics.stream()
                .map(metric -> metric.dailyAggregate()
                        ? aggregateToday(metric, todayByCode.getOrDefault(metric.canonicalCode(), List.of()))
                        : latestSummary(metric, latestByCode.get(metric.canonicalCode())))
                .toList();
    }

    private List<SharedMetric> sharedMetrics(FamilyDashboardResponse.Permission permission) {
        List<SharedMetric> result = new ArrayList<>();
        if (permission.quickNoteWeight()) {
            result.add(new SharedMetric("WEIGHT", "WEIGHT", "kg", false));
        }
        if (permission.quickNoteFetalMovement()) {
            result.add(new SharedMetric("FETAL_MOVEMENT_COUNT", "FETAL_MOVEMENT_SESSION", "count", true));
        }
        if (permission.quickNoteBloodPressure()) {
            result.add(new SharedMetric("BLOOD_PRESSURE", "BLOOD_PRESSURE", "mmHg", false));
        }
        if (permission.quickNoteHydration()) {
            result.add(new SharedMetric("HYDRATION", "HYDRATION", "ml", true));
        }
        if (permission.quickNoteEpds()) {
            result.add(new SharedMetric("EPDS_SCORE", "EPDS_SCORE", "điểm", false));
        }
        if (permission.quickNoteBloodGlucose()) {
            result.add(new SharedMetric("BLOOD_GLUCOSE", "BLOOD_GLUCOSE", "mg/dL", false));
        }
        return result;
    }

    private Optional<MotherJourney> linkedOrCanonicalJourney(CareGroup group) {
        if (group.getLinkedJourneyId() == null) {
            return journeyRepository.findCanonical(group.getOwnerUserId());
        }
        return journeyRepository.findById(group.getLinkedJourneyId())
                .filter(journey -> group.getOwnerUserId().equals(journey.getOwnerUserId()));
    }

    private FamilyDashboardResponse.HealthMetricSummary latestSummary(
            SharedMetric metric, HealthObservation observation) {
        if (observation == null) {
            return emptyHealthMetricSummary(metric);
        }
        return new FamilyDashboardResponse.HealthMetricSummary(
                metric.apiMetricType(),
                observation.getValueNumeric(),
                "EPDS_SCORE".equals(metric.canonicalCode())
                        ? null : observation.getValueSecondary(),
                unitOrDefault(observation.getUnit(), metric.defaultUnit()),
                observation.getMeasuredAt(),
                safeGlucoseContext(metric, observation),
                1);
    }

    private FamilyDashboardResponse.HealthMetricSummary aggregateToday(
            SharedMetric metric, List<HealthObservation> observations) {
        if (observations.isEmpty()) {
            return emptyHealthMetricSummary(metric);
        }
        BigDecimal total = observations.stream()
                .map(HealthObservation::getValueNumeric)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(null);
        HealthObservation latest = observations.stream()
                .max(Comparator.comparing(
                        HealthObservation::getMeasuredAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseThrow();
        return new FamilyDashboardResponse.HealthMetricSummary(
                metric.apiMetricType(),
                total,
                null,
                unitOrDefault(latest.getUnit(), metric.defaultUnit()),
                latest.getMeasuredAt(),
                null,
                observations.size());
    }

    private FamilyDashboardResponse.HealthMetricSummary emptyHealthMetricSummary(SharedMetric metric) {
        return new FamilyDashboardResponse.HealthMetricSummary(
                metric.apiMetricType(), null, null, metric.defaultUnit(), null, null, 0);
    }

    private String safeGlucoseContext(SharedMetric metric, HealthObservation observation) {
        if (!"BLOOD_GLUCOSE".equals(metric.canonicalCode()) || observation.getContext() == null) {
            return null;
        }
        Object raw = observation.getContext().get("measurementContext");
        if (raw == null) {
            return null;
        }
        String value = raw.toString();
        return Set.of("FASTING", "PRE_MEAL", "POST_MEAL_1H", "POST_MEAL_2H", "RANDOM", "OTHER_APPROVED")
                .contains(value) ? value : null;
    }

    private String unitOrDefault(String unit, String defaultUnit) {
        return unit == null || unit.isBlank() ? defaultUnit : unit;
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

    private String motherDisplayName(UUID motherId) {
        return userRepository.findById(motherId)
                .map(user -> {
                    if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
                        return user.getDisplayName();
                    }
                    if (user.getName() != null && !user.getName().isBlank()) {
                        return user.getName();
                    }
                    return "Mẹ";
                })
                .orElse("Mẹ");
    }

    private List<FamilyDashboardResponse.TodayReminder> loadMotherTodayReminders(UUID motherId) {
        LocalDate today = LocalDate.now(DASHBOARD_TIMEZONE);
        return reminderRepository
                .findByOwnerUserIdAndStatusNot(motherId, ReminderStatus.CANCELLED)
                .stream()
                .map(reminder -> reminderRecurrenceService.occurrenceForDate(
                        reminder, today, DASHBOARD_TIMEZONE))
                .flatMap(java.util.Optional::stream)
                .map(occurrence -> {
                    String type = occurrence.reminder().getReminderType() == null
                            ? "OTHER"
                            : occurrence.reminder().getReminderType().name();
                    return new FamilyDashboardResponse.TodayReminder(
                            occurrence.reminder().getId(),
                            occurrence.reminder().getTitle(),
                            type,
                            occurrence.status().name(),
                            occurrence.scheduledAt(),
                            occurrence.dueAt(),
                            occurrence.snoozedUntil(),
                            reminderPriority(type));
                })
                .sorted(Comparator
                        .comparingInt(FamilyDashboardResponse.TodayReminder::priority)
                        .thenComparing(
                                FamilyDashboardResponse.TodayReminder::dueAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private int reminderPriority(String reminderType) {
        return switch (reminderType) {
            case "VACCINATION" -> 1;
            case "MEDICATION" -> 2;
            case "APPOINTMENT" -> 3;
            default -> 4;
        };
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

    private record SharedMetric(
            String apiMetricType,
            String canonicalCode,
            String defaultUnit,
            boolean dailyAggregate) {
    }
}
