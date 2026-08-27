package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthObservationRepository extends JpaRepository<HealthObservation, UUID> {

    Optional<HealthObservation> findByLegacySourceAndLegacyId(String legacySource, String legacyId);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE health_observation_id = :id
               AND legacy_source = 'maternal_health_observations'
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
            """, nativeQuery = true)
    Optional<HealthObservation> findByIdAndStatus(
            @Param("id") UUID id,
            @Param("status") MetricStatus status);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE health_observation_id = :id
               AND care_subject_id = :careSubjectId
               AND legacy_source = 'maternal_health_observations'
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
            """, nativeQuery = true)
    Optional<HealthObservation> findByIdAndCareSubjectIdAndStatus(
            @Param("id") UUID id,
            @Param("careSubjectId") UUID careSubjectId,
            @Param("status") MetricStatus status);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE care_subject_id = :careSubjectId
               AND legacy_source = 'maternal_health_observations'
               AND observation_type = :metricCode
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
               AND observed_at BETWEEN :from AND :to
             ORDER BY observed_at ASC
            """, nativeQuery = true)
    List<HealthObservation> findTrend(
            @Param("careSubjectId") UUID careSubjectId,
            @Param("metricCode") String metricCode,
            @Param("status") MetricStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = """
            SELECT DISTINCT ON (observation_type) *
              FROM health_observations
             WHERE care_subject_id = :careSubjectId
               AND legacy_source = 'maternal_health_observations'
               AND observation_type IN (:metricCodes)
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
             ORDER BY observation_type, observed_at DESC, health_observation_id DESC
            """, nativeQuery = true)
    List<HealthObservation> findLatestByMetricCodes(
            @Param("careSubjectId") UUID careSubjectId,
            @Param("metricCodes") List<String> metricCodes,
            @Param("status") MetricStatus status);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE care_subject_id = :careSubjectId
               AND legacy_source = 'maternal_health_observations'
               AND observation_type IN (:metricCodes)
               AND COALESCE(raw_payload_jsonb->>'recordStatus', 'ACTIVE') = :#{#status.name()}
               AND observed_at BETWEEN :from AND :to
             ORDER BY observed_at ASC
            """, nativeQuery = true)
    List<HealthObservation> findTrendByMetricCodes(
            @Param("careSubjectId") UUID careSubjectId,
            @Param("metricCodes") List<String> metricCodes,
            @Param("status") MetricStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);

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
               AND legacy_source = 'maternal_health_observations'
            """, nativeQuery = true)
    int updateStatus(@Param("id") UUID id, @Param("status") MetricStatus status);

    // --- Baby growth sessions (wave 13). Scoped by legacy_source so these rows can never
    // --- surface in the maternal queries above, which are all filtered the same way.

    @Query(value = """
            SELECT * FROM health_observations
             WHERE legacy_source = :legacySource
               AND care_subject_id = :careSubjectId
             ORDER BY observed_at ASC, observation_type ASC
            """, nativeQuery = true)
    List<HealthObservation> findGrowthByCareSubject(
            @Param("legacySource") String legacySource,
            @Param("careSubjectId") UUID careSubjectId);

    @Query(value = """
            SELECT * FROM health_observations
             WHERE legacy_source = :legacySource
               AND measurement_group_id = :measurementGroupId
             ORDER BY observation_type ASC
            """, nativeQuery = true)
    List<HealthObservation> findGrowthByMeasurementGroup(
            @Param("legacySource") String legacySource,
            @Param("measurementGroupId") UUID measurementGroupId);
}
