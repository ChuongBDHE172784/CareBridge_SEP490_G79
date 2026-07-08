package com.carebridge.backend.emergency.service.impl;

import com.carebridge.backend.emergency.EmergencyStatus;
import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import com.carebridge.backend.emergency.dto.response.FamilyAlertDetailResponse;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.entity.FamilyAlertLog;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
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
    private final IFamilyAlertLogRepository familyAlertLogRepository;
    private final FamilyMemberPort familyMemberPort;
    private final LocationConsentPort locationConsentPort;
    private final UserRepository userRepository;
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

    @Override
    @Transactional(readOnly = true)
    public FamilyAlertDetailResponse getAlertDetail(UUID sessionId, UUID callerId) {
        EmergencySession session = emergencySessionRepository.findById(sessionId)
                .orElseThrow(() -> new EmergencyException(HttpStatus.NOT_FOUND, "EMERG-003",
                        "Emergency session not found: " + sessionId));

        boolean isOwner = session.getUserId().equals(callerId);
        if (!isOwner && !familyMemberPort.isFamilyMember(session.getUserId(), callerId)) {
            throw new EmergencyException(HttpStatus.FORBIDDEN, "EMERG-004",
                    "You are not authorized to view this alert");
        }

        boolean hasConsent = locationConsentPort.hasLocationConsent(session.getUserId());
        String motherName = userRepository.findById(session.getUserId())
                .map(User::getName)
                .orElse("Người thân");
        FamilyAlertLog alertLog = familyAlertLogRepository.findBySessionId(sessionId).orElse(null);

        return FamilyAlertDetailResponse.builder()
                .sessionId(session.getId())
                .motherName(motherName)
                .status(session.getStatus().name())
                .triggerSource(session.getTriggerSource())
                .latitude(hasConsent ? session.getUserLatitude() : null)
                .longitude(hasConsent ? session.getUserLongitude() : null)
                .locationIncluded(hasConsent && session.getUserLatitude() != null)
                .recipientCount(alertLog != null ? alertLog.getRecipientCount() : 0)
                .createdAt(session.getCreatedAt())
                .resolvedAt(session.getResolvedAt())
                .build();
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
