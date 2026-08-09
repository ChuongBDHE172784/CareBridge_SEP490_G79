package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.repository.EmergencyAlertAttemptRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmergencyAlertAttemptService {
    private static final Duration LEASE_DURATION = Duration.ofMinutes(2);

    private final EmergencyAlertAttemptRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<EmergencyAlertClaim> claim(UUID sessionId) {
        return repository.claim(sessionId, Instant.now().plus(LEASE_DURATION), false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<EmergencyAlertClaim> claimForRealert(UUID sessionId) {
        return repository.claim(sessionId, Instant.now().plus(LEASE_DURATION), true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean renew(EmergencyAlertClaim claim) {
        return repository.renew(claim, Instant.now().plus(LEASE_DURATION));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(
            EmergencyAlertClaim claim,
            String status,
            int successfulRecipients,
            int failedRecipients,
            boolean locationIncluded) {
        return repository.complete(claim, status, successfulRecipients,
                failedRecipients, locationIncluded);
    }
}
