package com.carebridge.backend.health.device.repository;

import com.carebridge.backend.health.device.entity.DeviceMeasurement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDeviceMeasurementRepository extends JpaRepository<DeviceMeasurement, UUID> {

    boolean existsByConnectionIdAndSourceRecordId(UUID connectionId, UUID sourceRecordId);

    List<DeviceMeasurement> findByConnectionIdOrderByMeasuredAtDesc(UUID connectionId);
}
