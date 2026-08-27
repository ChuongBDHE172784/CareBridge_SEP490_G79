package com.carebridge.backend.consultation.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import com.carebridge.backend.consultation.repository.ConsultationRequestRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.notification.service.IConsultationRequestNotificationService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultationRequestNotificationListenerTest {

    @Mock private ConsultationRequestRepository requestRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private IConsultationRequestNotificationService notificationService;

    @Test
    void routesLifecycleEventsToTheCorrectParticipant() {
        UUID requestId = UUID.randomUUID();
        UUID motherId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        ConsultationRequest request = ConsultationRequest.builder()
                .id(requestId)
                .requesterUserId(motherId)
                .expertProfileId(profileId)
                .clientRequestId(UUID.randomUUID())
                .topic("Nutrition")
                .description("Description")
                .status(ConsultationRequestStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(expertProfileRepository.findById(profileId))
                .thenReturn(Optional.of(ExpertProfile.builder()
                        .expertProfileId(profileId)
                        .userId(expertId)
                        .build()));
        var listener = new ConsultationRequestNotificationListener(
                requestRepository, expertProfileRepository, notificationService);

        listener.onRequestEvent(event("REQUEST_CREATED", requestId));
        listener.onRequestEvent(event("REQUEST_ACCEPTED", requestId));
        listener.onRequestEvent(event("REQUEST_REJECTED", requestId));
        listener.onRequestEvent(event("REQUEST_CANCELLED", requestId));
        listener.onRequestEvent(event("REQUEST_EXPIRED", requestId));

        verify(notificationService).notifyCreated(expertId, motherId, requestId);
        verify(notificationService).notifyAccepted(motherId, expertId, requestId);
        verify(notificationService).notifyRejected(motherId, expertId, requestId);
        verify(notificationService).notifyCancelled(expertId, motherId, requestId);
        verify(notificationService).notifyExpired(motherId, expertId, requestId);
    }

    @Test
    void missingRequestProducesNoNotification() {
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());
        var listener = new ConsultationRequestNotificationListener(
                requestRepository, expertProfileRepository, notificationService);

        listener.onRequestEvent(event("REQUEST_CREATED", requestId));

        verifyNoInteractions(expertProfileRepository, notificationService);
    }

    private static ConsultationRequestDomainEvent event(String type, UUID requestId) {
        return new ConsultationRequestDomainEvent(
                type, requestId, UUID.randomUUID(), "USER", Instant.now());
    }
}
