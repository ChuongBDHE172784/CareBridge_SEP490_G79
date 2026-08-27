package com.carebridge.backend.safety.service;

public interface IFallDetectionAlgorithmService {
    FallAnalysisResult analyze(ImuDataPayload payload, String sensitivityLevel);
}
