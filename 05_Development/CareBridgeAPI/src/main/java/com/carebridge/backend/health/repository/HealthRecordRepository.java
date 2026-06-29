package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {

    Optional<HealthRecord> findByIdAndStatus(UUID id, HealthRecordStatus status);
}
