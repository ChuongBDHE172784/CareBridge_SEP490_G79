package com.carebridge.backend.expertverification.adapter;

import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Replaceable boundary around Dockerized CompreFace. It never makes an approval decision. */
@Component
public class CompreFaceVerificationAdapter implements FaceVerificationAdapter {

    private final boolean enabled;
    private final String apiKey;
    private final BigDecimal threshold;
    private final RestClient restClient;

    public CompreFaceVerificationAdapter(
            @Value("${carebridge.compreface.enabled:false}") boolean enabled,
            @Value("${carebridge.compreface.base-url:http://localhost:8000}") String baseUrl,
            @Value("${carebridge.compreface.api-key:}") String apiKey,
            @Value("${carebridge.compreface.similarity-threshold:0.75}") BigDecimal threshold,
            @Value("${carebridge.compreface.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${carebridge.compreface.read-timeout-ms:8000}") int readTimeoutMs) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.threshold = threshold;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public FaceVerificationResult verify(
            byte[] selfie, String selfieMimeType,
            byte[] identityFront, String identityFrontMimeType) {
        if (!enabled) {
            return new FaceVerificationResult(
                    FaceVerificationStatus.DISABLED, null, threshold, "PENDING_LINUX_VERIFICATION");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return new FaceVerificationResult(
                    FaceVerificationStatus.RETRYABLE_ERROR, null, threshold, "COMPREFACE_NOT_CONFIGURED");
        }

        try {
            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("source_image", namedResource(selfie, "selfie.jpg"));
            parts.add("target_image", namedResource(identityFront, "identity-front.jpg"));
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/v1/verification/verify")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(Map.class);
            Optional<BigDecimal> similarity = findHighestSimilarity(response);
            if (similarity.isEmpty()) {
                return new FaceVerificationResult(
                        FaceVerificationStatus.NO_FACE, null, threshold, "NO_FACE_MATCH_RESULT");
            }
            BigDecimal score = similarity.get();
            FaceVerificationStatus status = score.compareTo(threshold) >= 0
                    ? FaceVerificationStatus.MATCHED : FaceVerificationStatus.NOT_MATCHED;
            return new FaceVerificationResult(status, score, threshold, null);
        } catch (Exception ex) {
            String message = String.valueOf(ex.getMessage()).toLowerCase();
            FaceVerificationStatus status = message.contains("multiple")
                    ? FaceVerificationStatus.MULTIPLE_FACES
                    : message.contains("no face") ? FaceVerificationStatus.NO_FACE
                    : FaceVerificationStatus.RETRYABLE_ERROR;
            return new FaceVerificationResult(status, null, threshold,
                    status == FaceVerificationStatus.RETRYABLE_ERROR
                            ? "COMPREFACE_UNAVAILABLE" : status.name());
        }
    }

    private static ByteArrayResource namedResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private static Optional<BigDecimal> findHighestSimilarity(Object value) {
        if (value instanceof Map<?, ?> map) {
            Optional<BigDecimal> direct = map.entrySet().stream()
                    .filter(entry -> "similarity".equals(String.valueOf(entry.getKey())))
                    .map(Map.Entry::getValue)
                    .filter(Number.class::isInstance)
                    .map(Number.class::cast)
                    .map(number -> BigDecimal.valueOf(number.doubleValue()))
                    .max(BigDecimal::compareTo);
            Optional<BigDecimal> nested = map.values().stream()
                    .map(CompreFaceVerificationAdapter::findHighestSimilarity)
                    .flatMap(Optional::stream)
                    .max(BigDecimal::compareTo);
            return direct.isPresent() && (nested.isEmpty() || direct.get().compareTo(nested.get()) >= 0)
                    ? direct : nested;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(CompreFaceVerificationAdapter::findHighestSimilarity)
                    .flatMap(Optional::stream)
                    .max(BigDecimal::compareTo);
        }
        return Optional.empty();
    }
}
