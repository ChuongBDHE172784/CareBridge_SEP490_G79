package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
    @Column(name = "metric_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "journey_id", nullable = false)
    private UUID journeyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    private MetricType metricType;

    @Column(name = "value_numeric", precision = 10, scale = 2)
    private BigDecimal valueNumeric;

    @Column(name = "value_secondary", precision = 10, scale = 2)
    private BigDecimal valueSecondary;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private DataSource sourceType;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MetricStatus status = MetricStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
