package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.repository.EmergencyNotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyNotificationOutboxDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyNotificationOutboxDeliveryService.class);
    private final EmergencyNotificationOutboxWriter outboxWriter;
    private final IFamilyAlertService familyAlertService;
    private final EmergencyNotificationOutboxRepository outboxRepository;

    @Transactional
    public void deliver(UUID emergencySessionId) {
        if (!outboxRepository.tryAcquireDeliveryLock(emergencySessionId)) {
            log.info("Emergency notification delivery outcome=ALREADY_IN_PROGRESS");
            return;
        }

        EmergencyNotificationOutboxWriter.ClaimedNotification claim;
        try {
            claim = outboxWriter.claim(emergencySessionId);
        } catch (RuntimeException claimFailure) {
            log.error("Emergency notification claim failed reason={}",
                    claimFailure.getClass().getSimpleName());
            return;
        }
        if (claim == null) {
            return;
        }

        try {
            FamilyAlertDeliveryOutcome outcome = familyAlertService.sendAlert(claim.event());
            if (outcome == FamilyAlertDeliveryOutcome.NO_RECIPIENTS) {
                boolean suppressed = outboxWriter.markSuppressed(
                        emergencySessionId, claim.claimToken(), "NO_RECIPIENTS");
                log.info("Emergency notification delivery transition outcome={}",
                        suppressed ? "SUPPRESSED_NO_RECIPIENTS" : "STALE_CLAIM_IGNORED");
                return;
            }
            if (outcome == null) {
                throw new IllegalStateException("Family alert delivery returned no outcome");
            }
            boolean delivered = outboxWriter.markDelivered(emergencySessionId, claim.claimToken());
            log.info("Emergency notification delivery transition outcome={}",
                    delivered ? "DELIVERED" : "STALE_CLAIM_IGNORED");
        } catch (RuntimeException exception) {
            String errorCode = exception.getClass().getSimpleName();
            try {
                boolean transitioned = outboxWriter.markRetry(
                        emergencySessionId, claim.claimToken(), errorCode);
                if (!transitioned) {
                    log.warn("Emergency notification retry transition outcome=STALE_CLAIM_IGNORED");
                }
            } catch (RuntimeException retryWriteFailure) {
                log.error("Emergency notification retry state failed reason={}",
                        retryWriteFailure.getClass().getSimpleName());
            }
            log.warn("Emergency notification delivery deferred reason={}", errorCode);
        }
    }
}
