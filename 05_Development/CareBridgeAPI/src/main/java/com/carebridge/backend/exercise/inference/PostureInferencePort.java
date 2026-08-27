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

    /**
     * @param stage movement phase when the provider reports one, otherwise {@code null}.
     *              Carried separately from {@code predictedClass} because a provider may
     *              replace the class with an error verdict and lose the phase with it.
     */
    record InferenceResult(
            String modelVersion,
            String exerciseKey,
            long sequenceNumber,
            String predictedClass,
            BigDecimal confidence,
            boolean correct,
            BigDecimal score,
            List<InferenceFeedback> feedback,
            String stage) {
    }

    record InferenceFeedback(String code, String severity, String message) {
    }
}
