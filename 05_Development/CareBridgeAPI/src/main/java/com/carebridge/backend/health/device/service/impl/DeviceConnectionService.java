package com.carebridge.backend.health.device.service.impl;

import com.carebridge.backend.health.device.dto.ConnectDeviceRequest;
import com.carebridge.backend.health.device.dto.DeviceConnectionResponse;
import com.carebridge.backend.health.device.entity.DeviceConnectionStatus;
import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import com.carebridge.backend.health.device.event.DeviceConnected;
import com.carebridge.backend.health.device.event.DeviceDisconnected;
import com.carebridge.backend.health.device.exception.DeviceOperationException;
import com.carebridge.backend.health.device.mapper.DeviceConnectionMapper;
import com.carebridge.backend.health.device.repository.IHealthDeviceConnectionRepository;
import com.carebridge.backend.health.device.service.IDeviceConnectionService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceConnectionService implements IDeviceConnectionService {

    private final IHealthDeviceConnectionRepository connectionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public DeviceConnectionResponse connect(ConnectDeviceRequest request, UUID userId) {
        if (!Boolean.TRUE.equals(request.consentAccepted())) {
            throw new DeviceOperationException("DEVICE-002", HttpStatus.BAD_REQUEST, "Consent is required");
        }

        Optional<HealthDeviceConnection> existing = connectionRepository
                .findFirstByUserIdAndProviderNameAndStatusOrderByCreatedAtDesc(
                        userId, request.providerName(), DeviceConnectionStatus.ACTIVE);
        if (existing.isPresent()) {
            return DeviceConnectionMapper.toResponse(existing.get());
        }

        Instant now = Instant.now();
        HealthDeviceConnection entity = HealthDeviceConnection.builder()
                .userId(userId)
                .providerName(request.providerName())
                .deviceName(request.deviceName())
                .scopesJson(request.scopes() == null ? null : String.join(",", request.scopes()))
                .tokenReference(request.tokenReference())
                .consentGrantedAt(now)
                .status(DeviceConnectionStatus.ACTIVE)
                .build();

        HealthDeviceConnection saved = connectionRepository.save(entity);
        eventPublisher.publishEvent(new DeviceConnected(
                UUID.randomUUID(), now, saved.getConnectionId(), userId, saved.getProviderName()));
        return DeviceConnectionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DeviceConnectionResponse disconnect(UUID connectionId, UUID userId) {
        HealthDeviceConnection entity = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new DeviceOperationException(
                        "DEVICE-203", HttpStatus.CONFLICT, "Connection is already disconnected or not found"));
        if (!entity.getUserId().equals(userId)) {
            throw new DeviceOperationException("DEVICE-204", HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
        if (entity.getStatus() != DeviceConnectionStatus.ACTIVE) {
            throw new DeviceOperationException(
                    "DEVICE-203", HttpStatus.CONFLICT, "Connection is already disconnected or not found");
        }

        Instant now = Instant.now();
        entity.setStatus(DeviceConnectionStatus.REVOKED);
        entity.setUpdatedAt(now);
        HealthDeviceConnection saved = connectionRepository.save(entity);
        eventPublisher.publishEvent(new DeviceDisconnected(
                UUID.randomUUID(), now, saved.getConnectionId(), userId, saved.getProviderName()));
        return DeviceConnectionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceConnectionResponse> getActiveConnection(UUID userId) {
        return connectionRepository.findByUserIdAndStatus(userId, DeviceConnectionStatus.ACTIVE)
                .stream()
                .findFirst()
                .map(DeviceConnectionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceConnectionResponse> listConnections(UUID userId) {
        return connectionRepository.findByUserIdAndStatus(userId, DeviceConnectionStatus.ACTIVE)
                .stream()
                .map(DeviceConnectionMapper::toResponse)
                .toList();
    }
}
