package com.carebridge.backend.reminder.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.reminder.appointment.policy.CareGroupAppointmentScopeResolver;
import com.carebridge.backend.reminder.appointment.policy.CareGroupAppointmentScopeResolver.AppointmentScope;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.notification.service.AppointmentNotificationScheduleService;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareGroupAppointmentServiceTest {

    private static final UUID FAMILY = UUID.fromString("53000000-0000-0000-0000-000000000001");
    private static final UUID MOTHER = UUID.fromString("53000000-0000-0000-0000-000000000002");
    private static final UUID GROUP = UUID.fromString("53000000-0000-0000-0000-000000000003");
    private static final UUID JOURNEY = UUID.fromString("53000000-0000-0000-0000-000000000004");

    @Test
    void listProjectsOnlyTheResolvedMotherContext() {
        ReminderRepository reminders = mock(ReminderRepository.class);
        CareGroupAppointmentScopeResolver resolver = mock(CareGroupAppointmentScopeResolver.class);
        AppointmentNotificationScheduleService schedules = mock(AppointmentNotificationScheduleService.class);
        AppointmentScope scope = new AppointmentScope(
                GROUP, "Family", MOTHER,
                List.of(new CareGroupAppointmentScopeResolver.LinkedContext(
                        com.carebridge.backend.checklist.model.ChecklistCareContextType.JOURNEY,
                        JOURNEY)));
        Reminder appointment = Reminder.builder().id(UUID.randomUUID()).ownerUserId(MOTHER)
                .reminderType(ReminderType.APPOINTMENT).title("Khám thai")
                .scheduledAt(Instant.parse("2026-08-05T02:00:00Z"))
                .status(ReminderStatus.PENDING).build();
        when(resolver.resolveView(FAMILY, GROUP)).thenReturn(scope, scope);
        when(reminders.findSharedAppointments(MOTHER, JOURNEY, null)).thenReturn(List.of(appointment));
        when(schedules.currentOffsets(appointment.getId())).thenReturn(List.of(-60));
        when(schedules.currentTimeZone(appointment.getId())).thenReturn("Asia/Ho_Chi_Minh");

        var response = new CareGroupAppointmentService(reminders, resolver, schedules)
                .list(FAMILY, GROUP);

        assertThat(response).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(appointment.getId());
            assertThat(item.careGroupId()).isEqualTo(GROUP);
            assertThat(item.reminderType()).isEqualTo("APPOINTMENT");
            assertThat(item.notificationOffsetsMinutes()).containsExactly(-60);
        });
        verify(reminders).findSharedAppointments(MOTHER, JOURNEY, null);
    }
}
