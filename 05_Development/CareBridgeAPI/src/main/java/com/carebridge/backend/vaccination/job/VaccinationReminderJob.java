package com.carebridge.backend.vaccination.job;

import com.carebridge.backend.vaccination.service.IVaccinationReminderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily sweep of the vaccination book. Runs once each morning in the mother's zone; the
 * per-milestone idempotency key in the notification record makes extra runs harmless.
 */
@Component
@RequiredArgsConstructor
public class VaccinationReminderJob {

    private static final Logger log = LoggerFactory.getLogger(VaccinationReminderJob.class);

    private final IVaccinationReminderService reminderService;

    @Scheduled(
            cron = "${carebridge.vaccination.reminder.cron:0 0 7 * * *}",
            zone = "${carebridge.vaccination.reminder.zone:Asia/Ho_Chi_Minh}")
    public void dispatchDueReminders() {
        int dispatched = reminderService.dispatchDueReminders();
        if (dispatched > 0) {
            log.info("Dispatched {} vaccination reminder(s)", dispatched);
        }
    }
}
