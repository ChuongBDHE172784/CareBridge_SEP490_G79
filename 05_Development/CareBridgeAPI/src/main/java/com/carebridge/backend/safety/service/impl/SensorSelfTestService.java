package com.carebridge.backend.safety.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.safety.SafetyEventStatus;
import com.carebridge.backend.safety.SafetyEventType;
import com.carebridge.backend.safety.dto.request.SensorSelfTestCompletionRequest;
import com.carebridge.backend.safety.dto.request.SensorSelfTestEventRequest;
import com.carebridge.backend.safety.dto.response.SafetyEventResponse;
import com.carebridge.backend.safety.entity.ImuMonitoringSession;
import com.carebridge.backend.safety.entity.SafetyEvent;
import com.carebridge.backend.safety.entity.SafetyEventResponseRecord;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import com.carebridge.backend.safety.exception.SafetyException;
import com.carebridge.backend.safety.policy.SafetyConsentPolicy;
import com.carebridge.backend.safety.repository.IImuMonitoringSessionRepository;
import com.carebridge.backend.safety.repository.ISafetyConfigRepository;
import com.carebridge.backend.safety.repository.ISafetyEventRepository;
import com.carebridge.backend.safety.repository.SafetyEventResponseRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SensorSelfTestService {

    private static final Duration MAX_CLIENT_PAST_SKEW = Duration.ofHours(24);
    private static final Duration MAX_CLIENT_FUTURE_SKEW = Duration.ofMinutes(5);

    private final IImuMonitoringSessionRepository imuSessionRepository;
    private final ISafetyEventRepository safetyEventRepository;
    private final ISafetyConfigRepository safetyConfigRepository;
    private final SafetyEventResponseRepository responseRepository;
    private final SafetyConsentPolicy consentPolicy;
    private final AuditService auditService;

    public SafetyEventResponse create(UUID userId, SensorSelfTestEventRequest request) {
        consentPolicy.requireSensorCollection(userId);
        SafetyMonitoringConfig config = requireActiveConfig(userId);
        if (!config.isSensorPermissionGranted()) {
            throw new SafetyException(HttpStatus.FORBIDDEN, "SAFETY-009",
                    "Sensor permission has not been granted");
        }
        ImuMonitoringSession activeSession = imuSessionRepository.findActiveForUpdateByUserId(userId)
                .orElseThrow(() -> new SafetyException(HttpStatus.CONFLICT, "SAFETY-006",
                        "No active IMU monitoring session found for user"));

        Instant receivedAt = Instant.now();
        validateClientTimestamp(request.getDetectedAt(), receivedAt);
        String signalKey = "SELF_TEST:" + request.getTestId().trim();
        safetyEventRepository.acquireSignalLock(activeSession.getId() + ":" + signalKey);
        var duplicate = safetyEventRepository.findByImuSessionIdAndSignalKey(activeSession.getId(), signalKey);
        if (duplicate.isPresent()) {
            return toResponse(duplicate.get());
        }

        SafetyEvent event = SafetyEvent.builder()
                .userId(userId)
                .imuSessionId(activeSession.getId())
                .eventType(SafetyEventType.SENSOR_SELF_TEST)
                .magnitude(BigDecimal.valueOf(request.getAccelerationMagnitude()))
                .detectedAt(receivedAt)
                .clientDetectedAt(request.getDetectedAt())
                .status(SafetyEventStatus.TEST_OPEN)
                .signalKey(signalKey)
                .countdownDeadlineAt(receivedAt.plusSeconds(config.getCountdownSeconds()))
                .notes("Diễn tập cảm biến IMU; gyro="
                        + String.format(java.util.Locale.ROOT, "%.2f rad/s", request.getGyroscopeMagnitude()))
                .createdBy("SYSTEM_SELF_TEST")
                .build();
        SafetyEvent saved = safetyEventRepository.save(event);
        auditService.log(AuditAction.SAFETY_EVENT_RECORDED, userId, "SafetyEvent", saved.getId().toString(),
                Map.of("eventType", SafetyEventType.SENSOR_SELF_TEST.name(),
                        "countdownSeconds", config.getCountdownSeconds(), "selfTest", true));
        return toResponse(saved);
    }

    public SafetyEventResponse complete(UUID userId, UUID eventId, SensorSelfTestCompletionRequest request) {
        SafetyEvent event = safetyEventRepository.findLockedByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new SafetyException(HttpStatus.NOT_FOUND, "SAFETY-007",
                        "Safety event not found"));
        if (event.getEventType() != SafetyEventType.SENSOR_SELF_TEST) {
            throw new SafetyException(HttpStatus.CONFLICT, "SAFETY-013",
                    "Only sensor self-test events can use this completion endpoint");
        }

        String responseType = "NEED_HELP".equals(request.getOutcome()) ? "TEST_NEED_HELP" : "TEST_TIMEOUT";
        if (event.getResponseType() != null) {
            if (event.getResponseType().equals(responseType)) return toResponse(event);
            throw new SafetyException(HttpStatus.CONFLICT, "SAFETY-010", "Safety event already has a response");
        }
        if (event.getStatus() != SafetyEventStatus.TEST_OPEN) {
            throw new SafetyException(HttpStatus.CONFLICT, "SAFETY-010", "Sensor self-test is no longer open");
        }

        Instant now = Instant.now();
        event.setStatus(SafetyEventStatus.TIMED_OUT);
        event.setResolvedAt(now);
        event.setResponseType(responseType);
        event.setResponseReason("NEED_HELP".equals(request.getOutcome())
                ? "Người dùng thử thao tác Cần trợ giúp"
                : "Người dùng thử tình huống không phản hồi");
        event.setRespondedAt(now);
        responseRepository.insert(SafetyEventResponseRecord.builder()
                .safetyEventId(event.getId())
                .ownerUserId(userId)
                .responseType(responseType)
                .reason(event.getResponseReason())
                .respondedAt(now)
                .createdBy("TEST_TIMEOUT".equals(responseType) ? null : userId)
                .actorType("TEST_TIMEOUT".equals(responseType) ? "SYSTEM" : "OWNER")
                .build());
        SafetyEvent saved = safetyEventRepository.save(event);
        auditService.log(AuditAction.SAFETY_EVENT_RESPONDED, userId, "SafetyEvent", eventId.toString(),
                Map.of("responseType", responseType, "selfTest", true));
        return toResponse(saved);
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

    private void validateClientTimestamp(Instant clientTimestamp, Instant receivedAt) {
        if (clientTimestamp.isBefore(receivedAt.minus(MAX_CLIENT_PAST_SKEW))
                || clientTimestamp.isAfter(receivedAt.plus(MAX_CLIENT_FUTURE_SKEW))) {
            throw new SafetyException(HttpStatus.BAD_REQUEST, "SAFETY-012",
                    "Sensor timestamp is outside the accepted clock-skew window");
        }
    }

    private SafetyEventResponse toResponse(SafetyEvent event) {
        return SafetyEventResponse.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .eventType(event.getEventType().name())
                .magnitude(event.getMagnitude().doubleValue())
                .detectedAt(event.getDetectedAt())
                .clientDetectedAt(event.getClientDetectedAt())
                .status(event.getStatus().name())
                .resolvedAt(event.getResolvedAt())
                .notes(event.getNotes())
                .countdownDeadlineAt(event.getCountdownDeadlineAt())
                .responseType(event.getResponseType())
                .responseReason(event.getResponseReason())
                .respondedAt(event.getRespondedAt())
                .build();
    }
}
