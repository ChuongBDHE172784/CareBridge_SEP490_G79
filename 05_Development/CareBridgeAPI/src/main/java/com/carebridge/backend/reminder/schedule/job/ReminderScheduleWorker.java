package com.carebridge.backend.reminder.schedule.job;

import com.carebridge.backend.reminder.schedule.service.ReminderScheduleProcessingService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduleWorker {
    private final ReminderScheduleProcessingService processingService;
    private final boolean enabled;
    private final int batchSize;
    private final String workerId = "reminder-schedule-" + UUID.randomUUID();

    public ReminderScheduleWorker(
            ReminderScheduleProcessingService processingService,
            @Value("${carebridge.notification.reminder-schedule.enabled:false}") boolean enabled,
            @Value("${carebridge.notification.reminder-schedule.batch-size:25}") int batchSize) {
        this.processingService = processingService;
        this.enabled = enabled;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(fixedDelayString = "${carebridge.notification.reminder-schedule.worker-delay-ms:15000}")
    public void poll() {
        if (!enabled) return;
        for (UUID jobId : processingService.claimDueJobs(workerId, batchSize)) {
            processingService.processAsync(jobId, workerId);
        }
    }
}
