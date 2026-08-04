package com.carebridge.backend.reminder.notification.job;

import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.reminder.notification.service.AppointmentNotificationProcessingService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AppointmentNotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationWorker.class);

    private final AppointmentNotificationProcessingService processingService;
    private final FcmService fcmService;
    private final boolean enabled;
    private final int batchSize;
    private final String workerId = "appointment-notification-" + UUID.randomUUID();

    public AppointmentNotificationWorker(
            AppointmentNotificationProcessingService processingService,
            FcmService fcmService,
            @Value("${carebridge.notification.appointment.enabled:false}") boolean enabled,
            @Value("${carebridge.notification.appointment.batch-size:25}") int batchSize) {
        this.processingService = processingService;
        this.fcmService = fcmService;
        this.enabled = enabled;
        this.batchSize = Math.max(1, batchSize);
        log.info("Appointment notification worker initialized: workerId={}, enabled={}, batchSize={}",
                workerId, enabled, this.batchSize);
    }

    @Scheduled(fixedDelayString = "${carebridge.notification.appointment.worker-delay-ms:15000}")
    public void poll() {
        if (!enabled) {
            log.debug("Appointment notification worker disabled: workerId={}", workerId);
            return;
        }
        if (!fcmService.isReady()) {
            log.warn("Appointment notification worker unavailable: workerId={}, reason=FCM_NOT_READY", workerId);
            return;
        }
        for (UUID jobId : processingService.claimDueJobs(workerId, batchSize)) {
            processingService.processAsync(jobId, workerId);
        }
    }
}
