package com.carebridge.backend.emergency.service.impl;

import com.carebridge.backend.emergency.EmergencyStatus;
import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import com.carebridge.backend.emergency.dto.response.FamilyAlertDetailResponse;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.entity.EmergencyNotificationOutbox;
import com.carebridge.backend.emergency.entity.FamilyAlertLog;
import com.carebridge.backend.emergency.entity.TriageEmergencyEscalation;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.repository.EmergencyNotificationOutboxRepository;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.repository.TriageEmergencyEscalationRepository;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(EmergencyService.class);

    private final IEmergencySessionRepository emergencySessionRepository;
    private final TriageEmergencyEscalationRepository triageEmergencyEscalationRepository;
    private final EmergencyNotificationOutboxRepository emergencyNotificationOutboxRepository;
    private final IIntakeSessionRepository intakeSessionRepository;
    private final IFamilyAlertLogRepository familyAlertLogRepository;
    private final FamilyMemberPort familyMemberPort;
    private final LocationConsentPort locationConsentPort;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public EmergencySessionResponse openFlow(OpenEmergencyRequest request, UUID userId) {
        emergencySessionRepository.acquireUserLock(userId);
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
                    recordNotificationOutbox(saved);
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
    public EmergencySessionResponse openOrReuseFromTriage(UUID intakeSessionId, UUID userId) {
        requireCompletedRedIntake(intakeSessionId, userId);
        emergencySessionRepository.acquireUserLock(userId);

        TriageEmergencyEscalation existingLink = triageEmergencyEscalationRepository
                .findByIntakeSessionId(intakeSessionId)
                .orElse(null);
        if (existingLink != null) {
            if (!userId.equals(existingLink.getUserId())) {
                throw new EmergencyException(HttpStatus.CONFLICT, "EMERG-005",
                        "Triage escalation ownership conflict");
            }
            EmergencySession linkedSession = emergencySessionRepository
                    .findById(existingLink.getEmergencySessionId())
                    .filter(session -> userId.equals(session.getUserId()))
                    .orElseThrow(() -> new EmergencyException(HttpStatus.CONFLICT, "EMERG-005",
                            "Triage escalation target is unavailable"));
            log.info("Triage emergency escalation outcome=REPLAYED");
            return toResponse(linkedSession);
        }

        var activeSession = emergencySessionRepository.findActiveByUserId(userId);
        boolean createdNewSession = activeSession.isEmpty();
        EmergencySession session = activeSession.orElseGet(() -> {
                    EmergencySession created = EmergencySession.builder()
                            .userId(userId)
                            .status(EmergencyStatus.ACTIVE)
                            .triggerSource("AUTO_TRIAGE")
                            .createdAt(Instant.now())
                            .createdBy(userId)
                            .build();
                    EmergencySession saved = emergencySessionRepository.save(created);
                    recordNotificationOutbox(saved);
                    eventPublisher.publishEvent(new EmergencySessionOpened(
                            UUID.randomUUID(), saved.getId(), userId,
                            "AUTO_TRIAGE", null, null, saved.getCreatedAt()));
                    return saved;
                });

        triageEmergencyEscalationRepository.save(TriageEmergencyEscalation.builder()
                .intakeSessionId(intakeSessionId)
                .emergencySessionId(session.getId())
                .userId(userId)
                .triggeredAt(Instant.now())
                .build());
        log.info("Triage emergency escalation outcome={}",
                createdNewSession ? "CREATED" : "REUSED");
        return toResponse(session);
    }

    private void recordNotificationOutbox(EmergencySession session) {
        Instant now = Instant.now();
        emergencyNotificationOutboxRepository.save(EmergencyNotificationOutbox.builder()
                .emergencySessionId(session.getId())
                .status(EmergencyNotificationOutbox.PENDING)
                .attemptCount(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .build());
    }

    private void requireCompletedRedIntake(UUID intakeSessionId, UUID userId) {
        boolean eligible = intakeSessionRepository.findByIdAndUserId(intakeSessionId, userId)
                .filter(intake -> intake.getStatus() == IntakeStatus.COMPLETED)
                .filter(intake -> intake.getRiskLevel() == RiskLevel.RED)
                .isPresent();
        if (!eligible) {
            throw new EmergencyException(HttpStatus.CONFLICT, "EMERG-006",
                    "Triage intake is not eligible for emergency escalation");
        }
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
        emergencySessionRepository.acquireUserLock(userId);
        EmergencySession session = emergencySessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new EmergencyException(HttpStatus.NOT_FOUND, "EMERG-003",
                        "Emergency session not found: " + sessionId));
        session.setStatus(EmergencyStatus.RESOLVED);
        session.setResolvedAt(Instant.now());
        emergencyNotificationOutboxRepository.findForUpdate(sessionId).ifPresent(outbox -> {
            if (EmergencyNotificationOutbox.PENDING.equals(outbox.getStatus())) {
                outbox.setStatus(EmergencyNotificationOutbox.SUPPRESSED);
                outbox.setLastErrorCode("EMERGENCY_NOT_ACTIVE");
                outbox.setClaimToken(null);
                outbox.setTerminalAt(Instant.now());
            }
        });
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
