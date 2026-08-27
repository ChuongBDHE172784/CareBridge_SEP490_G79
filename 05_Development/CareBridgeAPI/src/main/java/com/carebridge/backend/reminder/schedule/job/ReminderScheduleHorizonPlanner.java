package com.carebridge.backend.reminder.schedule.job;

import com.carebridge.backend.reminder.schedule.entity.ReminderScheduleRecurrence;
import com.carebridge.backend.reminder.schedule.repository.ReminderScheduleRepository;
import com.carebridge.backend.reminder.schedule.service.ReminderScheduleServiceImpl;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily horizon extension is deliberately gated with the same worker flag. */
@Component
public class ReminderScheduleHorizonPlanner {
    private final ReminderScheduleRepository scheduleRepository;
    private final ReminderScheduleServiceImpl scheduleService;
    private final boolean enabled;

    public ReminderScheduleHorizonPlanner(
            ReminderScheduleRepository scheduleRepository,
            ReminderScheduleServiceImpl scheduleService,
            @Value("${carebridge.notification.reminder-schedule.enabled:false}") boolean enabled) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleService = scheduleService;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${carebridge.notification.reminder-schedule.planner-cron:0 20 2 * * *}")
    public void extendDailySchedules() {
        if (!enabled) return;
        scheduleRepository.findByActiveTrueAndRecurrence(ReminderScheduleRecurrence.DAILY)
                .forEach(scheduleService::materializeForPlanner);
    }
}
