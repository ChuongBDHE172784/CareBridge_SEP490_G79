package com.carebridge.backend.device.entity;

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
@Table(name = "device_measurements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "device_measurement_id")
    private UUID deviceMeasurementId;

    @Column(name = "connection_id")
    private UUID connectionId;

    @Column(name = "measurement_type", length = 40)
    private String measurementType;

    @Column(name = "value_numeric")
    private BigDecimal valueNumeric;

    @Column(name = "value_secondary")
    private BigDecimal valueSecondary;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "measured_at")
    private Instant measuredAt;

    @Column(name = "source_record_id", length = 150)
    private String sourceRecordId;

    @Column(name = "quality_label", length = 30)
    private String qualityLabel;

    @Column(name = "raw_metadata_json", columnDefinition = "jsonb")
    private String rawMetadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
