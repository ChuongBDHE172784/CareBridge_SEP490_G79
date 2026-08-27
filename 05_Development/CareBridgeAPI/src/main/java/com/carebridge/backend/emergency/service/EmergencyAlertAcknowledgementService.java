package com.carebridge.backend.emergency.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.repository.EmergencyAlertAcknowledgementRepository;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmergencyAlertAcknowledgementService {

    private final IEmergencySessionRepository emergencySessionRepository;
    private final FamilyMemberPort familyMemberPort;
    private final EmergencyAlertAcknowledgementRepository acknowledgementRepository;
    private final AuditService auditService;

    @Transactional
    public void acknowledge(UUID sessionId, UUID callerId) {
        EmergencySession session = emergencySessionRepository.findById(sessionId)
                .orElseThrow(() -> new EmergencyException(HttpStatus.NOT_FOUND, "EMERG-003",
                        "Emergency session not found: " + sessionId));
        boolean isOwner = session.getUserId().equals(callerId);
        if (!isOwner && !familyMemberPort.isFamilyMember(session.getUserId(), callerId)) {
            throw new EmergencyException(HttpStatus.FORBIDDEN, "EMERG-004",
                    "You are not authorized to acknowledge this alert");
        }

        var state = acknowledgementRepository.find(sessionId, callerId);
        if (!state.notificationExists()) {
            throw new EmergencyException(HttpStatus.NOT_FOUND, "EMERG-007",
                    "No emergency notification is available for this account");
        }
        int affected = acknowledgementRepository.acknowledge(sessionId, callerId, Instant.now());
        if (affected > 0) {
            auditService.log(AuditAction.FAMILY_ALERT_VIEWED, callerId,
                    "EmergencySession", sessionId.toString(),
                    Map.of("action", "ACKNOWLEDGED", "notificationsUpdated", affected));
        }
    }
}
