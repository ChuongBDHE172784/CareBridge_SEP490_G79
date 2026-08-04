package com.carebridge.backend.exercise.inference;

import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceFeedback;
import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceRequest;
import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** HTTP adapter for the private, persistent Exercise-Correction sidecar. */
@Component
public class ExerciseCorrectionHttpAdapter implements PostureInferencePort {

    static final String LANDMARK_SCHEMA_VERSION = "mediapipe-pose-landmarks-v1";
    static final String RESPONSE_SCHEMA_VERSION = "exercise-correction-inference-v1";
    static final int MAX_CONNECT_TIMEOUT_MS = 10_000;
    static final int MAX_READ_TIMEOUT_MS = 30_000;
    private static final Set<String> LANDMARK_NAMES = Set.of(
            "nose", "left_eye_inner", "left_eye", "left_eye_outer", "right_eye_inner",
            "right_eye", "right_eye_outer", "left_ear", "right_ear", "mouth_left",
            "mouth_right", "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
            "left_wrist", "right_wrist", "left_pinky", "right_pinky", "left_index",
            "right_index", "left_thumb", "right_thumb", "left_hip", "right_hip",
            "left_knee", "right_knee", "left_ankle", "right_ankle", "left_heel",
            "right_heel", "left_foot_index", "right_foot_index");
    private static final Set<String> LANDMARK_FIELDS = Set.of("x", "y", "z", "visibility");

    private final boolean enabled;
    private final RestClient restClient;

    @Autowired
    public ExerciseCorrectionHttpAdapter(
            @Value("${carebridge.exercise-correction.enabled:false}") boolean enabled,
            @Value("${carebridge.exercise-correction.base-url:http://localhost:8002}") String baseUrl,
            @Value("${carebridge.exercise-correction.connect-timeout-ms:500}") int connectTimeoutMs,
            @Value("${carebridge.exercise-correction.read-timeout-ms:1200}") int readTimeoutMs) {
        String normalizedBaseUrl = baseUrl == null ? "" : stripTrailingSlash(baseUrl);
        if (normalizedBaseUrl.isBlank()) {
            throw new IllegalArgumentException("Exercise-Correction base URL must not be blank");
        }
        try {
            URI parsed = URI.create(normalizedBaseUrl);
            if (parsed.getScheme() == null || parsed.getHost() == null) {
                throw new IllegalArgumentException("Exercise-Correction base URL must be an absolute URI");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Exercise-Correction base URL must be an absolute URI", exception);
        }
        if (connectTimeoutMs <= 0
                || readTimeoutMs <= 0
                || connectTimeoutMs > MAX_CONNECT_TIMEOUT_MS
                || readTimeoutMs > MAX_READ_TIMEOUT_MS) {
            throw new IllegalArgumentException(
                    "Exercise-Correction timeouts must be within the configured bounds");
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.enabled = enabled;
        this.restClient = RestClient.builder()
                .baseUrl(normalizedBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    ExerciseCorrectionHttpAdapter(boolean enabled, RestClient restClient) {
        this.enabled = enabled;
        this.restClient = restClient;
    }

    @Override
    public InferenceResult infer(InferenceRequest request) {
        if (!enabled) {
            throw unavailable("PROVIDER_DISABLED");
        }
        validateRequest(request);

        SidecarRequest body = new SidecarRequest(
                LANDMARK_SCHEMA_VERSION,
                request.modelVersion(),
                request.exerciseKey(),
                request.sequenceNumber(),
                null,
                request.landmarks());

        try {
            SidecarResponse response = restClient.post()
                    .uri("/v1/inference/landmarks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(SidecarResponse.class);
            return mapResponse(response, request);
        } catch (HttpClientErrorException exception) {
            throw unavailable("SIDECAR_REJECTED_REQUEST");
        } catch (HttpServerErrorException exception) {
            throw unavailable("SIDECAR_UNAVAILABLE");
        } catch (ResourceAccessException exception) {
            throw unavailable("SIDECAR_TIMEOUT_OR_UNREACHABLE");
        } catch (RestClientException exception) {
            throw unavailable("SIDECAR_INVALID_RESPONSE");
        }
    }

    private void validateRequest(InferenceRequest request) {
        if (request == null
                || request.modelVersion() == null
                || request.modelVersion().isBlank()
                || request.exerciseKey() == null
                || request.exerciseKey().isBlank()
                || request.sequenceNumber() < 0
                || request.landmarks() == null
                || request.landmarks().isEmpty()
                || !validLandmarks(request.landmarks())) {
            throw unavailable("INFERENCE_REQUEST_INVALID");
        }
    }

    private boolean validLandmarks(Map<String, Object> landmarks) {
        for (Map.Entry<String, Object> entry : landmarks.entrySet()) {
            if (!LANDMARK_NAMES.contains(entry.getKey()) || !(entry.getValue() instanceof Map<?, ?> point)) {
                return false;
            }
            if (!point.keySet().stream().allMatch(String.class::isInstance)
                    || !point.keySet().stream().map(Object::toString).allMatch(LANDMARK_FIELDS::contains)
                    || !LANDMARK_FIELDS.stream().allMatch(point::containsKey)) {
                return false;
            }
            for (String field : LANDMARK_FIELDS) {
                Object value = point.get(field);
                if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    private InferenceResult mapResponse(SidecarResponse response, InferenceRequest request) {
        if (response == null
                || !RESPONSE_SCHEMA_VERSION.equals(response.schemaVersion())
                || !request.modelVersion().equals(response.modelVersion())
                || !request.exerciseKey().equals(response.exerciseKey())
                || request.sequenceNumber() != response.sequenceNumber()
                || response.predictedClass() == null
                || response.predictedClass().isBlank()
                || response.confidence() == null
                || response.correct() == null
                || response.score() == null
                || response.feedback() == null
                || !isProbability(response.confidence())
                || !isScore(response.score())) {
            throw unavailable("SIDECAR_INVALID_RESPONSE");
        }

        if (response.feedback().stream().anyMatch(java.util.Objects::isNull)) {
            throw unavailable("SIDECAR_INVALID_RESPONSE");
        }
        List<InferenceFeedback> feedback = response.feedback().stream()
                .map(item -> new InferenceFeedback(item.code(), item.severity(), item.message()))
                .toList();
        if (feedback.stream().anyMatch(this::invalidFeedback)) {
            throw unavailable("SIDECAR_INVALID_RESPONSE");
        }

        return new InferenceResult(
                response.modelVersion(),
                response.exerciseKey(),
                response.sequenceNumber(),
                response.predictedClass(),
                response.confidence(),
                response.correct(),
                response.score(),
                feedback);
    }

    private boolean invalidFeedback(InferenceFeedback feedback) {
        return feedback.code() == null
                || feedback.code().isBlank()
                || feedback.severity() == null
                || !SetHolder.SEVERITIES.contains(feedback.severity())
                || feedback.message() == null
                || feedback.message().isBlank();
    }

    private boolean isProbability(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) >= 0 && value.compareTo(BigDecimal.ONE) <= 0;
    }

    private boolean isScore(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) >= 0
                && value.compareTo(new BigDecimal("100")) <= 0;
    }

    private PostureInferenceUnavailableException unavailable(String reasonCode) {
        return new PostureInferenceUnavailableException(reasonCode);
    }

    private static String stripTrailingSlash(String value) {
        String stripped = value.strip();
        while (stripped.endsWith("/")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        return stripped;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record SidecarRequest(
            String schemaVersion,
            String modelVersion,
            String exerciseKey,
            long sequenceNumber,
            String inferenceStreamId,
            Map<String, Object> landmarks) {

        /** Prevent Spring HTTP converter DEBUG logs from rendering landmark coordinates. */
        @Override
        public String toString() {
            return "SidecarRequest[schemaVersion=" + schemaVersion
                    + ", modelVersion=" + modelVersion
                    + ", exerciseKey=" + exerciseKey
                    + ", sequenceNumber=" + sequenceNumber
                    + ", inferenceStreamId=" + (inferenceStreamId == null ? "absent" : "present")
                    + ", landmarkCount=" + (landmarks == null ? 0 : landmarks.size())
                    + "]";
        }
    }

    private record SidecarResponse(
            String schemaVersion,
            String modelVersion,
            String exerciseKey,
            long sequenceNumber,
            String predictedClass,
            BigDecimal confidence,
            Boolean correct,
            BigDecimal score,
            List<SidecarFeedback> feedback) {
    }

    private record SidecarFeedback(String code, String severity, String message) {
    }

    private static final class SetHolder {
        private static final java.util.Set<String> SEVERITIES =
                java.util.Set.of("INFO", "WARNING", "CRITICAL");

        private SetHolder() {
        }
    }
}
