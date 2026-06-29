package com.carebridge.backend.safety;

import com.carebridge.backend.safety.dto.request.SafetyConfigRequest;
import com.carebridge.backend.safety.dto.response.SafetyConfigResponse;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import com.carebridge.backend.safety.event.SafetyConfigChanged;
import com.carebridge.backend.safety.repository.ISafetyConfigRepository;
import com.carebridge.backend.safety.service.impl.SafetyConfigService;
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
class SafetyConfigServiceTest {

    @Mock
    private ISafetyConfigRepository configRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SafetyConfigService safetyConfigService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    void configure_firstTime_shouldInsertNewConfig() {
        // SCONFIG-TC-001
        when(configRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(configRepository.save(any())).thenReturn(SafetyConfigTestFactory.makeConfig());

        SafetyConfigResponse result = safetyConfigService.configure(SafetyConfigTestFactory.makeRequest(), USER_ID);

        verify(configRepository).save(any(SafetyMonitoringConfig.class));
        assertThat(result).isNotNull();
    }

    @Test
    void configure_shouldPublishSafetyConfigChangedEvent() {
        // SCONFIG-TC-003
        when(configRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(configRepository.save(any())).thenReturn(SafetyConfigTestFactory.makeConfig());

        safetyConfigService.configure(SafetyConfigTestFactory.makeRequest(), USER_ID);

        ArgumentCaptor<SafetyConfigChanged> captor = ArgumentCaptor.forClass(SafetyConfigChanged.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(SafetyConfigChanged.class);
    }

    @Test
    void getConfig_whenNoRecord_shouldReturnDefault() {
        // SCONFIG-TC-004
        when(configRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        SafetyConfigResponse result = safetyConfigService.getConfig(USER_ID);

        assertThat(result.isFallDetectionEnabled()).isFalse();
        assertThat(result.getSensitivityLevel()).isEqualTo("MEDIUM");
    }
}
