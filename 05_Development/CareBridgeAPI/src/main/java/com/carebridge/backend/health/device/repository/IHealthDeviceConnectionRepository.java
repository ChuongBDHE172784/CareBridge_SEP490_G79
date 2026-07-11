package com.carebridge.backend.health.device.repository;

import com.carebridge.backend.health.device.entity.DeviceConnectionStatus;
import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IHealthDeviceConnectionRepository extends JpaRepository<HealthDeviceConnection, UUID> {

    List<HealthDeviceConnection> findByStatus(DeviceConnectionStatus status);

    List<HealthDeviceConnection> findByUserIdAndStatus(UUID userId, DeviceConnectionStatus status);

    Optional<HealthDeviceConnection> findByConnectionIdAndUserId(UUID connectionId, UUID userId);

    Optional<HealthDeviceConnection> findFirstByUserIdAndProviderNameAndStatusOrderByCreatedAtDesc(
            UUID userId, String providerName, DeviceConnectionStatus status);
}
