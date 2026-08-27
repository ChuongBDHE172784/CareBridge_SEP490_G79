package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskCadence;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.policy.ReminderAccessPolicy;
import com.carebridge.backend.checklist.today.provider.ReminderTodayTaskProvider;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReminderTodayTaskProviderTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID FAMILY = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID JOURNEY = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID GROUP = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Test
    void mapsOwnedReminderToUnifiedCandidateWithStableMetadataAndActions() {
        ReminderRepository reminders = mock(ReminderRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        UUID reminderId = UUID.fromString("00000000-0000-0000-0000-000000000401");
        Reminder reminder = Reminder.builder()
                .id(reminderId)
                .ownerUserId(OWNER)
                .journeyId(JOURNEY)
                .title("Appointment")
                .reminderType(ReminderType.APPOINTMENT)
                .scheduledAt(Instant.parse("2026-08-03T01:00:00Z"))
                .status(ReminderStatus.PENDING)
                .build();
        when(reminders.findByOwnerUserIdAndStatusNot(OWNER, ReminderStatus.CANCELLED))
                .thenReturn(List.of(reminder));
        when(groups.findByLinkedJourneyId(JOURNEY)).thenReturn(List.of(
                CareGroup.builder().id(GROUP).ownerUserId(OWNER).linkedJourneyId(JOURNEY).build()));

        var candidates = new ReminderTodayTaskProvider(reminders, groups).findAuthorizedTasks(OWNER);

        assertThat(candidates).singleElement().satisfies(candidate -> {
            assertThat(candidate.taskKind()).isEqualTo(TaskKind.REMINDER);
            assertThat(candidate.taskId()).isEqualTo(UUID.nameUUIDFromBytes(
                    ("reminder-occurrence-v1|" + reminderId + "|2026-08-03T01:00:00Z")
                            .getBytes(StandardCharsets.UTF_8)));
            assertThat(candidate.taskId()).isNotEqualTo(reminderId);
            assertThat(candidate.careGroupId()).isEqualTo(GROUP);
            assertThat(candidate.careContextType()).isEqualTo(ChecklistCareContextType.JOURNEY);
            assertThat(candidate.careContextId()).isEqualTo(JOURNEY);
            assertThat(candidate.targetSubject()).isEqualTo(ChecklistTargetSubject.MOTHER);
            assertThat(candidate.origin()).isEqualTo(ChecklistOrigin.USER_CREATED);
            assertThat(candidate.reminderType()).isEqualTo(ReminderType.APPOINTMENT);
            assertThat(candidate.allowedActions()).containsExactlyInAnyOrder(TaskAction.COMPLETE, TaskAction.SKIP);
        });
    }

    @Test
    void mapsReminderRecurrenceToPresentationCadence() {
        ReminderRepository reminders = mock(ReminderRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        Reminder reminder = Reminder.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000406"))
                .ownerUserId(OWNER)
                .title("Daily medication")
                .scheduledAt(Instant.parse("2026-08-03T01:00:00Z"))
                .recurrenceType(RecurrenceType.DAILY)
                .status(ReminderStatus.PENDING)
                .build();
        when(reminders.findByOwnerUserIdAndStatusNot(OWNER, ReminderStatus.CANCELLED))
                .thenReturn(List.of(reminder));

        assertThat(new ReminderTodayTaskProvider(reminders, groups)
                .findAuthorizedTasks(OWNER)).singleElement()
                .extracting(candidate -> candidate.cadence())
                .isEqualTo(TaskCadence.DAILY);
    }

    @Test
    void mapsMotherOwnedCurrentContextReminderForPermittedFamilyExactlyOnce() {
        ReminderRepository reminders = mock(ReminderRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy authorization = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        ReminderAccessPolicy access = new ReminderAccessPolicy(
                groups, authorization, journeys, mock(BabyProfileRepository.class));
        Reminder reminder = Reminder.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000499"))
                .ownerUserId(OWNER)
                .journeyId(JOURNEY)
                .title("Shared appointment")
                .scheduledAt(Instant.parse("2026-08-03T01:00:00Z"))
                .status(ReminderStatus.PENDING)
                .build();
        CareGroup group = CareGroup.builder()
                .id(GROUP).ownerUserId(OWNER).linkedJourneyId(JOURNEY)
                .status(CareGroupStatus.ACTIVE).build();
        when(reminders.findByOwnerUserIdAndStatusNot(FAMILY, ReminderStatus.CANCELLED))
                .thenReturn(List.of());
        when(groups.findActiveOwnerUserIdsForChecklistViewer(FAMILY)).thenReturn(List.of(OWNER));
        when(reminders.findByOwnerUserIdAndStatusNot(OWNER, ReminderStatus.CANCELLED))
                .thenReturn(List.of(reminder));
        when(groups.findByLinkedJourneyId(JOURNEY)).thenReturn(List.of(group));
        when(journeys.existsByIdAndOwnerUserId(JOURNEY, OWNER)).thenReturn(true);
        when(authorization.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW))
                .thenReturn(true);
        when(authorization.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_COMPLETE))
                .thenReturn(true);

        var candidates = new ReminderTodayTaskProvider(reminders, groups, access)
                .findAuthorizedTasks(FAMILY);

        assertThat(candidates).singleElement().satisfies(candidate -> {
            assertThat(candidate.careGroupId()).isEqualTo(GROUP);
            assertThat(candidate.careContextType()).isEqualTo(ChecklistCareContextType.JOURNEY);
            assertThat(candidate.careContextId()).isEqualTo(JOURNEY);
            assertThat(candidate.allowedActions())
                    .containsExactlyInAnyOrder(TaskAction.COMPLETE, TaskAction.SKIP);
        });
    }

    @Test
    void excludesForeignOwnedJourneyEvenWhenGroupAndReminderOwnersMatch() {
        ReminderRepository reminders = mock(ReminderRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy authorization = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        ReminderAccessPolicy access = new ReminderAccessPolicy(
                groups, authorization, journeys, mock(BabyProfileRepository.class));
        Reminder reminder = Reminder.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000498"))
                .ownerUserId(OWNER).journeyId(JOURNEY).title("Foreign journey")
                .scheduledAt(Instant.parse("2026-08-03T01:00:00Z"))
                .status(ReminderStatus.PENDING).build();
        CareGroup group = CareGroup.builder()
                .id(GROUP).ownerUserId(OWNER).linkedJourneyId(JOURNEY)
                .status(CareGroupStatus.ACTIVE).build();
        when(reminders.findByOwnerUserIdAndStatusNot(FAMILY, ReminderStatus.CANCELLED))
                .thenReturn(List.of());
        when(groups.findActiveOwnerUserIdsForChecklistViewer(FAMILY)).thenReturn(List.of(OWNER));
        when(reminders.findByOwnerUserIdAndStatusNot(OWNER, ReminderStatus.CANCELLED))
                .thenReturn(List.of(reminder));
        when(groups.findByLinkedJourneyId(JOURNEY)).thenReturn(List.of(group));
        when(journeys.existsByIdAndOwnerUserId(JOURNEY, OWNER)).thenReturn(false);
        when(authorization.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW))
                .thenReturn(true);
        when(authorization.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_COMPLETE))
                .thenReturn(true);

        assertThat(new ReminderTodayTaskProvider(reminders, groups, access)
                .findAuthorizedTasks(FAMILY)).isEmpty();
    }

    @Test
    void familyDiscoveryDoesNotEnumerateAllActiveGroups() {
        ReminderRepository reminders = mock(ReminderRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        ReminderAccessPolicy access = mock(ReminderAccessPolicy.class);
        when(reminders.findByOwnerUserIdAndStatusNot(FAMILY, ReminderStatus.CANCELLED))
                .thenReturn(List.of());

        assertThat(new ReminderTodayTaskProvider(reminders, groups, access)
                .findAuthorizedTasks(FAMILY)).isEmpty();

        verify(groups, never()).findByStatus(CareGroupStatus.ACTIVE);
    }

    @Test
    void keepsTerminalReminderVisibleForDueTodayButDoesNotAdvertiseActions() {
        ReminderRepository reminders = mock(ReminderRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        Reminder reminder = Reminder.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000402"))
                .ownerUserId(OWNER)
                .title("Medication")
                .scheduledAt(Instant.parse("2026-08-03T01:00:00Z"))
                .status(ReminderStatus.COMPLETED)
                .build();
        when(reminders.findByOwnerUserIdAndStatusNot(OWNER, ReminderStatus.CANCELLED))
                .thenReturn(List.of(reminder));

        var candidates = new ReminderTodayTaskProvider(reminders, groups).findAuthorizedTasks(OWNER);

        assertThat(candidates).singleElement().satisfies(candidate -> {
            assertThat(candidate.status()).isEqualTo("COMPLETED");
            assertThat(candidate.allowedActions()).isEmpty();
            assertThat(candidate.careGroupId()).isNull();
        });
    }

    @Test
    void snoozeMovesDueAtWithoutChangingThePublicOccurrenceIdentity() {
        ReminderRepository reminders = mock(ReminderRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        UUID reminderId = UUID.fromString("00000000-0000-0000-0000-000000000403");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        Instant snoozedUntil = Instant.parse("2026-08-03T03:00:00Z");
        Reminder reminder = Reminder.builder()
                .id(reminderId)
                .ownerUserId(OWNER)
                .title("Medication")
                .scheduledAt(scheduledAt)
                .status(ReminderStatus.PENDING)
                .build();
        when(reminders.findByOwnerUserIdAndStatusNot(OWNER, ReminderStatus.CANCELLED))
                .thenReturn(List.of(reminder));
        var provider = new ReminderTodayTaskProvider(reminders, groups);

        var beforeSnooze = provider.findAuthorizedTasks(OWNER).getFirst();
        reminder.setStatus(ReminderStatus.SNOOZED);
        reminder.setSnoozedUntil(snoozedUntil);
        var afterSnooze = provider.findAuthorizedTasks(OWNER).getFirst();

        assertThat(afterSnooze.taskId()).isEqualTo(beforeSnooze.taskId());
        assertThat(afterSnooze.dueAt()).isEqualTo(snoozedUntil);
    }

    @Test
    void legacyNullScheduleUsesDefinitionIdentityWithoutAbortingOtherTodayTasks() {
        ReminderRepository reminders = mock(ReminderRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        UUID legacyId = UUID.fromString("00000000-0000-0000-0000-000000000404");
        UUID scheduledId = UUID.fromString("00000000-0000-0000-0000-000000000405");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        Reminder legacy = Reminder.builder()
                .id(legacyId)
                .ownerUserId(OWNER)
                .title("Legacy reminder")
                .status(ReminderStatus.PENDING)
                .build();
        Reminder scheduled = Reminder.builder()
                .id(scheduledId)
                .ownerUserId(OWNER)
                .title("Scheduled reminder")
                .scheduledAt(scheduledAt)
                .status(ReminderStatus.PENDING)
                .build();
        when(reminders.findByOwnerUserIdAndStatusNot(OWNER, ReminderStatus.CANCELLED))
                .thenReturn(List.of(legacy, scheduled));

        var candidates = new ReminderTodayTaskProvider(reminders, groups).findAuthorizedTasks(OWNER);

        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(candidate -> candidate.taskId())
                .contains(legacyId, UUID.nameUUIDFromBytes(
                        ("reminder-occurrence-v1|" + scheduledId + "|" + scheduledAt)
                                .getBytes(StandardCharsets.UTF_8)));
    }
}
