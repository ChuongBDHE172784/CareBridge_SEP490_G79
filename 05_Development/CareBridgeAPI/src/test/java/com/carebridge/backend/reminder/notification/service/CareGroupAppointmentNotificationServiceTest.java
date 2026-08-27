package com.carebridge.backend.reminder.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.job.entity.NotificationJob;
import com.carebridge.backend.reminder.job.entity.NotificationJobType;
import com.carebridge.backend.reminder.job.repository.NotificationJobRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareGroupAppointmentNotificationServiceTest {

    private static final UUID MOTHER = UUID.fromString("51000000-0000-0000-0000-000000000001");
    private static final UUID FAMILY = UUID.fromString("51000000-0000-0000-0000-000000000002");
    private static final UUID FAMILY_TWO = UUID.fromString("51000000-0000-0000-0000-000000000006");
    private static final UUID GROUP = UUID.fromString("51000000-0000-0000-0000-000000000003");
    private static final UUID JOURNEY = UUID.fromString("51000000-0000-0000-0000-000000000004");

    @Test
    void createdAppointmentFansOutOnlyToCalendarPermittedAcceptedMembers() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupMemberRepository members = mock(CareGroupMemberRepository.class);
        CareGroupAuthorizationPolicy permissions = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        NotificationPreferenceRepository preferences = mock(NotificationPreferenceRepository.class);
        DeviceTokenRepository tokens = mock(DeviceTokenRepository.class);
        NotificationRecordRepository records = mock(NotificationRecordRepository.class);
        FcmService fcm = mock(FcmService.class);
        AuditService audit = mock(AuditService.class);

        CareGroup group = CareGroup.builder().id(GROUP).ownerUserId(MOTHER)
                .linkedJourneyId(JOURNEY).status(CareGroupStatus.ACTIVE).build();
        CareGroupMember family = CareGroupMember.builder().id(UUID.randomUUID())
                .careGroupId(GROUP).userId(FAMILY).memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED).build();
        Reminder appointment = Reminder.builder().id(UUID.randomUUID()).ownerUserId(MOTHER)
                .journeyId(JOURNEY).reminderType(ReminderType.APPOINTMENT)
                .title("Khám thai").scheduledAt(Instant.parse("2026-08-05T02:00:00Z"))
                .status(ReminderStatus.PENDING).build();
        when(groups.findByOwnerUserIdAndStatus(MOTHER, CareGroupStatus.ACTIVE))
                .thenReturn(List.of(group));
        when(members.findByCareGroupIdAndInviteStatusIn(GROUP, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(family));
        when(permissions.hasPermission(GROUP, FAMILY, PermissionFlag.CALENDAR)).thenReturn(true);
        when(journeys.existsByIdAndOwnerUserIdAndStatus(JOURNEY, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(true);
        when(records.findAppointmentEventByRecipientAndGroup(
                FAMILY, GROUP, appointment.getId() + "|CREATED"))
                .thenReturn(Optional.empty());
        when(records.save(any(NotificationRecord.class)))
                .thenAnswer(invocation -> {
                    NotificationRecord record = invocation.getArgument(0);
                    record.setId(UUID.randomUUID());
                    return record;
                });

        CareGroupAppointmentNotificationService service = new CareGroupAppointmentNotificationService(
                groups, members, permissions, journeys, babies, preferences, tokens, records, fcm, audit);
        service.notifyCreated(appointment);

        var captured = org.mockito.ArgumentCaptor.forClass(NotificationRecord.class);
        verify(records).save(captured.capture());
        assertThat(captured.getValue().getUserId()).isEqualTo(FAMILY);
        assertThat(captured.getValue().getCareGroupId()).isEqualTo(GROUP);
        assertThat(captured.getValue().getReferenceType()).isEqualTo("APPOINTMENT");
        assertThat(captured.getValue().getMetadata()).containsEntry("eventType", "CREATED");
    }

    @Test
    void missingCalendarPermissionKeepsAppointmentNotificationPrivate() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupMemberRepository members = mock(CareGroupMemberRepository.class);
        CareGroupAuthorizationPolicy permissions = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        NotificationPreferenceRepository preferences = mock(NotificationPreferenceRepository.class);
        DeviceTokenRepository tokens = mock(DeviceTokenRepository.class);
        NotificationRecordRepository records = mock(NotificationRecordRepository.class);
        FcmService fcm = mock(FcmService.class);
        AuditService audit = mock(AuditService.class);

        CareGroup group = CareGroup.builder().id(GROUP).ownerUserId(MOTHER)
                .linkedJourneyId(JOURNEY).status(CareGroupStatus.ACTIVE).build();
        CareGroupMember family = CareGroupMember.builder().id(UUID.randomUUID())
                .careGroupId(GROUP).userId(FAMILY).memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED).build();
        Reminder appointment = Reminder.builder().id(UUID.randomUUID()).ownerUserId(MOTHER)
                .journeyId(JOURNEY).reminderType(ReminderType.APPOINTMENT)
                .title("Khám thai").scheduledAt(Instant.now()).status(ReminderStatus.PENDING).build();
        when(groups.findByOwnerUserIdAndStatus(MOTHER, CareGroupStatus.ACTIVE)).thenReturn(List.of(group));
        when(members.findByCareGroupIdAndInviteStatusIn(GROUP, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(family));
        when(permissions.hasPermission(GROUP, FAMILY, PermissionFlag.CALENDAR)).thenReturn(false);
        when(journeys.existsByIdAndOwnerUserIdAndStatus(JOURNEY, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(true);

        CareGroupAppointmentNotificationService service = new CareGroupAppointmentNotificationService(
                groups, members, permissions, journeys, babies, preferences, tokens, records, fcm, audit);
        service.notifyCreated(appointment);

        verify(records, never()).save(any(NotificationRecord.class));
    }

    @Test
    void milestoneDeduplicatesFamilyAccountAcrossEligibleGroups() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupMemberRepository members = mock(CareGroupMemberRepository.class);
        CareGroupAuthorizationPolicy permissions = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        NotificationPreferenceRepository preferences = mock(NotificationPreferenceRepository.class);
        DeviceTokenRepository tokens = mock(DeviceTokenRepository.class);
        NotificationRecordRepository records = mock(NotificationRecordRepository.class);
        FcmService fcm = mock(FcmService.class);
        AuditService audit = mock(AuditService.class);
        NotificationJobRepository jobs = mock(NotificationJobRepository.class);

        UUID secondGroupId = UUID.fromString("51000000-0000-0000-0000-000000000005");
        CareGroup first = CareGroup.builder().id(GROUP).ownerUserId(MOTHER)
                .linkedJourneyId(JOURNEY).status(CareGroupStatus.ACTIVE).build();
        CareGroup second = CareGroup.builder().id(secondGroupId).ownerUserId(MOTHER)
                .linkedJourneyId(JOURNEY).status(CareGroupStatus.ACTIVE).build();
        CareGroupMember firstMembership = CareGroupMember.builder().id(UUID.randomUUID())
                .careGroupId(GROUP).userId(FAMILY).memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED).build();
        CareGroupMember secondMembership = CareGroupMember.builder().id(UUID.randomUUID())
                .careGroupId(secondGroupId).userId(FAMILY).memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED).build();
        Reminder appointment = appointment();
        NotificationJob job = NotificationJob.builder()
                .jobType(NotificationJobType.APPOINTMENT)
                .id(UUID.fromString("51000000-0000-0000-0000-000000000007"))
                .offsetMinutes(-30)
                .occurrenceScheduledAt(appointment.getScheduledAt())
                .build();
        when(jobs.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));

        when(groups.findByOwnerUserIdAndStatus(MOTHER, CareGroupStatus.ACTIVE))
                .thenReturn(List.of(second, first));
        when(members.findByCareGroupIdAndInviteStatusIn(GROUP, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(firstMembership));
        when(members.findByCareGroupIdAndInviteStatusIn(secondGroupId, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(secondMembership));
        when(permissions.hasPermission(GROUP, FAMILY, PermissionFlag.CALENDAR)).thenReturn(true);
        when(permissions.hasPermission(secondGroupId, FAMILY, PermissionFlag.CALENDAR)).thenReturn(true);
        when(journeys.existsByIdAndOwnerUserIdAndStatus(JOURNEY, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(true);
        when(records.findAppointmentMilestoneByRecipientAndJobShared(FAMILY, job.getId()))
                .thenReturn(Optional.empty());
        when(records.save(any(NotificationRecord.class))).thenAnswer(invocation -> {
            NotificationRecord record = invocation.getArgument(0);
            record.setId(UUID.randomUUID());
            return record;
        });

        CareGroupAppointmentNotificationService service = new CareGroupAppointmentNotificationService(
                groups, members, permissions, journeys, babies, preferences, tokens, records, fcm, audit, jobs);
        service.notifyMilestone(appointment, job, "UTC");

        var captured = org.mockito.ArgumentCaptor.forClass(NotificationRecord.class);
        verify(records, times(1)).save(captured.capture());
        assertThat(captured.getValue().getUserId()).isEqualTo(FAMILY);
        assertThat(captured.getValue().getCareGroupId()).isEqualTo(GROUP);
    }

    @Test
    void milestoneProviderFailureForOneFamilyTokenDoesNotBlockAnotherFamilyAccount() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupMemberRepository members = mock(CareGroupMemberRepository.class);
        CareGroupAuthorizationPolicy permissions = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        NotificationPreferenceRepository preferences = mock(NotificationPreferenceRepository.class);
        DeviceTokenRepository tokens = mock(DeviceTokenRepository.class);
        NotificationRecordRepository records = mock(NotificationRecordRepository.class);
        FcmService fcm = mock(FcmService.class);
        AuditService audit = mock(AuditService.class);
        NotificationJobRepository jobs = mock(NotificationJobRepository.class);

        CareGroup group = CareGroup.builder().id(GROUP).ownerUserId(MOTHER)
                .linkedJourneyId(JOURNEY).status(CareGroupStatus.ACTIVE).build();
        CareGroupMember first = CareGroupMember.builder().id(UUID.randomUUID())
                .careGroupId(GROUP).userId(FAMILY).memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED).build();
        CareGroupMember second = CareGroupMember.builder().id(UUID.randomUUID())
                .careGroupId(GROUP).userId(FAMILY_TWO).memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED).build();
        Reminder appointment = appointment();
        NotificationJob job = NotificationJob.builder()
                .jobType(NotificationJobType.APPOINTMENT)
                .id(UUID.fromString("51000000-0000-0000-0000-000000000008"))
                .offsetMinutes(-30)
                .occurrenceScheduledAt(appointment.getScheduledAt())
                .build();
        when(jobs.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));

        when(groups.findByOwnerUserIdAndStatus(MOTHER, CareGroupStatus.ACTIVE)).thenReturn(List.of(group));
        when(members.findByCareGroupIdAndInviteStatusIn(GROUP, List.of(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(first, second));
        when(permissions.hasPermission(GROUP, FAMILY, PermissionFlag.CALENDAR)).thenReturn(true);
        when(permissions.hasPermission(GROUP, FAMILY_TWO, PermissionFlag.CALENDAR)).thenReturn(true);
        when(journeys.existsByIdAndOwnerUserIdAndStatus(JOURNEY, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(true);
        when(preferences.isPushEnabled(FAMILY, com.carebridge.backend.notification.entity.NotificationType.REMINDER))
                .thenReturn(true);
        when(preferences.isPushEnabled(FAMILY_TWO, com.carebridge.backend.notification.entity.NotificationType.REMINDER))
                .thenReturn(true);
        when(tokens.findByUserIdAndActiveTrue(FAMILY))
                .thenReturn(List.of(DeviceToken.builder().userId(FAMILY).token("token-a").build()));
        when(tokens.findByUserIdAndActiveTrue(FAMILY_TWO))
                .thenReturn(List.of(DeviceToken.builder().userId(FAMILY_TWO).token("token-b").build()));
        when(fcm.isReady()).thenReturn(true);
        when(fcm.sendWithRetry(eq("token-a"), any(), any(), any(java.util.Map.class), eq(3)))
                .thenThrow(new IllegalStateException("token failed"));
        when(fcm.sendWithRetry(eq("token-b"), any(), any(), any(java.util.Map.class), eq(3)))
                .thenReturn(FcmDeliveryResult.success("message-b", 1));
        when(records.findAppointmentMilestoneByRecipientAndJobShared(any(), eq(job.getId())))
                .thenReturn(Optional.empty());
        when(records.save(any(NotificationRecord.class))).thenAnswer(invocation -> {
            NotificationRecord record = invocation.getArgument(0);
            record.setId(UUID.randomUUID());
            return record;
        });

        CareGroupAppointmentNotificationService service = new CareGroupAppointmentNotificationService(
                groups, members, permissions, journeys, babies, preferences, tokens, records, fcm, audit, jobs);
        service.notifyMilestone(appointment, job, "UTC");

        verify(records, times(2)).save(any(NotificationRecord.class));
        verify(fcm).sendWithRetry(eq("token-b"), any(), any(), any(java.util.Map.class), eq(3));
    }

    private Reminder appointment() {
        return Reminder.builder().id(UUID.randomUUID()).ownerUserId(MOTHER)
                .journeyId(JOURNEY).reminderType(ReminderType.APPOINTMENT)
                .title("Khám thai").scheduledAt(Instant.parse("2026-08-05T02:00:00Z"))
                .status(ReminderStatus.PENDING).build();
    }
}
