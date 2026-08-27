package com.carebridge.backend.reminder.notification.job;

import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.notification.service.AppointmentNotificationScheduleService;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AppointmentNotificationHorizonPlanner {

    private static final List<ReminderStatus> ACTIVE_STATUSES =
            List.of(ReminderStatus.PENDING, ReminderStatus.SNOOZED);

    private final ReminderRepository reminderRepository;
    private final AppointmentNotificationScheduleService scheduleService;
    private final boolean enabled;

    public AppointmentNotificationHorizonPlanner(
            ReminderRepository reminderRepository,
            AppointmentNotificationScheduleService scheduleService,
            @Value("${carebridge.notification.appointment.enabled:false}") boolean enabled) {
        this.reminderRepository = reminderRepository;
        this.scheduleService = scheduleService;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${carebridge.notification.appointment.planner-cron:0 15 2 * * *}")
    public void extendRecurringHorizons() {
        if (!enabled) return;
        reminderRepository.findByReminderTypeAndStatusIn(ReminderType.APPOINTMENT, ACTIVE_STATUSES)
                .stream()
                .filter(reminder -> reminder.getRecurrenceType() != null)
                .filter(reminder -> reminder.getRecurrenceType() != RecurrenceType.NONE)
                .forEach(scheduleService::extendHorizon);
    }
}
