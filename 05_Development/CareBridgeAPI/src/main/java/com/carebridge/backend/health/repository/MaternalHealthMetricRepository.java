package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MaternalHealthMetricRepository extends JpaRepository<MaternalHealthMetric, UUID> {

    Optional<MaternalHealthMetric> findByIdAndStatus(UUID id, MetricStatus status);
}
