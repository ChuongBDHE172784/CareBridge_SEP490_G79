package com.carebridge.backend.health.device.event;

import com.carebridge.backend.health.entity.MetricType;
import java.time.Instant;
import java.util.UUID;

public record DeviceDataImported(UUID eventId, Instant timestamp, UUID metricId, UUID userId, UUID journeyId,
                                 MetricType metricType) {
}
