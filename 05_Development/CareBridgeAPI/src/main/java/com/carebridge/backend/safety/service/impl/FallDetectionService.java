package com.carebridge.backend.safety.service.impl;

import com.carebridge.backend.safety.ImuSessionStatus;
import com.carebridge.backend.safety.SafetyEventStatus;
import com.carebridge.backend.safety.SafetyEventType;
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
import com.carebridge.backend.safety.repository.SafetyConfigStore;
import com.carebridge.backend.safety.repository.SafetyEventResponseRepository;
import com.carebridge.backend.safety.entity.SafetyEventResponseRecord;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import com.carebridge.backend.safety.policy.SafetyConsentPolicy;
import com.carebridge.backend.safety.service.FallAnalysisResult;
import com.carebridge.backend.safety.service.IFallDetectionAlgorithmService;
import com.carebridge.backend.safety.service.IFallDetectionService;
import com.carebridge.backend.safety.service.ImuDataPayload;
import com.carebridge.backend.safety.service.SafetyCountdownTransactionRunner;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.service.IEmergencyService;

/**
 * Service triển khai nghiệp vụ giám sát an toàn và phát hiện té ngã trên Backend (UC-134 đến UC-137).
 *
 * **Trách nhiệm chính:**
 * 1. Quản lý vòng đời phiên giám sát IMU ([ImuMonitoringSession]).
 * 2. Tiếp nhận, lọc trùng lặp và phân tích dữ liệu cảm biến IMU & vị trí GPS gửi từ Client.
 * 3. Tạo bản ghi sự kiện té ngã ([SafetyEvent]) ở trạng thái OPEN kèm thời hạn đếm ngược (30s).
 * 4. Xử lý phản hồi của người dùng: "Tôi vẫn ổn" (CONFIRMED_SAFE) hoặc "Báo nhầm" (FALSE_POSITIVE).
 * 5. Tự động chuyển cấp sang phiên khẩn cấp ([EmergencySession]) và gửi thông báo cảnh báo tới người thân trong gia đình (Family Alert) khi hết thời gian 30s hoặc người dùng yêu cầu trợ giúp.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FallDetectionService implements IFallDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FallDetectionService.class);

    /** Giới hạn độ lệch thời gian cho phép giữa Client và Server (Quá khứ: 24 giờ). */
    private static final Duration MAX_CLIENT_PAST_SKEW = Duration.ofHours(24);

    /** Giới hạn độ lệch thời gian cho phép giữa Client và Server (Tương lai: 5 phút). */
    private static final Duration MAX_CLIENT_FUTURE_SKEW = Duration.ofMinutes(5);

    /** Cửa sổ lọc trùng lặp cú ngã (10 giây): Nhiều mẫu va đập trong vòng 10s được coi là cùng 1 sự cố ngã. */
    private static final Duration DUPLICATE_FALL_WINDOW = Duration.ofSeconds(10);

    private final IImuMonitoringSessionRepository imuSessionRepository;
    private final ISafetyEventRepository safetyEventRepository;
    private final IFallDetectionAlgorithmService algorithmService;
    private final ApplicationEventPublisher eventPublisher;
    private final SafetyConfigStore safetyConfigStore;
    private final SafetyEventResponseRepository responseRepository;
    private final SafetyConsentPolicy consentPolicy;
    private final IEmergencyService emergencyService;
    private final AuditService auditService;
    private final SafetyCountdownTransactionRunner countdownTransactionRunner;

    /**
     * UC-134: Kích hoạt phiên giám sát an toàn IMU.
     * Kiểm tra quyền thu thập cảm biến (PDPA Consent) và quyền phần cứng trước khi tạo session.
     */
    @Override
    public ImuMonitoringSessionResponse enable(UUID userId, String sensitivityLevel) {
        SafetyMonitoringConfig config = requireActiveConfig(userId);
        consentPolicy.requireSensorCollection(userId);
        if (!config.isSensorPermissionGranted()) {
            throw new SafetyException(HttpStatus.FORBIDDEN, "SAFETY-009",
                    "Sensor permission has not been granted");
        }
        imuSessionRepository.acquireUserLock(userId);
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

    /**
     * UC-135: Hủy / dừng phiên giám sát an toàn IMU.
     */
    @Override
    public void disable(UUID userId) {
        imuSessionRepository.acquireUserLock(userId);
        imuSessionRepository.findActiveForUpdateByUserId(userId).ifPresent(session -> {
            session.setStatus(ImuSessionStatus.STOPPED);
            session.setEndedAt(Instant.now());
            imuSessionRepository.save(session);
            eventPublisher.publishEvent(new FallDetectionDisabled(
                    UUID.randomUUID(), userId, session.getId(), Instant.now()));
            auditService.log(AuditAction.SAFETY_MONITORING_DISABLED, userId,
                    "ImuMonitoringSession", session.getId().toString(), Map.of("status", "STOPPED"));
        });
    }

    /**
     * UC-136/UC-137: Tiếp nhận dữ liệu IMU từ điện thoại, phân tích vật lý và tạo sự kiện ngã nếu xác nhận.
     */
    @Override
    public SafetyEventResponse processImuData(UUID userId, ImuDataPayload payload) {
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
        validateClientTimestamp(payload.timestamp(), receivedAt);
        String signalKey = payload.signalId() == null || payload.signalId().isBlank()
                ? payload.timestamp().toString()
                : payload.signalId().trim();
        safetyEventRepository.acquireSignalLock(activeSession.getId() + ":" + signalKey);
        var duplicate = safetyEventRepository.findByImuSessionIdAndSignalKey(activeSession.getId(), signalKey);
        if (duplicate.isPresent()) {
            return toEventResponse(duplicate.get());
        }

        // Nếu thiết bị di động đã xác thực ngã 3 pha (rơi tự do -> va đập -> nằm yên)
        // thì tin cậy cờ onDeviceFallConfirmed=true để không bị loại bỏ nhầm trường hợp rơi lên bề mặt mềm.
        FallAnalysisResult analysis = payload.onDeviceFallConfirmed()
                ? onDeviceConfirmedAnalysis(payload)
                : algorithmService.analyze(payload, activeSession.getSensitivityLevel());

        if (!analysis.suspected()) {
            return null;
        }

        // Chống nhân đôi sự kiện ngã trong cửa sổ 10 giây (DUPLICATE_FALL_WINDOW)
        Optional<SafetyEvent> recentEvent = safetyEventRepository
                .findFirstByImuSessionIdAndResponseTypeIsNullAndDetectedAtAfterOrderByDetectedAtDesc(
                        activeSession.getId(), receivedAt.minus(DUPLICATE_FALL_WINDOW));
        if (recentEvent.isPresent()) {
            return toEventResponse(recentEvent.get());
        }

        // Kiểm tra quyền chia sẻ vị trí (PDPA Consent LOCATION:SHARE) trước khi lưu tọa độ GPS
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
        eventPublisher.publishEvent(new SuspectedFallDetected(
                UUID.randomUUID(),
                saved.getUserId(),
                saved.getId(),
                saved.getEventType().name(),
                saved.getMagnitude().doubleValue(),
                saved.getUserLatitude(),
                saved.getUserLongitude(),
                saved.getDetectedAt()));

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

    /**
     * Người dùng nhấn "Tôi vẫn ổn" -> Cập nhật trạng thái CONFIRMED_SAFE.
     */
    @Override
    public SafetyEventResponse confirmSafetyCheck(UUID userId, UUID eventId, String note) {
        return respond(userId, eventId, "I_AM_OK", SafetyEventStatus.CONFIRMED_SAFE, note, false);
    }

    /**
     * Người dùng báo phát hiện nhầm -> Cập nhật trạng thái FALSE_POSITIVE.
     */
    @Override
    public SafetyEventResponse reportFalsePositive(UUID userId, UUID eventId, String note) {
        return respond(userId, eventId, "FALSE_POSITIVE", SafetyEventStatus.FALSE_POSITIVE, note, false);
    }

    /**
     * Kích hoạt phiên cấp cứu khẩn cấp (EmergencySession) và gửi cảnh báo tới gia đình khi hết thời gian đếm ngược hoặc nhấn nút cứu hộ.
     */
    @Override
    public void sendEmergencyAlert(UUID userId, UUID eventId) {
        SafetyEvent requestedEvent = findOwnedEvent(userId, eventId);
        if (requestedEvent.getEventType() == SafetyEventType.SENSOR_SELF_TEST) {
            throw new SafetyException(HttpStatus.CONFLICT, "SAFETY-013",
                    "Sensor self-test events cannot trigger emergency alerts");
        }
        // Nếu người dùng đã kịp nhấn "Tôi vẫn ổn" trước đó thì bỏ qua yêu cầu khẩn cấp đến muộn
        if ("I_AM_OK".equals(requestedEvent.getResponseType())
                || "FALSE_POSITIVE".equals(requestedEvent.getResponseType())) {
            return;
        }
        // Xử lý khi quá thời gian đếm ngược (TIMEOUT)
        if ("TIMEOUT".equals(requestedEvent.getResponseType())) {
            requestedEvent.setStatus(SafetyEventStatus.ESCALATION_REQUESTED);
            requestedEvent.setResolvedAt(null);
            SafetyEvent saved = safetyEventRepository.save(requestedEvent);
            openEmergency(saved);
            return;
        }
        SafetyEvent saved = respondEntity(userId, eventId, "NEED_HELP", SafetyEventStatus.ESCALATION_REQUESTED, null, true);
        openEmergency(saved);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processExpiredCountdowns() {
        for (SafetyEvent event : safetyEventRepository
                .findTop100ByStatusAndResponseTypeIsNullAndCountdownDeadlineAtLessThanEqualOrderByCountdownDeadlineAtAsc(
                        SafetyEventStatus.OPEN, Instant.now())) {
            try {
                countdownTransactionRunner.run(() -> processExpiredCountdown(event.getUserId(), event.getId()));
            } catch (RuntimeException exception) {
                log.error("Safety countdown event failed eventId={} reason={}",
                        event.getId(), exception.getClass().getSimpleName());
            }
        }
    }

    private void processExpiredCountdown(UUID userId, UUID eventId) {
        SafetyEvent event = findOwnedEvent(userId, eventId);
        if (event.getStatus() != SafetyEventStatus.OPEN
                || event.getResponseType() != null
                || event.getCountdownDeadlineAt() == null
                || event.getCountdownDeadlineAt().isAfter(Instant.now())) {
            return;
        }
        boolean shouldEscalate = safetyConfigStore.findByUserId(userId)
                .map(SafetyMonitoringConfig::isEmergencyAutoAlert).orElse(false);
        SafetyEvent saved = respondEntity(userId, eventId, "TIMEOUT",
                shouldEscalate ? SafetyEventStatus.ESCALATION_REQUESTED : SafetyEventStatus.TIMED_OUT,
                null, shouldEscalate);
        if (shouldEscalate) {
            openEmergency(saved);
        }
    }

    private void openEmergency(SafetyEvent saved) {
        if (saved.getEmergencySessionId() != null) {
            synchronizeLinkedSentSession(saved);
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
        synchronizeLinkedSentSession(saved);
        auditService.log(AuditAction.SAFETY_EVENT_ESCALATED, saved.getUserId(), "SafetyEvent",
                saved.getId().toString(), Map.of("emergencySessionId", emergency.getSessionId().toString()));
    }

    private FallAnalysisResult onDeviceConfirmedAnalysis(ImuDataPayload payload) {
        double accelerationMagnitude = Math.sqrt(
                payload.accelerometerX() * payload.accelerometerX()
                        + payload.accelerometerY() * payload.accelerometerY()
                        + payload.accelerometerZ() * payload.accelerometerZ());
        return new FallAnalysisResult(
                true,
                SafetyEventType.SUSPECTED_FALL,
                Math.abs(accelerationMagnitude - 9.81));
    }

    private void synchronizeLinkedSentSession(SafetyEvent saved) {
        int synchronizedEvents = safetyEventRepository.transitionLinkedEventForSentEmergencySession(
                saved.getId(),
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT);
        if (synchronizedEvents > 0) {
            saved.setStatus(SafetyEventStatus.EMERGENCY_ALERT_SENT);
        }
    }

    private SafetyEventResponse respond(UUID userId, UUID eventId, String responseType,
                                        SafetyEventStatus status, String reason, boolean escalation) {
        return toEventResponse(respondEntity(userId, eventId, responseType, status, reason, escalation));
    }

    private SafetyEvent respondEntity(UUID userId, UUID eventId, String responseType,
                                      SafetyEventStatus status, String reason, boolean escalation) {
        SafetyEvent event = findOwnedEvent(userId, eventId);
        if (event.getEventType() == SafetyEventType.SENSOR_SELF_TEST) {
            if (event.getResponseType() != null) {
                if (event.getResponseType().equals(responseType)) {
                    return event;
                }
                throw new SafetyException(HttpStatus.CONFLICT, "SAFETY-010", "Safety event already has a response");
            }
            if (event.getStatus() != SafetyEventStatus.TEST_OPEN) {
                throw new SafetyException(HttpStatus.CONFLICT, "SAFETY-010", "Sensor self-test is no longer open");
            }
        } else {
            if (event.getResponseType() != null) {
                if (event.getResponseType().equals(responseType)) {
                    return event;
                }
                boolean lateOwnerSafetyResponse =
                        ("TIMEOUT".equals(event.getResponseType()) || "NEED_HELP".equals(event.getResponseType()))
                                && ("I_AM_OK".equals(responseType) || "FALSE_POSITIVE".equals(responseType));
                if (!lateOwnerSafetyResponse) {
                    throw new SafetyException(HttpStatus.CONFLICT, "SAFETY-010", "Safety event already has a response");
                }
            }
        }
        Instant now = Instant.now();
        event.setStatus(status);
        event.setResolvedAt(escalation ? null : now);
        event.setNotes(canonicalNote(reason));
        event.setResponseType(responseType);
        event.setResponseReason(reason);
        event.setRespondedAt(now);
        responseRepository.insert(SafetyEventResponseRecord.builder()
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

    private String canonicalNote(String reason) {
        if (reason == null || reason.length() <= 255) {
            return reason;
        }
        return reason.substring(0, 255);
    }

    private SafetyMonitoringConfig requireActiveConfig(UUID userId) {
        SafetyMonitoringConfig config = safetyConfigStore.findByUserId(userId)
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
