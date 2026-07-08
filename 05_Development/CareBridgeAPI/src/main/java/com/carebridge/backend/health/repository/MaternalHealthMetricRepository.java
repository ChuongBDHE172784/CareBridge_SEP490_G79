package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaternalHealthMetricRepository extends JpaRepository<MaternalHealthMetric, UUID> {

    Optional<MaternalHealthMetric> findByIdAndStatus(UUID id, MetricStatus status);

    Optional<MaternalHealthMetric> findByIdAndJourneyIdAndStatus(UUID id, UUID journeyId, MetricStatus status);

    List<MaternalHealthMetric> findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
            UUID journeyId, MetricType metricType, MetricStatus status, Instant from, Instant to);
}
