package com.carebridge.backend.exercise.inference;

import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Resolves only server-owned posture configuration into an inference provider request. */
@Component
@RequiredArgsConstructor
public class PostureInferenceConfigResolver {

    public static final String PINNED_MODEL_VERSION =
            "exercise-correction@202a0a802d8d2e3ea42f00e6a8c47da9cafc09d7";

    private static final Set<String> SUPPORTED_EXERCISES =
            Set.of("bicep_curl", "plank", "squat", "lunge");

    private final ObjectMapper objectMapper;

    /**
     * Phân giải cấu hình PostureAnalysisConfig từ Database thành ResolvedInferenceConfig để gọi AI Sidecar:
     * - Kiểm tra modelVersion cố định (pinned SHA256 version).
     * - Kiểm tra ngưỡng tin cậy confidenceThreshold trong khoảng [0, 1].
     * - Trích xuất exerciseKey từ configJson và kiểm tra xem có thuộc danh sách hỗ trợ ("bicep_curl", "plank", "squat", "lunge") hay không.
     */
    public ResolvedInferenceConfig resolve(PostureAnalysisConfig config) {
        String modelVersion = config.getRuleOrModelVersion();
        if (!PINNED_MODEL_VERSION.equals(modelVersion)) {
            throw unavailable();
        }
        BigDecimal confidenceThreshold = config.getConfidenceThreshold();
        if (confidenceThreshold == null
                || confidenceThreshold.compareTo(BigDecimal.ZERO) < 0
                || confidenceThreshold.compareTo(BigDecimal.ONE) > 0) {
            throw unavailable();
        }

        String configJson = config.getConfigJson();
        if (configJson == null || configJson.isBlank()) {
            throw unavailable();
        }

        try {
            JsonNode root = objectMapper.readTree(configJson);
            JsonNode exerciseKeyNode = root.get("exerciseKey");
            if (exerciseKeyNode == null || !exerciseKeyNode.isTextual()) {
                throw unavailable();
            }
            String exerciseKey = exerciseKeyNode.textValue();
            if (!SUPPORTED_EXERCISES.contains(exerciseKey)) {
                throw unavailable();
            }
            return new ResolvedInferenceConfig(modelVersion, exerciseKey);
        } catch (JsonProcessingException exception) {
            throw unavailable();
        }
    }

    private PostureInferenceUnavailableException unavailable() {
        return new PostureInferenceUnavailableException("CONFIGURATION_INVALID");
    }

    public record ResolvedInferenceConfig(String modelVersion, String exerciseKey) {
    }
}
