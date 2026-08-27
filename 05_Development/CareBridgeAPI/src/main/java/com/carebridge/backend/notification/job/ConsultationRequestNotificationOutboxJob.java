package com.carebridge.backend.notification.job;

import com.carebridge.backend.notification.service.IConsultationRequestNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsultationRequestNotificationOutboxJob {

    private final IConsultationRequestNotificationService service;

    @Scheduled(fixedDelayString = "${carebridge.notification.consultation-request-outbox-delay-ms:5000}")
    public void dispatchPending() {
        service.retryPendingNotifications();
    }
}
