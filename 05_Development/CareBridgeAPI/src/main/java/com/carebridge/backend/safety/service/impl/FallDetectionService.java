package com.carebridge.backend.safety.service.impl;

import com.carebridge.backend.safety.ImuSessionStatus;
import com.carebridge.backend.safety.dto.response.ImuMonitoringSessionResponse;
import com.carebridge.backend.safety.dto.response.SafetyEventResponse;
import com.carebridge.backend.safety.entity.ImuMonitoringSession;
import com.carebridge.backend.safety.entity.SafetyEvent;
import com.carebridge.backend.safety.event.FallDetectionDisabled;
import com.carebridge.backend.safety.event.FallDetectionEnabled;
import com.carebridge.backend.safety.event.SuspectedFallDetected;
import com.carebridge.backend.safety.exception.SafetyException;
import com.carebridge.backend.safety.repository.IImuMonitoringSessionRepository;
import com.carebridge.backend.safety.repository.ISafetyEventRepository;
import com.carebridge.backend.safety.service.FallAnalysisResult;
import com.carebridge.backend.safety.service.IFallDetectionAlgorithmService;
import com.carebridge.backend.safety.service.IFallDetectionService;
import com.carebridge.backend.safety.service.ImuDataPayload;
import com.carebridge.backend.safety.service.LocationConsentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FallDetectionService implements IFallDetectionService {

    private final IImuMonitoringSessionRepository imuSessionRepository;
    private final ISafetyEventRepository safetyEventRepository;
    private final IFallDetectionAlgorithmService algorithmService;
    private final LocationConsentPort locationConsentPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ImuMonitoringSessionResponse enable(UUID userId, String sensitivityLevel) {
        return imuSessionRepository.findActiveByUserId(userId)
                .map(this::toSessionResponse)
                .orElseGet(() -> {
                    ImuMonitoringSession session = ImuMonitoringSession.builder()
                            .userId(userId)
                            .status(ImuSessionStatus.ACTIVE)
                            .sensitivityLevel(sensitivityLevel)
                            .startedAt(Instant.now())
                            .createdBy(userId)
                            .build();
                    ImuMonitoringSession saved = imuSessionRepository.save(session);
                    eventPublisher.publishEvent(new FallDetectionEnabled(
                            UUID.randomUUID(), userId, saved.getId(), sensitivityLevel, Instant.now()));
                    return toSessionResponse(saved);
                });
    }

    @Override
    public void disable(UUID userId) {
        imuSessionRepository.findActiveByUserId(userId).ifPresent(session -> {
            session.setStatus(ImuSessionStatus.STOPPED);
            session.setEndedAt(Instant.now());
            imuSessionRepository.save(session);
            eventPublisher.publishEvent(new FallDetectionDisabled(
                    UUID.randomUUID(), userId, session.getId(), Instant.now()));
        });
    }

    @Override
    public SafetyEventResponse processImuData(UUID userId, ImuDataPayload payload) {
        ImuMonitoringSession activeSession = imuSessionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new SafetyException(HttpStatus.CONFLICT, "SAFETY-006",
                        "No active IMU monitoring session found for user"));

        FallAnalysisResult analysis = algorithmService.analyze(payload, activeSession.getSensitivityLevel());

        if (!analysis.suspected()) {
            return null;
        }

        boolean hasConsent = locationConsentPort.hasLocationConsent(userId);
        BigDecimal latitude = hasConsent ? payload.latitude() : null;
        BigDecimal longitude = hasConsent ? payload.longitude() : null;

        SafetyEvent event = SafetyEvent.builder()
                .userId(userId)
                .imuSessionId(activeSession.getId())
                .eventType(analysis.eventType())
                .magnitude(BigDecimal.valueOf(analysis.magnitude()))
                .userLatitude(latitude)
                .userLongitude(longitude)
                .detectedAt(Instant.now())
                .createdBy("SYSTEM")
                .build();

        SafetyEvent saved = safetyEventRepository.save(event);

        eventPublisher.publishEvent(new SuspectedFallDetected(
                UUID.randomUUID(),
                userId,
                saved.getId(),
                analysis.eventType().name(),
                analysis.magnitude(),
                latitude,
                longitude,
                saved.getDetectedAt()));

        return toEventResponse(saved);
    }

    private ImuMonitoringSessionResponse toSessionResponse(ImuMonitoringSession session) {
        return ImuMonitoringSessionResponse.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .status(session.getStatus().name())
                .sensitivityLevel(session.getSensitivityLevel())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();
    }

    private SafetyEventResponse toEventResponse(SafetyEvent event) {
        return SafetyEventResponse.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .eventType(event.getEventType().name())
                .magnitude(event.getMagnitude().doubleValue())
                .userLatitude(event.getUserLatitude())
                .userLongitude(event.getUserLongitude())
                .detectedAt(event.getDetectedAt())
                .build();
    }
}
