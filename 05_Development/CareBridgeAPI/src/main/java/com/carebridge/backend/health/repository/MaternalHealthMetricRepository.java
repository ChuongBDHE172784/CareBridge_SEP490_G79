package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaternalHealthMetricRepository extends JpaRepository<MaternalHealthMetric, UUID> {

    @Query(value = """
            SELECT * FROM health_observations
             WHERE health_observation_id = :id
               AND legacy_source = 'maternal_health_metrics'
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
            """, nativeQuery = true)
    Optional<MaternalHealthMetric> findByIdAndStatus(
            @Param("id") UUID id,
            @Param("status") MetricStatus status);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE health_observation_id = :id
               AND raw_payload_jsonb->>'journeyId' = CAST(:journeyId AS text)
               AND legacy_source = 'maternal_health_metrics'
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
            """, nativeQuery = true)
    Optional<MaternalHealthMetric> findByIdAndJourneyIdAndStatus(
            @Param("id") UUID id,
            @Param("journeyId") UUID journeyId,
            @Param("status") MetricStatus status);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE raw_payload_jsonb->>'journeyId' = CAST(:journeyId AS text)
               AND legacy_source = 'maternal_health_metrics'
               AND observation_type = :#{#metricType.name()}
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
               AND observed_at BETWEEN :from AND :to
             ORDER BY observed_at ASC
            """, nativeQuery = true)
    List<MaternalHealthMetric> findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
            @Param("journeyId") UUID journeyId,
            @Param("metricType") MetricType metricType,
            @Param("status") MetricStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE raw_payload_jsonb->>'journeyId' = CAST(:journeyId AS text)
               AND legacy_source = 'maternal_health_metrics'
               AND observation_type IN (:types)
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
               AND observed_at BETWEEN :from AND :to
             ORDER BY observed_at ASC
            """, nativeQuery = true)
    List<MaternalHealthMetric> findByJourneyIdAndMetricTypeInAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
            @Param("journeyId") UUID journeyId,
            @Param("types") List<String> types,
            @Param("status") MetricStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE raw_payload_jsonb->>'journeyId' = CAST(:journeyId AS text)
               AND legacy_source = 'maternal_health_metrics'
               AND observation_type = :#{#metricType.name()}
               AND observed_at BETWEEN :from AND :to
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
             ORDER BY observed_at ASC
            """, nativeQuery = true)
    List<MaternalHealthMetric> findByJourneyIdAndMetricTypeAndMeasuredAtBetweenAndStatusOrderByMeasuredAtAsc(
            @Param("journeyId") UUID journeyId,
            @Param("metricType") MetricType metricType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("status") MetricStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE health_observations
               SET raw_payload_jsonb = jsonb_set(
                       COALESCE(raw_payload_jsonb, '{}'::jsonb),
                       '{recordStatus}',
                       to_jsonb(CAST(:#{#status.name()} AS text)),
                       true),
                   updated_at = now()
             WHERE health_observation_id = :id
               AND legacy_source = 'maternal_health_metrics'
            """, nativeQuery = true)
    int updateStatus(@Param("id") UUID id, @Param("status") MetricStatus status);
}
