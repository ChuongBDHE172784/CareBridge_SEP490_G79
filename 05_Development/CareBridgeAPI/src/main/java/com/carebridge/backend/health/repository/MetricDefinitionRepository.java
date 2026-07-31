package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.MetricDefinition;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetricDefinitionRepository extends JpaRepository<MetricDefinition, UUID> {

    Optional<MetricDefinition> findByMetricCodeAndActiveTrue(String metricCode);

    Optional<MetricDefinition> findByMetricCodeAndVersion(String metricCode, int version);

    @Query("""
            SELECT definition
              FROM MetricDefinition definition
             WHERE definition.active = true
               AND definition.effectiveFrom <= :effectiveAt
               AND (definition.effectiveUntil IS NULL OR definition.effectiveUntil > :effectiveAt)
             ORDER BY definition.displayName ASC
            """)
    List<MetricDefinition> findAllEffectiveAt(@Param("effectiveAt") Instant effectiveAt);
}
