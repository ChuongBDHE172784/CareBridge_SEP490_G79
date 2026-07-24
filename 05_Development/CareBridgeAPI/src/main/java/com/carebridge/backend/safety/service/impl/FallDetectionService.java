package com.carebridge.backend.safety.service.impl;

import com.carebridge.backend.safety.ImuSessionStatus;
import com.carebridge.backend.safety.SafetyEventStatus;
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
import com.carebridge.backend.safety.repository.ISafetyConfigRepository;
import com.carebridge.backend.safety.repository.SafetyEventResponseRepository;
import com.carebridge.backend.safety.entity.SafetyEventResponseRecord;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import com.carebridge.backend.safety.policy.SafetyConsentPolicy;
import com.carebridge.backend.safety.service.FallAnalysisResult;
import com.carebridge.backend.safety.service.IFallDetectionAlgorithmService;
import com.carebridge.backend.safety.service.IFallDetectionService;
import com.carebridge.backend.safety.service.ImuDataPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.service.IEmergencyService;

@Service
@RequiredArgsConstructor
@Transactional
public class FallDetectionService implements IFallDetectionService {

    private static final Duration MAX_CLIENT_PAST_SKEW = Duration.ofHours(24);
    private static final Duration MAX_CLIENT_FUTURE_SKEW = Duration.ofMinutes(5);

    private final IImuMonitoringSessionRepository imuSessionRepository;
    private final ISafetyEventRepository safetyEventRepository;
    private final IFallDetectionAlgorithmService algorithmService;
    private final ApplicationEventPublisher eventPublisher;
    private final ISafetyConfigRepository safetyConfigRepository;
    private final SafetyEventResponseRepository responseRepository;
    private final SafetyConsentPolicy consentPolicy;
    private final IEmergencyService emergencyService;
    private final AuditService auditService;

    @Override
    public ImuMonitoringSessionResponse enable(UUID userId, String sensitivityLevel) {
        SafetyMonitoringConfig config = requireActiveConfig(userId);
        consentPolicy.requireSensorCollection(userId);
        if (!config.isSensorPermissionGranted()) {
            throw new SafetyException(HttpStatus.FORBIDDEN, "SAFETY-009",
                    "Sensor permission has not been granted");
        }
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
                    auditService.log(AuditAction.SAFETY_MONITORING_ENABLED, userId,
                            "ImuMonitoringSession", saved.getId().toString(), Map.of("sensitivity", sensitivityLevel));
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
            auditService.log(AuditAction.SAFETY_MONITORING_DISABLED, userId,
                    "ImuMonitoringSession", session.getId().toString(), Map.of("status", "STOPPED"));
        });
    }

    @Override
    public SafetyEventResponse processImuData(UUID userId, ImuDataPayload payload) {
        consentPolicy.requireSensorCollection(userId);
        SafetyMonitoringConfig config = requireActiveConfig(userId);
        if (!config.isSensorPermissionGranted()) {
            throw new SafetyException(HttpStatus.FORBIDDEN, "SAFETY-009",
                    "Sensor permission has not been granted");
        }
        ImuMonitoringSession activeSession = imuSessionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new SafetyException(HttpStatus.CONFLICT, "SAFETY-006",
                        "No active IMU monitoring session found for user"));

        Instant receivedAt = Instant.now();
        validateClientTimestamp(payload.timestamp(), receivedAt);
        String signalKey = payload.signalId() == null || payload.signalId().isBlank()
                ? payload.timestamp().toString()
                : payload.signalId().trim();
        safetyEventRepository.acquireSignalLock(activeSession.getId() + ":" + signalKey);
        var duplicate = safetyEventRepository.findByImuSessionIdAndSignalKey(activeSession.getId(), signalKey);
        if (duplicate.isPresent()) {
            return toEventResponse(duplicate.get());
        }

        FallAnalysisResult analysis = algorithmService.analyze(payload, activeSession.getSensitivityLevel());

        if (!analysis.suspected()) {
            return null;
        }

        boolean includeLocation = payload.latitude() != null && payload.longitude() != null
                && consentPolicy.mayPersistLocation(userId);
        SafetyEvent event = SafetyEvent.builder()
                .userId(userId)
                .imuSessionId(activeSession.getId())
                .eventType(analysis.eventType())
                .magnitude(BigDecimal.valueOf(analysis.magnitude()))
                .userLatitude(includeLocation ? payload.latitude() : null)
                .userLongitude(includeLocation ? payload.longitude() : null)
                .detectedAt(receivedAt)
                .clientDetectedAt(payload.timestamp())
                .signalKey(signalKey)
                .countdownDeadlineAt(Instant.now().plusSeconds(config.getCountdownSeconds()))
                .createdBy("SYSTEM")
                .build();

        SafetyEvent saved = safetyEventRepository.save(event);

        auditService.log(AuditAction.SAFETY_EVENT_RECORDED, userId, "SafetyEvent", saved.getId().toString(),
                Map.of("eventType", saved.getEventType().name(), "countdownSeconds", config.getCountdownSeconds()));

        return toEventResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SafetyEventResponse> listSafetyEvents(UUID userId, org.springframework.data.domain.Pageable pageable) {
        return safetyEventRepository.findByUserIdOrderByDetectedAtDesc(userId, pageable)
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Override
    public SafetyEventResponse confirmSafetyCheck(UUID userId, UUID eventId, String note) {
        return respond(userId, eventId, "I_AM_OK", SafetyEventStatus.CONFIRMED_SAFE, note, false);
    }

    @Override
    public SafetyEventResponse reportFalsePositive(UUID userId, UUID eventId, String note) {
        return respond(userId, eventId, "FALSE_POSITIVE", SafetyEventStatus.FALSE_POSITIVE, note, false);
    }

    @Override
    public void sendEmergencyAlert(UUID userId, UUID eventId) {
        SafetyEvent saved = respondEntity(userId, eventId, "NEED_HELP", SafetyEventStatus.ESCALATION_REQUESTED, null, true);
        openEmergency(saved);
    }

    @Override
    public void processExpiredCountdowns() {
        for (SafetyEvent event : safetyEventRepository
                .findTop100ByStatusAndResponseTypeIsNullAndCountdownDeadlineAtLessThanEqualOrderByCountdownDeadlineAtAsc(
                        SafetyEventStatus.OPEN, Instant.now())) {
            boolean shouldEscalate = safetyConfigRepository.findByUserId(event.getUserId())
                    .map(SafetyMonitoringConfig::isEmergencyAutoAlert).orElse(false);
            SafetyEvent saved = respondEntity(event.getUserId(), event.getId(), "TIMEOUT",
                    shouldEscalate ? SafetyEventStatus.ESCALATION_REQUESTED : SafetyEventStatus.TIMED_OUT,
                    null, shouldEscalate);
            if (shouldEscalate) openEmergency(saved);
        }
    }

    private void openEmergency(SafetyEvent saved) {
        if (saved.getEmergencySessionId() != null) {
            return;
        }
        var emergency = emergencyService.openFlow(OpenEmergencyRequest.builder()
                .triggerSource("FALL_DETECTION")
                .userLatitude(saved.getUserLatitude())
                .userLongitude(saved.getUserLongitude())
                .build(), saved.getUserId());
        saved.setEmergencySessionId(emergency.getSessionId());
        saved.setEscalationStartedAt(Instant.now());
        safetyEventRepository.save(saved);
        eventPublisher.publishEvent(new SuspectedFallDetected(
                UUID.randomUUID(),
                saved.getUserId(),
                saved.getId(),
                saved.getEventType().name(),
                saved.getMagnitude().doubleValue(),
                saved.getUserLatitude(),
                saved.getUserLongitude(),
                saved.getDetectedAt()));
        auditService.log(AuditAction.SAFETY_EVENT_ESCALATED, saved.getUserId(), "SafetyEvent",
                saved.getId().toString(), Map.of("emergencySessionId", emergency.getSessionId().toString()));
    }

    private SafetyEventResponse respond(UUID userId, UUID eventId, String responseType,
                                        SafetyEventStatus status, String reason, boolean escalation) {
        return toEventResponse(respondEntity(userId, eventId, responseType, status, reason, escalation));
    }

    private SafetyEvent respondEntity(UUID userId, UUID eventId, String responseType,
                                      SafetyEventStatus status, String reason, boolean escalation) {
        SafetyEvent event = findOwnedEvent(userId, eventId);
        if (event.getResponseType() != null) {
            if (event.getResponseType().equals(responseType)) {
                return event;
            }
            throw new SafetyException(HttpStatus.CONFLICT, "SAFETY-010", "Safety event already has a response");
        }
        Instant now = Instant.now();
        event.setStatus(status);
        event.setResolvedAt(escalation ? null : now);
        event.setNotes(reason);
        event.setResponseType(responseType);
        event.setResponseReason(reason);
        event.setRespondedAt(now);
        responseRepository.save(SafetyEventResponseRecord.builder()
                .safetyEventId(event.getId())
                .ownerUserId(userId)
                .responseType(responseType)
                .reason(reason)
                .respondedAt(now)
                .createdBy("TIMEOUT".equals(responseType) ? null : userId)
                .actorType("TIMEOUT".equals(responseType) ? "SYSTEM" : "OWNER")
                .build());
        SafetyEvent saved = safetyEventRepository.save(event);
        auditService.log(AuditAction.SAFETY_EVENT_RESPONDED, userId, "SafetyEvent", eventId.toString(),
                Map.of("responseType", responseType));
        return saved;
    }

    private SafetyMonitoringConfig requireActiveConfig(UUID userId) {
        SafetyMonitoringConfig config = safetyConfigRepository.findByUserId(userId)
                .orElseThrow(() -> new SafetyException(HttpStatus.CONFLICT, "SAFETY-011",
                        "Safety monitoring is not configured"));
        if (!config.isFallDetectionEnabled()) {
            throw new SafetyException(HttpStatus.CONFLICT, "SAFETY-011", "Fall detection is disabled");
        }
        return config;
    }

    private SafetyEvent findOwnedEvent(UUID userId, UUID eventId) {
        return safetyEventRepository.findLockedByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new SafetyException(HttpStatus.NOT_FOUND, "SAFETY-007",
                        "Safety event not found"));
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
                .clientDetectedAt(event.getClientDetectedAt())
                .status(event.getStatus().name())
                .resolvedAt(event.getResolvedAt())
                .notes(event.getNotes())
                .countdownDeadlineAt(event.getCountdownDeadlineAt())
                .responseType(event.getResponseType())
                .responseReason(event.getResponseReason())
                .respondedAt(event.getRespondedAt())
                .emergencySessionId(event.getEmergencySessionId())
                .build();
    }

    private void validateClientTimestamp(Instant clientTimestamp, Instant receivedAt) {
        if (clientTimestamp.isBefore(receivedAt.minus(MAX_CLIENT_PAST_SKEW))
                || clientTimestamp.isAfter(receivedAt.plus(MAX_CLIENT_FUTURE_SKEW))) {
            throw new SafetyException(HttpStatus.BAD_REQUEST, "SAFETY-012",
                    "Sensor timestamp is outside the accepted clock-skew window");
        }
    }
}
