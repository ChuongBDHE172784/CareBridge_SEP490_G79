package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.EmergencyStatus;
import com.carebridge.backend.emergency.entity.EmergencyNotificationOutbox;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.repository.EmergencyNotificationOutboxRepository;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyNotificationOutboxWriter {

    public record ClaimedNotification(EmergencySessionOpened event, UUID claimToken) {}

    private static final Duration CLAIM_LEASE = Duration.ofSeconds(30);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);

    private final EmergencyNotificationOutboxRepository outboxRepository;
    private final IEmergencySessionRepository emergencySessionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimedNotification claim(UUID emergencySessionId) {
        Instant now = Instant.now();
        EmergencyNotificationOutbox outbox = outboxRepository.findForUpdate(emergencySessionId)
                .orElse(null);
        if (outbox == null
                || !EmergencyNotificationOutbox.PENDING.equals(outbox.getStatus())
                || outbox.getNextAttemptAt().isAfter(now)) {
            return null;
        }

        EmergencySession session = emergencySessionRepository.findById(emergencySessionId)
                .orElse(null);
        if (session == null) {
            suppress(outbox, "MISSING_EMERGENCY_SESSION", now);
            return null;
        }
        if (session.getStatus() != EmergencyStatus.ACTIVE) {
            suppress(outbox, "EMERGENCY_NOT_ACTIVE", now);
            return null;
        }

        UUID claimToken = UUID.randomUUID();
        outbox.setAttemptCount(outbox.getAttemptCount() + 1);
        outbox.setClaimToken(claimToken);
        outbox.setNextAttemptAt(now.plus(CLAIM_LEASE));
        return new ClaimedNotification(
                new EmergencySessionOpened(
                        UUID.randomUUID(), session.getId(), session.getUserId(),
                        session.getTriggerSource(), session.getUserLatitude(), session.getUserLongitude(),
                        session.getCreatedAt()),
                claimToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markDelivered(UUID emergencySessionId, UUID claimToken) {
        EmergencyNotificationOutbox outbox = outboxRepository.findForUpdate(emergencySessionId).orElse(null);
        if (!ownsPendingClaim(outbox, claimToken)) {
            return false;
        }
        Instant now = Instant.now();
        outbox.setStatus(EmergencyNotificationOutbox.DELIVERED);
        outbox.setDeliveredAt(now);
        outbox.setTerminalAt(now);
        outbox.setLastErrorCode(null);
        outbox.setClaimToken(null);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markSuppressed(UUID emergencySessionId, UUID claimToken, String reason) {
        EmergencyNotificationOutbox outbox = outboxRepository.findForUpdate(emergencySessionId)
                .orElse(null);
        if (!ownsPendingClaim(outbox, claimToken)) {
            return false;
        }
        suppress(outbox, reason, Instant.now());
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markRetry(UUID emergencySessionId, UUID claimToken, String errorCode) {
        EmergencyNotificationOutbox outbox = outboxRepository.findForUpdate(emergencySessionId).orElse(null);
        if (!ownsPendingClaim(outbox, claimToken)) {
            return false;
        }

        Instant now = Instant.now();
        EmergencySession session = emergencySessionRepository.findById(emergencySessionId).orElse(null);
        if (session == null) {
            suppress(outbox, "MISSING_EMERGENCY_SESSION", now);
            return true;
        }
        if (session.getStatus() != EmergencyStatus.ACTIVE) {
            suppress(outbox, "EMERGENCY_NOT_ACTIVE", now);
            return true;
        }

        long delaySeconds = Math.min(
                MAX_RETRY_DELAY.toSeconds(),
                1L << Math.min(outbox.getAttemptCount(), 8));
        outbox.setStatus(EmergencyNotificationOutbox.PENDING);
        outbox.setLastErrorCode(errorCode);
        outbox.setNextAttemptAt(now.plusSeconds(delaySeconds));
        outbox.setClaimToken(null);
        return true;
    }

    private boolean ownsPendingClaim(EmergencyNotificationOutbox outbox, UUID claimToken) {
        return outbox != null
                && EmergencyNotificationOutbox.PENDING.equals(outbox.getStatus())
                && claimToken != null
                && claimToken.equals(outbox.getClaimToken());
    }

    private void suppress(EmergencyNotificationOutbox outbox, String reason, Instant now) {
        outbox.setStatus(EmergencyNotificationOutbox.SUPPRESSED);
        outbox.setLastErrorCode(reason);
        outbox.setClaimToken(null);
        outbox.setTerminalAt(now);
    }
}
