package com.carebridge.backend.safety.service;

import java.time.Instant;

public record ImuDataPayload(
        double accelerometerX,
        double accelerometerY,
        double accelerometerZ,
        double gyroscopeX,
        double gyroscopeY,
        double gyroscopeZ,
        Instant timestamp
) {}
