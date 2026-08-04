package com.carebridge.backend.reminder.schedule.job;

import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.reminder.schedule.service.ReminderScheduleProcessingService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduleWorker {
    private static final Logger log = LoggerFactory.getLogger(ReminderScheduleWorker.class);
    private final ReminderScheduleProcessingService processingService;
    private final FcmService fcmService;
    private final boolean enabled;
    private final int batchSize;
    private final String workerId = "reminder-schedule-" + UUID.randomUUID();

    public ReminderScheduleWorker(
            ReminderScheduleProcessingService processingService,
            FcmService fcmService,
            @Value("${carebridge.notification.reminder-schedule.enabled:false}") boolean enabled,
            @Value("${carebridge.notification.reminder-schedule.batch-size:25}") int batchSize) {
        this.processingService = processingService;
        this.fcmService = fcmService;
        this.enabled = enabled;
        this.batchSize = Math.max(1, batchSize);
        log.info("Reminder-schedule worker initialized: workerId={}, enabled={}, batchSize={}",
                workerId, enabled, this.batchSize);
    }

    @Scheduled(fixedDelayString = "${carebridge.notification.reminder-schedule.worker-delay-ms:15000}")
    public void poll() {
        if (!enabled) {
            log.debug("Reminder-schedule worker disabled: workerId={}", workerId);
            return;
        }
        if (!fcmService.isReady()) {
            log.warn("Reminder-schedule worker unavailable: workerId={}, reason=FCM_NOT_READY", workerId);
            return;
        }
        for (UUID jobId : processingService.claimDueJobs(workerId, batchSize)) {
            processingService.processAsync(jobId, workerId);
        }
    }
}
