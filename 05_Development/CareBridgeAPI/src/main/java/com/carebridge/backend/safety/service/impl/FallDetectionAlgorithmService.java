package com.carebridge.backend.safety.service.impl;

import com.carebridge.backend.safety.SensitivityLevel;
import com.carebridge.backend.safety.SafetyEventType;
import com.carebridge.backend.safety.service.FallAnalysisResult;
import com.carebridge.backend.safety.service.IFallDetectionAlgorithmService;
import com.carebridge.backend.safety.service.ImuDataPayload;
import org.springframework.stereotype.Service;

@Service
public class FallDetectionAlgorithmService implements IFallDetectionAlgorithmService {

    private static final double GRAVITY = 9.81;

    @Override
    public FallAnalysisResult analyze(ImuDataPayload payload, String sensitivityLevel) {
        double magnitude = Math.sqrt(
                payload.accelerometerX() * payload.accelerometerX() +
                payload.accelerometerY() * payload.accelerometerY() +
                payload.accelerometerZ() * payload.accelerometerZ()
        ) - GRAVITY;

        double threshold = SensitivityLevel.valueOf(sensitivityLevel.toUpperCase()).getThreshold();

        if (magnitude >= threshold) {
            SafetyEventType eventType = magnitude >= threshold * 1.5
                    ? SafetyEventType.SUSPECTED_FALL
                    : SafetyEventType.SUSPECTED_IMPACT;
            return new FallAnalysisResult(true, eventType, magnitude);
        }
        return new FallAnalysisResult(false, SafetyEventType.FALSE_ALARM, magnitude);
    }
}
