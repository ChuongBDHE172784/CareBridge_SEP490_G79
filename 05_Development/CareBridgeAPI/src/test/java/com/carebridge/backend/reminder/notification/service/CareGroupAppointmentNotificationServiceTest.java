package com.carebridge.backend.reminder.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareGroupAppointmentNotificationServiceTest {

    private static final UUID MOTHER = UUID.fromString("51000000-0000-0000-0000-000000000001");
    private static final UUID FAMILY = UUID.fromString("51000000-0000-0000-0000-000000000002");
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
}
