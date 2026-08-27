package com.carebridge.backend.safety.service.impl;

import com.carebridge.backend.safety.SensitivityLevel;
import com.carebridge.backend.safety.SafetyEventType;
import com.carebridge.backend.safety.service.FallAnalysisResult;
import com.carebridge.backend.safety.service.IFallDetectionAlgorithmService;
import com.carebridge.backend.safety.service.ImuDataPayload;
import org.springframework.stereotype.Service;

/**
 * Service tính toán và phân tích ngưỡng gia tốc trên Backend khi không có cờ xác nhận từ thiết bị.
 */
@Service
public class FallDetectionAlgorithmService implements IFallDetectionAlgorithmService {

    /** Trọng lực chuẩn của Trái Đất (~9.81 m/s²). */
    private static final double GRAVITY = 9.81;

    /**
     * Phân tích độ lớn vector gia tốc $\sqrt{X^2 + Y^2 + Z^2} - 9.81$ so với ngưỡng độ nhạy [SensitivityLevel]:
     * - LOW: Ngưỡng 15.0 m/s² (~2.5G)
     * - MEDIUM: Ngưỡng 12.0 m/s² (~2.2G)
     * - HIGH: Ngưỡng 9.0 m/s² (~1.9G)
     */
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
