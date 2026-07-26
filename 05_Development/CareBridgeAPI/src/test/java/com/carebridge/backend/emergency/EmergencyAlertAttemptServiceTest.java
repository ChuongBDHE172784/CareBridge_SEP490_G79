package com.carebridge.backend.emergency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.emergency.repository.EmergencyAlertAttemptRepository;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.service.EmergencyAlertAttemptService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmergencyAlertAttemptServiceTest {

    @Test
    void firstClaimIsAcceptedAndConcurrentDuplicateIsRejected() {
        var repository = mock(EmergencyAlertAttemptRepository.class);
        var service = new EmergencyAlertAttemptService(repository,
                mock(IFamilyAlertLogRepository.class), mock(IEmergencySessionRepository.class));
        UUID sessionId = UUID.randomUUID();
        when(repository.claim(eq(sessionId), any(Instant.class))).thenReturn(1, 0);

        assertThat(service.claim(sessionId)).isTrue();
        assertThat(service.claim(sessionId)).isFalse();
    }

    @Test
    void expiredOrFailedAttemptCanBeReclaimed() {
        var repository = mock(EmergencyAlertAttemptRepository.class);
        var service = new EmergencyAlertAttemptService(repository,
                mock(IFamilyAlertLogRepository.class), mock(IEmergencySessionRepository.class));
        UUID sessionId = UUID.randomUUID();
        when(repository.claim(eq(sessionId), any(Instant.class))).thenReturn(1, 1);

        assertThat(service.claim(sessionId)).isTrue();
        assertThat(service.claim(sessionId)).isTrue();
    }
}
