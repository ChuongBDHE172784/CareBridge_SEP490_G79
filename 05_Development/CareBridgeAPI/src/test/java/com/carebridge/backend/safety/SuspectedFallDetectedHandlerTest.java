package com.carebridge.backend.safety;

import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import com.carebridge.backend.safety.event.SuspectedFallDetected;
import com.carebridge.backend.safety.repository.ISafetyConfigRepository;
import com.carebridge.backend.safety.service.SuspectedFallDetectedHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuspectedFallDetectedHandlerTest {

    @Mock
    private ISafetyConfigRepository safetyConfigRepository;

    @Mock
    private IEmergencyService emergencyService;

    @InjectMocks
    private SuspectedFallDetectedHandler handler;

    private SuspectedFallDetected event(String eventType) {
        return new SuspectedFallDetected(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                eventType, 33.5, BigDecimal.valueOf(10.1), BigDecimal.valueOf(106.2), Instant.now());
    }

    @Test
    void onSuspectedFallDetected_withAutoAlertEnabled_shouldOpenEmergencyFlow() {
        SuspectedFallDetected evt = event("SUSPECTED_FALL");
        SafetyMonitoringConfig config = SafetyMonitoringConfig.builder().emergencyAutoAlert(true).build();
        when(safetyConfigRepository.findByUserId(evt.userId())).thenReturn(Optional.of(config));

        handler.onSuspectedFallDetected(evt);

        verify(emergencyService).openFlow(any(OpenEmergencyRequest.class), eq(evt.userId()));
    }

    @Test
    void onSuspectedFallDetected_withAutoAlertDisabled_shouldNotEscalate() {
        SuspectedFallDetected evt = event("SUSPECTED_FALL");
        SafetyMonitoringConfig config = SafetyMonitoringConfig.builder().emergencyAutoAlert(false).build();
        when(safetyConfigRepository.findByUserId(evt.userId())).thenReturn(Optional.of(config));

        handler.onSuspectedFallDetected(evt);

        verifyNoInteractions(emergencyService);
    }

    @Test
    void onSuspectedFallDetected_noConfigRow_shouldDefaultToNoEscalation() {
        SuspectedFallDetected evt = event("SUSPECTED_FALL");
        when(safetyConfigRepository.findByUserId(evt.userId())).thenReturn(Optional.empty());

        handler.onSuspectedFallDetected(evt);

        verifyNoInteractions(emergencyService);
    }

    @Test
    void onSuspectedFallDetected_suspectedImpactOnly_shouldNotEscalate() {
        SuspectedFallDetected evt = event("SUSPECTED_IMPACT");

        handler.onSuspectedFallDetected(evt);

        verifyNoInteractions(safetyConfigRepository);
        verifyNoInteractions(emergencyService);
    }
}
