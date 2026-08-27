package com.carebridge.backend.notification.job;

import com.carebridge.backend.notification.service.impl.DirectMessageNotificationServiceImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Retries durable MESSAGE outbox rows that were committed before an interrupted dispatch. */
@Component
public class DirectMessageNotificationOutboxJob {

    private final DirectMessageNotificationServiceImpl service;

    public DirectMessageNotificationOutboxJob(DirectMessageNotificationServiceImpl service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${carebridge.notification.message-outbox-delay-ms:5000}")
    public void dispatchPending() {
        service.retryPendingNotifications();
    }
}
