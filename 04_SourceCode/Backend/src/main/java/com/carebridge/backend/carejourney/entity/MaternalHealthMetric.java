package com.carebridge.backend.carejourney.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "maternal_health_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaternalHealthMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "metric_id")
    private UUID metricId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "metric_type", length = 40)
    private String metricType;

    @Column(name = "value_numeric")
    private BigDecimal valueNumeric;

    @Column(name = "value_secondary")
    private BigDecimal valueSecondary;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "measured_at")
    private Instant measuredAt;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "source_reference_id")
    private UUID sourceReferenceId;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
