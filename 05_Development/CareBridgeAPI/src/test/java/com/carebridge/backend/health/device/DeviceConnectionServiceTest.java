package com.carebridge.backend.health.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.health.device.dto.ConnectDeviceRequest;
import com.carebridge.backend.health.device.entity.DeviceConnectionStatus;
import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import com.carebridge.backend.health.device.exception.DeviceOperationException;
import com.carebridge.backend.health.device.repository.IHealthDeviceConnectionRepository;
import com.carebridge.backend.health.device.service.impl.DeviceConnectionService;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DeviceConnectionServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private IHealthDeviceConnectionRepository connectionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeviceConnectionService service;

    @Test
    void connectCreatesActiveConnectionWithConsentTimestamp() {
        var request = new ConnectDeviceRequest(
                "SMARTWATCH_GENERIC", "Mi Band 8", List.of("heart_rate"), "token-ref", true);
        when(connectionRepository.findFirstByUserIdAndProviderNameAndStatusOrderByCreatedAtDesc(
                        USER_ID, "SMARTWATCH_GENERIC", DeviceConnectionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(connectionRepository.save(any())).thenAnswer(invocation -> {
            HealthDeviceConnection entity = invocation.getArgument(0);
            entity.setConnectionId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            return entity;
        });

        var response = service.connect(request, USER_ID);

        ArgumentCaptor<HealthDeviceConnection> captor = ArgumentCaptor.forClass(HealthDeviceConnection.class);
        verify(connectionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeviceConnectionStatus.ACTIVE);
        assertThat(captor.getValue().getConsentGrantedAt()).isNotNull();
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void connectReturnsExistingActiveConnectionWithoutDuplicateSave() {
        HealthDeviceConnection existing = activeConnection(USER_ID);
        when(connectionRepository.findFirstByUserIdAndProviderNameAndStatusOrderByCreatedAtDesc(
                        USER_ID, "SMARTWATCH_GENERIC", DeviceConnectionStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        var response = service.connect(new ConnectDeviceRequest(
                "SMARTWATCH_GENERIC", "Mi Band 8", null, null, true), USER_ID);

        assertThat(response.id()).isEqualTo(existing.getConnectionId());
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void disconnectRevokesOnlyOwnedActiveConnection() {
        HealthDeviceConnection existing = activeConnection(USER_ID);
        when(connectionRepository.findById(existing.getConnectionId())).thenReturn(Optional.of(existing));
        when(connectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.disconnect(existing.getConnectionId(), USER_ID);

        assertThat(response.status()).isEqualTo("REVOKED");
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void disconnectRejectsWrongOwner() {
        HealthDeviceConnection existing = activeConnection(UUID.randomUUID());
        when(connectionRepository.findById(existing.getConnectionId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.disconnect(existing.getConnectionId(), USER_ID))
                .isInstanceOf(DeviceOperationException.class)
                .hasMessageContaining("Insufficient permissions");
        verify(connectionRepository, never()).save(any());
    }

    private static HealthDeviceConnection activeConnection(UUID userId) {
        return HealthDeviceConnection.builder()
                .connectionId(UUID.randomUUID())
                .userId(userId)
                .providerName("SMARTWATCH_GENERIC")
                .deviceName("Mi Band 8")
                .consentGrantedAt(Instant.now())
                .status(DeviceConnectionStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}

