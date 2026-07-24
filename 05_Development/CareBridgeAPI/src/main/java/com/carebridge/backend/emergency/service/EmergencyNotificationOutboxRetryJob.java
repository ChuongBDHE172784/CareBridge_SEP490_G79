package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.repository.EmergencyNotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EmergencyNotificationOutboxRetryJob {

    private final EmergencyNotificationOutboxRepository outboxRepository;
    private final EmergencyNotificationOutboxDeliveryService deliveryService;

    @Scheduled(fixedDelayString = "${carebridge.emergency.notification-outbox-delay-ms:5000}")
    public void dispatchPending() {
        for (var sessionId : outboxRepository.findDueSessionIds(Instant.now())) {
            deliveryService.deliver(sessionId);
        }
    }
}
