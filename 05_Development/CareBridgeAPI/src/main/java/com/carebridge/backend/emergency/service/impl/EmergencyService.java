package com.carebridge.backend.emergency.service.impl;

import com.carebridge.backend.emergency.EmergencyStatus;
import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import com.carebridge.backend.emergency.dto.response.FamilyAlertDetailResponse;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.entity.FamilyAlertLog;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.event.EmergencySessionRealertRequested;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.repository.EmergencyAlertAcknowledgementRepository;
import com.carebridge.backend.emergency.repository.TriageEmergencyEscalationLinkRepository;
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
    private final IIntakeSessionRepository intakeSessionRepository;
    private final TriageEmergencyEscalationLinkRepository triageEscalationLinkRepository;
    private final IFamilyAlertLogRepository familyAlertLogRepository;
    private final FamilyMemberPort familyMemberPort;
    private final LocationConsentPort locationConsentPort;
    private final UserRepository userRepository;
    private final EmergencyAlertAcknowledgementRepository acknowledgementRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public EmergencySessionResponse openFlow(OpenEmergencyRequest request, UUID userId) {
        emergencySessionRepository.acquireUserLock(userId);

        // UC62 C3: idempotent — return existing ACTIVE session if one exists
        return emergencySessionRepository.findActiveByUserId(userId)
                .map(session -> {
                    eventPublisher.publishEvent(new EmergencySessionRealertRequested(
                            UUID.randomUUID(), session.getId(), userId, request.getTriggerSource(),
                            request.getUserLatitude(), request.getUserLongitude(), Instant.now()));
                    return toResponse(session);
                })
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
    public EmergencySessionResponse openOrReuseFromTriage(UUID intakeSessionId, UUID userId) {
        requireCompletedRedIntake(intakeSessionId, userId);
        emergencySessionRepository.acquireUserLock(userId);

        EmergencySession existingLink = triageEscalationLinkRepository
                .findEmergencySessionId(intakeSessionId, userId)
                .flatMap(emergencySessionRepository::findById)
                .filter(session -> session.getUserId().equals(userId))
                .orElse(null);
        if (existingLink != null) {
            log.info("Triage emergency escalation outcome=REPLAYED");
            return toResponse(existingLink);
        }

        var activeSession = emergencySessionRepository.findActiveByUserId(userId);
        boolean createdNewSession = activeSession.isEmpty();
        EmergencySession session = activeSession.orElseGet(() -> {
                    EmergencySession created = EmergencySession.builder()
                            .userId(userId)
                            .sourceEventId(intakeSessionId)
                            .status(EmergencyStatus.ACTIVE)
                            .triggerSource("AUTO_TRIAGE")
                            .createdAt(Instant.now())
                            .createdBy(userId)
                            .build();
                    EmergencySession saved = emergencySessionRepository.saveAndFlush(created);
                    eventPublisher.publishEvent(new EmergencySessionOpened(
                            UUID.randomUUID(), saved.getId(), userId,
                            "AUTO_TRIAGE", null, null, saved.getCreatedAt()));
                    return saved;
                });

        if (session.getSourceEventId() == null) {
            session.setSourceEventId(intakeSessionId);
            session = emergencySessionRepository.save(session);
        }

        UUID canonicalSessionId = triageEscalationLinkRepository.linkIfAbsent(
                intakeSessionId, session.getId(), userId, Instant.now());
        if (!canonicalSessionId.equals(session.getId())) {
            session = emergencySessionRepository.findById(canonicalSessionId)
                    .filter(candidate -> candidate.getUserId().equals(userId))
                    .orElseThrow(() -> new IllegalStateException(
                            "Canonical triage escalation references an unavailable emergency session"));
        }
        log.info("Triage emergency escalation outcome={}",
                createdNewSession ? "CREATED" : "REUSED");
        return toResponse(session);
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
        EmergencySession session = emergencySessionRepository.findByIdForUpdate(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new EmergencyException(HttpStatus.NOT_FOUND, "EMERG-003",
                        "Emergency session not found: " + sessionId));
        session.setStatus(EmergencyStatus.RESOLVED);
        Instant resolvedAt = Instant.now();
        session.setResolvedAt(resolvedAt);
        if (!"SENT".equals(session.getAlertStatus())) {
            session.setAlertStatus("SUPPRESSED");
            session.setAlertLeaseExpiresAt(null);
            session.setAlertUpdatedAt(resolvedAt);
        }
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
        User mother = userRepository.findById(session.getUserId()).orElse(null);
        String motherName = mother == null || mother.getName() == null || mother.getName().isBlank()
                ? "Người thân"
                : mother.getName();
        FamilyAlertLog alertLog = familyAlertLogRepository.findBySessionId(sessionId).orElse(null);
        var acknowledgement = acknowledgementRepository.find(sessionId, callerId);

        return FamilyAlertDetailResponse.builder()
                .sessionId(session.getId())
                .motherName(motherName)
                .motherPhone(mother == null ? null : mother.getPhone())
                .status(session.getStatus().name())
                .triggerSource(session.getTriggerSource())
                .latitude(hasConsent ? session.getUserLatitude() : null)
                .longitude(hasConsent ? session.getUserLongitude() : null)
                .locationIncluded(hasConsent && session.getUserLatitude() != null)
                .recipientCount(alertLog != null ? alertLog.getRecipientCount() : 0)
                .acknowledged(acknowledgement.acknowledged())
                .acknowledgedAt(acknowledgement.acknowledgedAt())
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
