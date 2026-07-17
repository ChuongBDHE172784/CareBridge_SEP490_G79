package com.carebridge.backend.consultation.event;

import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.repository.ConsultationRequestRepository;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.notification.service.IConsultationRequestNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsultationRequestNotificationListener {

    private final ConsultationRequestRepository requestRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final IConsultationRequestNotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestEvent(ConsultationRequestDomainEvent event) {
        try {
            ConsultationRequest request = requestRepository.findById(event.requestId()).orElse(null);
            if (request == null) {
                return;
            }
            var expert = expertProfileRepository.findById(request.getExpertProfileId()).orElse(null);
            if (expert == null) {
                return;
            }
            switch (event.eventType()) {
                case "REQUEST_CREATED" -> notificationService.notifyCreated(
                        expert.getUserId(), request.getRequesterUserId(), request.getId());
                case "REQUEST_ACCEPTED" -> notificationService.notifyAccepted(
                        request.getRequesterUserId(), expert.getUserId(), request.getId());
                case "REQUEST_REJECTED" -> notificationService.notifyRejected(
                        request.getRequesterUserId(), expert.getUserId(), request.getId());
                case "REQUEST_CANCELLED" -> notificationService.notifyCancelled(
                        expert.getUserId(), request.getRequesterUserId(), request.getId());
                case "REQUEST_EXPIRED" -> notificationService.notifyExpired(
                        request.getRequesterUserId(), expert.getUserId(), request.getId());
                default -> log.warn("Ignoring unsupported consultation request event type");
            }
        } catch (RuntimeException ex) {
            log.error("Failed to process consultation request notification {}", event.requestId(), ex);
        }
    }
}
