package com.carebridge.backend.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
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
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.ReminderRecurrenceService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.Instant;
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
    @Mock
    private ReminderRepository reminderRepository;
    @Mock
    private ReminderRecurrenceService reminderRecurrenceService;
    @Mock
    private MotherJourneyRepository journeyRepository;
    @Mock
    private HealthObservationRepository observationRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID motherId = UUID.randomUUID();
    private final Map<UUID, List<CareTask>> tasksByGroup = new HashMap<>();
    private FamilyDashboardService service;

    @BeforeEach
    void setUp() {
        service = new FamilyDashboardService(
                memberRepository,
                notificationRepository,
                entityManager,
                authorizationPolicy,
                sharedDataService,
                userRepository,
                reminderRepository,
                reminderRecurrenceService,
                journeyRepository,
                observationRepository);
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
    void archivedAcceptedGroupIsExcludedFromDashboard() {
        UUID groupId = UUID.randomUUID();
        CareGroup archived = group(groupId, "Archived");
        archived.setStatus(CareGroupStatus.ARCHIVED);
        stubAcceptedGroups(
                List.of(membership(groupId, userId, Instant.now())),
                Map.of(groupId, archived));

        var response = service.get(userId, null);

        assertThat(response.groups()).isEmpty();
        assertThat(response.selectedGroupDetail()).isNull();
        verify(observationRepository, never())
                .findLatestByMetricCodes(any(), anyList(), any());
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
    void selectedDetailContainsOnlySelectedGroupMotherTodayReminders() {
        UUID groupId = UUID.randomUUID();
        CareGroupMember membership = membership(groupId, userId, Instant.now());
        Instant scheduledAt = Instant.parse("2026-07-30T02:00:00Z");
        Reminder reminder = Reminder.builder()
                .id(UUID.randomUUID())
                .ownerUserId(motherId)
                .journeyId(UUID.randomUUID())
                .reminderType(ReminderType.APPOINTMENT)
                .title("Khám thai")
                .scheduledAt(scheduledAt)
                .status(ReminderStatus.PENDING)
                .build();
        CareGroup group = group(groupId, "A");
        group.setLinkedJourneyId(reminder.getJourneyId());
        stubAcceptedGroups(List.of(membership), Map.of(groupId, group));
        allowCalendar(groupId);
        when(journeyRepository.existsByIdAndOwnerUserIdAndStatus(
                        reminder.getJourneyId(), motherId,
                        com.carebridge.backend.journey.entity.JourneyStatus.ACTIVE))
                .thenReturn(true);
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(
                        groupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(membership));
        lenient().when(userRepository.findById(motherId))
                .thenReturn(Optional.of(User.builder()
                        .id(motherId)
                        .displayName("Nguyễn Lan")
                        .build()));
        when(reminderRepository.findByOwnerUserIdAndStatusNot(motherId, ReminderStatus.CANCELLED))
                .thenReturn(List.of(reminder));
        when(reminderRecurrenceService.occurrenceForDate(any(), any(), any()))
                .thenReturn(Optional.of(new ReminderRecurrenceService.GeneratedOccurrence(
                        reminder,
                        scheduledAt,
                        scheduledAt,
                        ReminderStatus.PENDING,
                        null)));
        when(sharedDataService.getSharedData(
                        groupId, userId, SharedDataCategory.CALENDAR, 0, Integer.MAX_VALUE))
                .thenReturn(SharedDataResponse.builder().items(List.of()).build());

        var response = service.get(userId, groupId);

        assertThat(response.selectedGroupDetail().motherDisplayName()).isEqualTo("Nguyễn Lan");
        assertThat(response.selectedGroupDetail().todayReminders())
                .singleElement()
                .satisfies(todayReminder -> {
                    assertThat(todayReminder.id()).isEqualTo(reminder.getId());
                    assertThat(todayReminder.title()).isEqualTo("Khám thai");
                    assertThat(todayReminder.type()).isEqualTo("APPOINTMENT");
                });
        verify(reminderRepository)
                .findByOwnerUserIdAndStatusNot(motherId, ReminderStatus.CANCELLED);
    }

    @Test
    void selectedDetailWithoutCalendarPermissionDoesNotExposeMotherReminders() {
        UUID groupId = UUID.randomUUID();
        CareGroupMember membership = membership(groupId, userId, Instant.now());
        stubAcceptedGroups(List.of(membership), Map.of(groupId, group(groupId, "A")));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(
                        groupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(membership));

        var response = service.get(userId, groupId);

        assertThat(response.selectedGroupDetail().todayReminders()).isEmpty();
        verify(reminderRepository, never())
                .findByOwnerUserIdAndStatusNot(motherId, ReminderStatus.CANCELLED);
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

    @Test
    void healthMetricSummariesContainOnlyPermittedCanonicalDataAndSafeGlucoseContext() {
        UUID groupId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        UUID careSubjectId = UUID.randomUUID();
        CareGroupMember membership = membership(groupId, userId, Instant.now());
        CareGroup group = CareGroup.builder()
                .id(groupId).ownerUserId(motherId).groupName("A")
                .status(CareGroupStatus.ACTIVE).linkedJourneyId(journeyId).build();
        stubAcceptedGroups(List.of(membership), Map.of(groupId, group));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(groupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(membership));
        lenient().when(authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.QUICK_NOTES))
                .thenReturn(true);
        lenient().when(authorizationPolicy.hasPermission(
                groupId, userId, PermissionFlag.QUICK_NOTE_BLOOD_PRESSURE)).thenReturn(true);
        lenient().when(authorizationPolicy.hasPermission(
                groupId, userId, PermissionFlag.QUICK_NOTE_BLOOD_GLUCOSE)).thenReturn(true);
        lenient().when(authorizationPolicy.hasPermission(
                groupId, userId, PermissionFlag.QUICK_NOTE_EPDS)).thenReturn(true);
        when(journeyRepository.findById(journeyId)).thenReturn(Optional.of(MotherJourney.builder()
                .id(journeyId).ownerUserId(motherId).careSubjectId(careSubjectId).build()));
        HealthObservation pressure = observation(
                careSubjectId, "BLOOD_PRESSURE", "118", Instant.parse("2026-08-03T02:00:00Z"));
        pressure.setValueSecondary(new BigDecimal("76"));
        HealthObservation glucose = observation(
                careSubjectId, "BLOOD_GLUCOSE", "96", Instant.parse("2026-08-03T03:00:00Z"));
        glucose.setContext(Map.of("measurementContext", "FASTING", "privateField", "secret"));
        HealthObservation epds = observation(
                careSubjectId, "EPDS_SCORE", "12", Instant.parse("2026-08-03T04:00:00Z"));
        epds.setValueSecondary(new BigDecimal("3"));
        epds.setNote("private answers");
        when(observationRepository.findLatestByMetricCodes(
                eq(careSubjectId), anyList(), eq(MetricStatus.ACTIVE)))
                .thenReturn(List.of(pressure, glucose, epds));

        var response = service.get(userId, groupId);

        assertThat(response.selectedGroupDetail().healthMetricSummaries())
                .extracting(summary -> summary.metricType())
                .containsExactly("BLOOD_PRESSURE", "EPDS_SCORE", "BLOOD_GLUCOSE");
        assertThat(response.selectedGroupDetail().healthMetricSummaries().get(0).valueSecondary())
                .isEqualByComparingTo("76");
        assertThat(response.selectedGroupDetail().healthMetricSummaries().get(1).valueSecondary())
                .isNull();
        assertThat(response.selectedGroupDetail().healthMetricSummaries().get(2).measurementContext())
                .isEqualTo("FASTING");
    }

    @Test
    void permittedMetricWithoutObservationProducesHonestEmptySummary() {
        UUID groupId = UUID.randomUUID();
        UUID careSubjectId = UUID.randomUUID();
        CareGroupMember membership = membership(groupId, userId, Instant.now());
        stubAcceptedGroups(List.of(membership), Map.of(groupId, group(groupId, "A")));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(groupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(membership));
        lenient().when(authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.QUICK_NOTES))
                .thenReturn(true);
        lenient().when(authorizationPolicy.hasPermission(
                groupId, userId, PermissionFlag.QUICK_NOTE_WEIGHT)).thenReturn(true);
        when(journeyRepository.findCanonical(motherId)).thenReturn(Optional.of(MotherJourney.builder()
                .id(UUID.randomUUID()).ownerUserId(motherId).careSubjectId(careSubjectId).build()));
        when(observationRepository.findLatestByMetricCodes(
                eq(careSubjectId), anyList(), eq(MetricStatus.ACTIVE))).thenReturn(List.of());

        var summary = service.get(userId, groupId)
                .selectedGroupDetail().healthMetricSummaries().getFirst();

        assertThat(summary.metricType()).isEqualTo("BMI");
        assertThat(summary.valueNumeric()).isNull();
        assertThat(summary.measuredAt()).isNull();
        assertThat(summary.recordCount()).isZero();
    }

    @Test
    void hydrationAndFetalMovementSummariesAggregateOnlyCanonicalTodayRows() {
        UUID groupId = UUID.randomUUID();
        UUID careSubjectId = UUID.randomUUID();
        CareGroupMember membership = membership(groupId, userId, Instant.now());
        stubAcceptedGroups(List.of(membership), Map.of(groupId, group(groupId, "A")));
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(groupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(membership));
        lenient().when(authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.QUICK_NOTES))
                .thenReturn(true);
        lenient().when(authorizationPolicy.hasPermission(
                groupId, userId, PermissionFlag.QUICK_NOTE_HYDRATION)).thenReturn(true);
        lenient().when(authorizationPolicy.hasPermission(
                groupId, userId, PermissionFlag.QUICK_NOTE_FETAL_MOVEMENT)).thenReturn(true);
        when(journeyRepository.findCanonical(motherId)).thenReturn(Optional.of(MotherJourney.builder()
                .id(UUID.randomUUID()).ownerUserId(motherId).careSubjectId(careSubjectId).build()));
        when(observationRepository.findLatestByMetricCodes(
                eq(careSubjectId), anyList(), eq(MetricStatus.ACTIVE))).thenReturn(List.of());
        HealthObservation waterOne = observation(careSubjectId, "HYDRATION", "250", Instant.now());
        waterOne.setUnit("ml");
        HealthObservation waterTwo = observation(careSubjectId, "HYDRATION", "300", Instant.now());
        waterTwo.setUnit("ml");
        HealthObservation movement = observation(
                careSubjectId, "FETAL_MOVEMENT_SESSION", "8", Instant.now());
        movement.setUnit("count");
        when(observationRepository.findTrendByMetricCodes(
                eq(careSubjectId), anyList(), eq(MetricStatus.ACTIVE), any(), any()))
                .thenReturn(List.of(waterOne, waterTwo, movement));

        var summaries = service.get(userId, groupId).selectedGroupDetail().healthMetricSummaries();

        assertThat(summaries).extracting(summary -> summary.metricType())
                .containsExactly("FETAL_MOVEMENT_COUNT", "HYDRATION");
        assertThat(summaries.get(0).valueNumeric()).isEqualByComparingTo("8");
        assertThat(summaries.get(0).recordCount()).isEqualTo(1);
        assertThat(summaries.get(1).valueNumeric()).isEqualByComparingTo("550");
        assertThat(summaries.get(1).recordCount()).isEqualTo(2);
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

    private void allowCalendar(UUID groupId) {
        lenient().when(authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.CALENDAR)).thenReturn(true);
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
        return CareGroup.builder()
                .id(groupId)
                .ownerUserId(motherId)
                .groupName(name)
                .status(CareGroupStatus.ACTIVE)
                .build();
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

    private HealthObservation observation(
            UUID careSubjectId, String metricCode, String value, Instant measuredAt) {
        return HealthObservation.builder()
                .id(UUID.randomUUID())
                .careSubjectId(careSubjectId)
                .metricCode(metricCode)
                .valueNumeric(new BigDecimal(value))
                .unit("BLOOD_PRESSURE".equals(metricCode) ? "mmHg" : "mg/dL")
                .measuredAt(measuredAt)
                .context(Map.of())
                .build();
    }
}
