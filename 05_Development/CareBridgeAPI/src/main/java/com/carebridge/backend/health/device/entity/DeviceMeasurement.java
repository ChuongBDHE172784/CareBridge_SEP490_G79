package com.carebridge.backend.health.device.entity;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "device_measurements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "device_measurement_id", updatable = false, nullable = false)
    private UUID deviceMeasurementId;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "measurement_type", nullable = false, length = 50)
    private String measurementType;

    @Column(name = "value_numeric", precision = 10, scale = 2)
    private BigDecimal valueNumeric;

    @Column(name = "value_secondary", precision = 10, scale = 2)
    private BigDecimal valueSecondary;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Column(name = "source_record_id")
    private UUID sourceRecordId;

    @Column(name = "quality_label", length = 30)
    private String qualityLabel;

    @Column(name = "raw_metadata_json", columnDefinition = "jsonb")
    private String rawMetadataJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
