package com.carebridge.backend.exercise.inference;

import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceFeedback;
import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceRequest;
import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** HTTP adapter for the private, persistent Exercise-Correction sidecar. */
@Component
public class ExerciseCorrectionHttpAdapter implements PostureInferencePort {

    private static final Logger LOG = LoggerFactory.getLogger(ExerciseCorrectionHttpAdapter.class);
    private static final ObjectMapper ERROR_BODY_MAPPER = new ObjectMapper();
    private static final Pattern SIDECAR_ERROR_CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");
    private static final Pattern STAGE_TOKEN = Pattern.compile("^[A-Za-z0-9_]{1,32}$");

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

        // Pin HTTP/1.1. The JDK client defaults to HTTP_2, which over cleartext
        // sends an `Upgrade: h2c` request; the sidecar runs uvicorn/h11 (HTTP/1.1
        // only), and h11 pauses the connection at the Upgrade header so the ASGI
        // app is invoked with an empty body — every inference then fails 422.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
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
            logSidecarStatus("rejected the inference request", exception);
            throw unavailable("SIDECAR_REJECTED_REQUEST");
        } catch (HttpServerErrorException exception) {
            logSidecarStatus("reported a server error", exception);
            throw unavailable("SIDECAR_UNAVAILABLE");
        } catch (ResourceAccessException exception) {
            LOG.warn(
                    "Exercise-Correction sidecar is unreachable or timed out: {}",
                    exception.getClass().getSimpleName());
            throw unavailable("SIDECAR_TIMEOUT_OR_UNREACHABLE");
        } catch (RestClientException exception) {
            LOG.warn(
                    "Exercise-Correction sidecar returned an unreadable response: {}",
                    exception.getClass().getSimpleName());
            throw unavailable("SIDECAR_INVALID_RESPONSE");
        }
    }

    /**
     * Logs the sidecar HTTP status plus its stable error code so transport-level
     * faults (a wrong protocol version, a stray proxy, a rejected contract) are
     * diagnosable without reading the sidecar's own logs. Only the allowlisted
     * {@code code} field is logged — never the response body, which may carry
     * text this backend does not control.
     */
    private void logSidecarStatus(String what, HttpStatusCodeException exception) {
        LOG.warn(
                "Exercise-Correction sidecar {}: status={} sidecarCode={}",
                what,
                exception.getStatusCode().value(),
                sidecarErrorCode(exception.getResponseBodyAsString()));
    }

    static String sidecarErrorCode(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "ABSENT";
        }
        try {
            JsonNode code = ERROR_BODY_MAPPER.readTree(responseBody).get("code");
            if (code == null || !code.isTextual()) {
                return "UNRECOGNIZED";
            }
            return SIDECAR_ERROR_CODE.matcher(code.textValue()).matches()
                    ? code.textValue()
                    : "UNRECOGNIZED";
        } catch (JsonProcessingException exception) {
            return "UNPARSEABLE";
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
                || !isScore(response.score())
                || !validStage(response.stage())) {
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
                feedback,
                response.stage());
    }

    private boolean invalidFeedback(InferenceFeedback feedback) {
        return feedback.code() == null
                || feedback.code().isBlank()
                || feedback.severity() == null
                || !SetHolder.SEVERITIES.contains(feedback.severity())
                || feedback.message() == null
                || feedback.message().isBlank();
    }

    /** Absent is valid; present must be a short opaque token, since it is persisted. */
    private boolean validStage(String stage) {
        return stage == null || STAGE_TOKEN.matcher(stage).matches();
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

    /** {@code stage} is optional and additive; an older sidecar simply omits it. */
    private record SidecarResponse(
            String schemaVersion,
            String modelVersion,
            String exerciseKey,
            long sequenceNumber,
            String predictedClass,
            BigDecimal confidence,
            Boolean correct,
            BigDecimal score,
            List<SidecarFeedback> feedback,
            String stage) {
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
