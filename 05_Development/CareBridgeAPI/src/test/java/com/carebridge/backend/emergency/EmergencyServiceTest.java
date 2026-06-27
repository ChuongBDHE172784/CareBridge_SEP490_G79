package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.service.impl.EmergencyService;
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
class EmergencyServiceTest {

    @Mock
    private IEmergencySessionRepository emergencySessionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EmergencyService emergencyService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    void openFlow_noActiveSession_shouldCreateNew() {
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        EmergencySession saved = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.save(any())).thenReturn(saved);

        EmergencySessionResponse result = emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        verify(emergencySessionRepository).save(any(EmergencySession.class));
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void openFlow_activeSessionExists_shouldReturnExisting() {
        // Idempotent — return existing ACTIVE session
        EmergencySession existing = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));

        EmergencySessionResponse result = emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        verify(emergencySessionRepository, never()).save(any());
        assertThat(result.getSessionId()).isEqualTo(existing.getId());
    }

    @Test
    void openFlow_shouldPublishEmergencySessionOpenedEvent() {
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(emergencySessionRepository.save(any())).thenReturn(EmergencyTestFactory.makeActiveSession());

        emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        ArgumentCaptor<EmergencySessionOpened> captor = ArgumentCaptor.forClass(EmergencySessionOpened.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(EmergencySessionOpened.class);
    }
}
