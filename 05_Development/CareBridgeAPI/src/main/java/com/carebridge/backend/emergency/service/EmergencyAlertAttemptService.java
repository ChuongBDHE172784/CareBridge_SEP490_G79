package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.repository.EmergencyAlertAttemptRepository;
import com.carebridge.backend.emergency.entity.FamilyAlertLog;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmergencyAlertAttemptService {
    private final EmergencyAlertAttemptRepository repository;
    private final IFamilyAlertLogRepository familyAlertLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(UUID sessionId) {
        return repository.claim(sessionId, Instant.now().plus(Duration.ofMinutes(2))) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID sessionId, String status, int successfulRecipients, int failedRecipients,
                         boolean locationIncluded) {
        repository.findById(sessionId).ifPresent(attempt -> {
            Instant now = Instant.now();
            attempt.setStatus(status);
            attempt.setCompletedAt(now);
            attempt.setSuccessfulRecipientCount(successfulRecipients);
            attempt.setFailedRecipientCount(failedRecipients);
            attempt.setUpdatedAt(now);
            repository.save(attempt);
        });
        if (successfulRecipients > 0) {
            FamilyAlertLog alertLog = familyAlertLogRepository.findBySessionId(sessionId)
                    .orElseGet(() -> FamilyAlertLog.builder()
                            .sessionId(sessionId)
                            .createdBy("SYSTEM")
                            .build());
            alertLog.setSentAt(Instant.now());
            alertLog.setRecipientCount(successfulRecipients);
            alertLog.setLocationIncluded(alertLog.isLocationIncluded() || locationIncluded);
            familyAlertLogRepository.save(alertLog);
        }
    }
}
