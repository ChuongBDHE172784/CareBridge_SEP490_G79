package com.carebridge.backend.emergency.service;

import com.carebridge.backend.emergency.repository.EmergencyAlertAttemptRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Holds the emergency-session row lock across the provider call and immutable
 * result insert. Resolution and expired-lease reclaim therefore serialize on
 * the same row: either they commit first and suppress the send, or the provider
 * result commits first and later workers observe it before they can resend.
 */
@Service
public class EmergencyAlertProviderFence {

    private final EmergencyAlertAttemptRepository attemptRepository;
    private final TransactionTemplate requiresNew;
    private final long leaseMillis;

    public EmergencyAlertProviderFence(
            PlatformTransactionManager transactionManager,
            EmergencyAlertAttemptRepository attemptRepository,
            @Value("${carebridge.emergency.alert-provider-fence-ms:120000}") long leaseMillis) {
        this.attemptRepository = attemptRepository;
        this.leaseMillis = leaseMillis;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Optional<FencedAlertDelivery> execute(
            EmergencyAlertClaim claim,
            Supplier<FencedAlertDelivery> providerCallAndRecorder) {
        return requiresNew.execute(status -> {
            boolean current = attemptRepository.renew(
                    claim, Instant.now().plusMillis(leaseMillis));
            if (!current) {
                return Optional.empty();
            }
            return Optional.of(providerCallAndRecorder.get());
        });
    }
}
