package com.carebridge.backend.emergency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.emergency.repository.EmergencyAlertAttemptRepository;
import com.carebridge.backend.emergency.service.EmergencyAlertAttemptService;
import com.carebridge.backend.emergency.service.EmergencyAlertClaim;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmergencyAlertAttemptServiceTest {

    private static EmergencyAlertClaim claim(UUID sessionId) {
        return new EmergencyAlertClaim(sessionId, 1L, UUID.randomUUID(),
                Instant.now().plusSeconds(120));
    }

    @Test
    void firstClaimIsAcceptedAndConcurrentDuplicateIsRejected() {
        var repository = mock(EmergencyAlertAttemptRepository.class);
        var service = new EmergencyAlertAttemptService(repository);
        UUID sessionId = UUID.randomUUID();
        when(repository.claim(eq(sessionId), any(Instant.class), anyBoolean()))
                .thenReturn(Optional.of(claim(sessionId)), Optional.empty());

        assertThat(service.claim(sessionId)).isPresent();
        assertThat(service.claim(sessionId)).isEmpty();
    }

    @Test
    void expiredOrFailedAttemptCanBeReclaimed() {
        var repository = mock(EmergencyAlertAttemptRepository.class);
        var service = new EmergencyAlertAttemptService(repository);
        UUID sessionId = UUID.randomUUID();
        when(repository.claim(eq(sessionId), any(Instant.class), anyBoolean()))
                .thenReturn(Optional.of(claim(sessionId)), Optional.of(claim(sessionId)));

        assertThat(service.claim(sessionId)).isPresent();
        assertThat(service.claim(sessionId)).isPresent();
    }
}
