package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.EmergencyAlertDelivery;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyAlertDeliveryRepository extends JpaRepository<EmergencyAlertDelivery, UUID> {
    boolean existsByEmergencySessionId(UUID emergencySessionId);
    Optional<EmergencyAlertDelivery> findByEmergencySessionIdAndDeviceTokenId(
            UUID emergencySessionId, UUID deviceTokenId);
}
