package com.carebridge.backend.expertverification.adapter;

import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class CompreFaceVerificationAdapter implements FaceVerificationAdapter {

    private final boolean enabled;
    private final String apiKey;
    private final BigDecimal threshold;
    private final RestClient restClient;

    public BigDecimal getThreshold() {
        return threshold;
    }

    public CompreFaceVerificationAdapter(
            @Value("${carebridge.compreface.enabled:false}") boolean enabled,
            @Value("${carebridge.compreface.base-url:http://localhost:8000}") String baseUrl,
            @Value("${carebridge.compreface.verification-api-key:}") String apiKey,
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
            byte[] selfieFaceBytes, String selfieMimeType,
            byte[] idCardFaceBytes, String idCardMimeType) {
        if (!enabled) {
            return new FaceVerificationResult(
                    FaceVerificationStatus.DISABLED, null, threshold, "CompreFaceIsDisabled");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return new FaceVerificationResult(
                    FaceVerificationStatus.RETRYABLE_ERROR, null, threshold, "VERIFICATION_API_KEY_NOT_CONFIGURED");
        }

        try {
            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("source_image", namedResource(selfieFaceBytes, "selfie.jpg"));
            parts.add("target_image", namedResource(idCardFaceBytes, "id-card.jpg"));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/verification/verify")
                            .queryParam("limit", 1)
                            .queryParam("prediction_count", 1)
                            .queryParam("det_prob_threshold", 0.5)
                            .build())
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(Map.class);

            return parseVerificationResponse(response);

        } catch (HttpClientErrorException ex) {
            return handleHttpClientError(ex, "VERIFICATION");
        } catch (HttpServerErrorException ex) {
            return new FaceVerificationResult(
                    FaceVerificationStatus.RETRYABLE_ERROR, null, threshold, "VERIFICATION_SERVER_ERROR");
        } catch (ResourceAccessException ex) {
            return new FaceVerificationResult(
                    FaceVerificationStatus.RETRYABLE_ERROR, null, threshold, "VERIFICATION_CONNECTION_FAILED");
        } catch (Exception ex) {
            return new FaceVerificationResult(
                    FaceVerificationStatus.RETRYABLE_ERROR, null, threshold, "VERIFICATION_UNKNOWN_ERROR");
        }
    }

    private FaceVerificationResult parseVerificationResponse(Map<String, Object> response) {
        try {
            // CompreFace 1.2.0 Face Verification response format:
            // {
            //   "result": [
            //     {
            //       "source_image_face": {"box": {...}, "probability": ...},
            //       "face_matches": [
            //         {"face_id": "...", "similarity": 0.95, "threshold": 0.75},
            //         ...
            //       ]
            //     }
            //   ],
            //   "success": true
            // }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) response.get("result");
            if (result == null || result.isEmpty()) {
                return new FaceVerificationResult(
                        FaceVerificationStatus.NOT_MATCHED, null, threshold, "NO_VERIFICATION_RESULT");
            }

            Map<String, Object> firstResult = result.get(0);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> faceMatches = (List<Map<String, Object>>) firstResult.get("face_matches");
            if (faceMatches == null || faceMatches.isEmpty()) {
                return new FaceVerificationResult(
                        FaceVerificationStatus.NOT_MATCHED, null, threshold, "NO_FACE_MATCHES");
            }

            // Get the best match (highest similarity) - pipeline crops exactly one face each,
            // so we expect exactly one match
            Map<String, Object> bestMatch = faceMatches.stream()
                    .max((a, b) -> {
                        double simA = parseSimilarity(a);
                        double simB = parseSimilarity(b);
                        return Double.compare(simA, simB);
                    })
                    .orElse(null);

            if (bestMatch == null) {
                return new FaceVerificationResult(
                        FaceVerificationStatus.NOT_MATCHED, null, threshold, "NO_VALID_MATCH");
            }

            double similarity = parseSimilarity(bestMatch);
            BigDecimal similarityBd = BigDecimal.valueOf(similarity);
            boolean matched = similarityBd.compareTo(threshold) >= 0;

            // Prefer provider's threshold if present, fallback to our configured threshold
            BigDecimal effectiveThreshold = extractThreshold(bestMatch, threshold);

            return new FaceVerificationResult(
                    matched ? FaceVerificationStatus.MATCHED : FaceVerificationStatus.NOT_MATCHED,
                    similarityBd, effectiveThreshold, null);

        } catch (Exception ex) {
            return new FaceVerificationResult(
                    FaceVerificationStatus.NOT_MATCHED, null, threshold, "VERIFICATION_PARSE_ERROR: " + ex.getMessage());
        }
    }

    private double parseSimilarity(Map<String, Object> match) {
        Object simObj = match.get("similarity");
        if (simObj == null) {
            throw new IllegalStateException("Missing 'similarity' in face_matches entry");
        }
        return ((Number) simObj).doubleValue();
    }

    private BigDecimal extractThreshold(Map<String, Object> match, BigDecimal defaultThreshold) {
        Object threshObj = match.get("threshold");
        if (threshObj == null) {
            return defaultThreshold;
        }
        return new BigDecimal(threshObj.toString());
    }

    private static ByteArrayResource namedResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private FaceVerificationResult handleHttpClientError(HttpClientErrorException ex, String prefix) {
        int status = ex.getStatusCode().value();
        return switch (status) {
            case 400 -> new FaceVerificationResult(
                    FaceVerificationStatus.RETRYABLE_ERROR, null, threshold, prefix + "_BAD_REQUEST");
            case 401, 403 -> new FaceVerificationResult(
                    FaceVerificationStatus.RETRYABLE_ERROR, null, threshold, prefix + "_UNAUTHORIZED");
            case 429 -> new FaceVerificationResult(
                    FaceVerificationStatus.RETRYABLE_ERROR, null, threshold, prefix + "_RATE_LIMITED");
            default -> new FaceVerificationResult(
                    FaceVerificationStatus.RETRYABLE_ERROR, null, threshold, prefix + "_CLIENT_ERROR_" + status);
        };
    }
}