package com.carebridge.backend.integration.wearable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RawMeasurement(
        UUID sourceRecordId,
        String measurementType,
        BigDecimal valueNumeric,
        BigDecimal valueSecondary,
        String unit,
        Instant measuredAt,
        String qualityLabel,
        String rawMetadataJson) {
}
