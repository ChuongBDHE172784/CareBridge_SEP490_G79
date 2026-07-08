package com.carebridge.backend.safety;

import com.carebridge.backend.safety.dto.response.ImuMonitoringSessionResponse;
import com.carebridge.backend.safety.entity.ImuMonitoringSession;
import com.carebridge.backend.safety.event.FallDetectionDisabled;
import com.carebridge.backend.safety.event.FallDetectionEnabled;
import com.carebridge.backend.safety.repository.IImuMonitoringSessionRepository;
import com.carebridge.backend.safety.service.impl.FallDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FallDetectionServiceTest {

    @Mock
    private IImuMonitoringSessionRepository imuSessionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FallDetectionService fallDetectionService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    void enable_noActiveSession_shouldCreateNew() {
        // FD-TC-001
        when(imuSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(imuSessionRepository.save(any())).thenReturn(SafetyConfigTestFactory.makeActiveSession());

        ImuMonitoringSessionResponse result = fallDetectionService.enable(USER_ID, "MEDIUM");

        verify(imuSessionRepository).save(any(ImuMonitoringSession.class));
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void enable_activeSessionExists_shouldReturnExisting() {
        // FD-TC-002 — idempotent
        ImuMonitoringSession existing = SafetyConfigTestFactory.makeActiveSession();
        when(imuSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));

        ImuMonitoringSessionResponse result = fallDetectionService.enable(USER_ID, "MEDIUM");

        verify(imuSessionRepository, never()).save(any());
        assertThat(result.getSessionId()).isEqualTo(existing.getId());
    }

    @Test
    void enable_shouldPublishFallDetectionEnabledEvent() {
        // FD-TC-004
        when(imuSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(imuSessionRepository.save(any())).thenReturn(SafetyConfigTestFactory.makeActiveSession());

        fallDetectionService.enable(USER_ID, "MEDIUM");

        ArgumentCaptor<FallDetectionEnabled> captor = ArgumentCaptor.forClass(FallDetectionEnabled.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(FallDetectionEnabled.class);
    }

    @Test
    void disable_activeSessionExists_shouldSetStatusStopped() {
        // DIS-TC-001 / FD-TC-005
        ImuMonitoringSession active = SafetyConfigTestFactory.makeActiveSession();
        when(imuSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(active));
        when(imuSessionRepository.save(any())).thenReturn(active);

        fallDetectionService.disable(USER_ID);

        ArgumentCaptor<ImuMonitoringSession> captor = ArgumentCaptor.forClass(ImuMonitoringSession.class);
        verify(imuSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ImuSessionStatus.STOPPED);
        assertThat(captor.getValue().getEndedAt()).isNotNull();
        verify(imuSessionRepository, never()).delete(any());
    }

    @Test
    void disable_noActiveSession_shouldBeNoOp() {
        // DIS-TC-002
        when(imuSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatCode(() -> fallDetectionService.disable(USER_ID)).doesNotThrowAnyException();
        verify(imuSessionRepository, never()).save(any());
    }

    @Test
    void disable_shouldPublishFallDetectionDisabledEvent() {
        // DIS-TC-004
        ImuMonitoringSession active = SafetyConfigTestFactory.makeActiveSession();
        when(imuSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(active));
        when(imuSessionRepository.save(any())).thenReturn(active);

        fallDetectionService.disable(USER_ID);

        verify(eventPublisher).publishEvent(any(FallDetectionDisabled.class));
    }
}
