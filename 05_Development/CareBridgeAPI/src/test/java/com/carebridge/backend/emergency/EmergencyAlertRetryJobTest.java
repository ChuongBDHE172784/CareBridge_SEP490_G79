package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.service.EmergencyAlertRetryJob;
import com.carebridge.backend.emergency.service.IFamilyAlertService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyAlertRetryJobTest {

    @Mock private IEmergencySessionRepository emergencySessionRepository;
    @Mock private IFamilyAlertService familyAlertService;
    @InjectMocks private EmergencyAlertRetryJob retryJob;

    @Test
    void retryPendingAlertsUsesOneMinuteCutoffAndDispatchesExistingCandidate() {
        UUID sessionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        EmergencySession session = EmergencySession.builder()
                .id(sessionId)
                .userId(ownerId)
                .status(EmergencyStatus.ACTIVE)
                .triggerSource("TRIAGE")
                .createdAt(Instant.parse("2026-07-25T00:00:00Z"))
                .build();
        when(emergencySessionRepository.findAlertRetryCandidates(any()))
                .thenReturn(List.of(sessionId));
        when(emergencySessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        Instant before = Instant.now().minusSeconds(61);

        retryJob.retryPendingAlerts();

        Instant after = Instant.now().minusSeconds(59);
        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(emergencySessionRepository).findAlertRetryCandidates(cutoff.capture());
        assertThat(cutoff.getValue()).isBetween(before, after);
        verify(familyAlertService).sendAlert(argThat(event ->
                sessionId.equals(event.sessionId())
                        && ownerId.equals(event.userId())
                        && "TRIAGE".equals(event.triggerSource())));
    }

    @Test
    void oneRetryFailureDoesNotPreventLaterCandidate() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        EmergencySession first = session(firstId);
        EmergencySession second = session(secondId);
        when(emergencySessionRepository.findAlertRetryCandidates(any()))
                .thenReturn(List.of(firstId, secondId));
        when(emergencySessionRepository.findById(firstId)).thenReturn(Optional.of(first));
        when(emergencySessionRepository.findById(secondId)).thenReturn(Optional.of(second));
        doThrow(new RuntimeException("synthetic failure"))
                .when(familyAlertService).sendAlert(argThat(event -> firstId.equals(event.sessionId())));

        retryJob.retryPendingAlerts();

        verify(familyAlertService).sendAlert(argThat(event -> secondId.equals(event.sessionId())));
    }

    private EmergencySession session(UUID sessionId) {
        return EmergencySession.builder()
                .id(sessionId)
                .userId(UUID.randomUUID())
                .status(EmergencyStatus.ACTIVE)
                .triggerSource("TRIAGE")
                .createdAt(Instant.parse("2026-07-25T00:00:00Z"))
                .build();
    }

    private static <T> T argThat(org.mockito.ArgumentMatcher<T> matcher) {
        return org.mockito.ArgumentMatchers.argThat(matcher);
    }
}
