package com.carebridge.backend.emergency.service.impl;

import com.carebridge.backend.emergency.EmergencyStatus;
import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.service.IEmergencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class EmergencyService implements IEmergencyService {

    private final IEmergencySessionRepository emergencySessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public EmergencySessionResponse openFlow(OpenEmergencyRequest request, UUID userId) {
        // UC62 C3: idempotent — return existing ACTIVE session if one exists
        return emergencySessionRepository.findActiveByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    EmergencySession session = EmergencySession.builder()
                            .userId(userId)
                            .status(EmergencyStatus.ACTIVE)
                            .triggerSource(request.getTriggerSource())
                            .userLatitude(request.getUserLatitude())   // UC62 C2: optional, may be null
                            .userLongitude(request.getUserLongitude()) // UC62 C2: optional, may be null
                            .createdAt(Instant.now())
                            .createdBy(userId)
                            .build();
                    EmergencySession saved = emergencySessionRepository.save(session);
                    // UC62 C5: publish event after save
                    eventPublisher.publishEvent(new EmergencySessionOpened(
                            UUID.randomUUID(), saved.getId(), userId,
                            request.getTriggerSource(),
                            request.getUserLatitude(), request.getUserLongitude(),
                            saved.getCreatedAt()));
                    return toResponse(saved);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencySessionResponse getActiveSession(UUID userId) {
        return emergencySessionRepository.findActiveByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new EmergencyException(HttpStatus.NOT_FOUND, "EMERG-003",
                        "No active emergency session found"));
    }

    @Override
    public EmergencySessionResponse resolveSession(UUID sessionId, UUID userId) {
        EmergencySession session = emergencySessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new EmergencyException(HttpStatus.NOT_FOUND, "EMERG-003",
                        "Emergency session not found: " + sessionId));
        session.setStatus(EmergencyStatus.RESOLVED);
        session.setResolvedAt(Instant.now());
        return toResponse(emergencySessionRepository.save(session));
    }

    private EmergencySessionResponse toResponse(EmergencySession session) {
        return EmergencySessionResponse.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .status(session.getStatus().name())
                .triggerSource(session.getTriggerSource())
                .userLatitude(session.getUserLatitude())
                .userLongitude(session.getUserLongitude())
                .createdAt(session.getCreatedAt())
                .resolvedAt(session.getResolvedAt())
                .build();
    }
}
