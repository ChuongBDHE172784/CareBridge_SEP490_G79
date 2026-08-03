package com.carebridge.backend.exercise.inference;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Provider-neutral boundary for posture inference. The request intentionally contains no
 * CareBridge user, journey, or session identifiers and no raw media.
 */
public interface PostureInferencePort {

    InferenceResult infer(InferenceRequest request);

    record InferenceRequest(
            String modelVersion,
            String exerciseKey,
            long sequenceNumber,
            Map<String, Object> landmarks) {
    }

    record InferenceResult(
            String modelVersion,
            String exerciseKey,
            long sequenceNumber,
            String predictedClass,
            BigDecimal confidence,
            boolean correct,
            BigDecimal score,
            List<InferenceFeedback> feedback) {
    }

    record InferenceFeedback(String code, String severity, String message) {
    }
}
