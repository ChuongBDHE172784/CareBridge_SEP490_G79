package com.carebridge.backend.safety.service;

import java.time.Instant;
import java.math.BigDecimal;

public record ImuDataPayload(
        double accelerometerX,
        double accelerometerY,
        double accelerometerZ,
        double gyroscopeX,
        double gyroscopeY,
        double gyroscopeZ,
        Instant timestamp,
        String signalId,
        BigDecimal latitude,
        BigDecimal longitude
) {}
