package com.carebridge.backend.expertverification.adapter;

import com.carebridge.backend.expertverification.adapter.FaceDetectionResult;
import com.carebridge.backend.expertverification.adapter.FaceBoundingBox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class CompreFaceDetectionAdapter implements FaceDetectionAdapter {

    private final boolean enabled;
    private final String apiKey;
    private final Double detProbThreshold;
    private final int limit;
    private final RestClient restClient;

    public CompreFaceDetectionAdapter(
            @Value("${carebridge.compreface.enabled:false}") boolean enabled,
            @Value("${carebridge.compreface.base-url:http://localhost:8000}") String baseUrl,
            @Value("${carebridge.compreface.detection-api-key:}") String apiKey,
            @Value("${carebridge.compreface.det-prob-threshold:0.7}") Double detProbThreshold,
            @Value("${carebridge.compreface.detection-limit:2}") int limit,
            @Value("${carebridge.compreface.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${carebridge.compreface.read-timeout-ms:8000}") int readTimeoutMs) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.detProbThreshold = detProbThreshold;
        this.limit = limit;

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
    public FaceDetectionResult detect(byte[] imageBytes, String mimeType) {
        if (!enabled) {
            return FaceDetectionResult.providerError("DISABLED");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return FaceDetectionResult.providerError("DETECTION_API_KEY_NOT_CONFIGURED");
        }

        try {
            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("file", namedResource(imageBytes, "image" + getExtension(mimeType)));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/detection/detect")
                            .queryParam("limit", limit)
                            .queryParam("det_prob_threshold", detProbThreshold)
                            .build())
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(Map.class);

            return parseDetectionResponse(response);

        } catch (HttpClientErrorException ex) {
            return handleHttpClientError(ex, "DETECTION");
        } catch (HttpServerErrorException ex) {
            return FaceDetectionResult.providerError("DETECTION_SERVER_ERROR");
        } catch (ResourceAccessException ex) {
            return FaceDetectionResult.providerError("DETECTION_CONNECTION_FAILED");
        } catch (Exception ex) {
            return FaceDetectionResult.providerError("DETECTION_UNKNOWN_ERROR");
        }
    }

    private FaceDetectionResult parseDetectionResponse(Map<String, Object> response) {
        try {
            // CompreFace 1.2.0 detection response format:
            // {
            //   "result": [
            //     {
            //       "box": {"x_min": ..., "y_min": ..., "x_max": ..., "y_max": ..., "probability": ...},
            //       "landmarks": [...]
            //     },
            //     ...
            //   ],
            //   "success": true
            // }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) response.get("result");
            if (result == null || result.isEmpty()) {
                return FaceDetectionResult.noFace();
            }

            List<FaceBoundingBox> faces = result.stream()
                    .map(this::parseFaceBox)
                    .filter(fb -> fb.probability() >= detProbThreshold)
                    .toList();

            if (faces.isEmpty()) {
                return FaceDetectionResult.noFace();
            }

            if (faces.size() > 1) {
                return FaceDetectionResult.multipleFaces(faces.size());
            }

            return FaceDetectionResult.detected(faces);

        } catch (Exception ex) {
            return FaceDetectionResult.providerError("DETECTION_PARSE_ERROR");
        }
    }

    private FaceBoundingBox parseFaceBox(Map<String, Object> faceData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> box = (Map<String, Object>) faceData.get("box");
        if (box == null) {
            throw new IllegalStateException("Missing 'box' in face detection result");
        }

        // CompreFace 1.2.0 uses "probability" field, not "prob"
        double probability = parseProbability(box);

        double xMin = parseCoordinate(box, "x_min");
        double yMin = parseCoordinate(box, "y_min");
        double xMax = parseCoordinate(box, "x_max");
        double yMax = parseCoordinate(box, "y_max");

        // Determine if coordinates are normalized (0-1) or pixel values
        boolean normalized = xMax <= 1.0 && yMax <= 1.0 && xMin >= 0 && yMin >= 0;

        double width = xMax - xMin;
        double height = yMax - yMin;

        // Validate box dimensions
        if (width <= 0 || height <= 0 || width > 1.1 || height > 1.1) {
            throw new IllegalStateException("Invalid face bounding box dimensions");
        }

        return new FaceBoundingBox(
                xMin,
                yMin,
                width,
                height,
                probability,
                normalized
        );
    }

    private double parseProbability(Map<String, Object> box) {
        // Try "probability" first (CompreFace 1.2.0), fallback to "prob" for compatibility
        Object probObj = box.get("probability");
        if (probObj == null) {
            probObj = box.get("prob");
        }
        if (probObj == null) {
            throw new IllegalStateException("Missing probability/prob in face box");
        }
        return ((Number) probObj).doubleValue();
    }

    private double parseCoordinate(Map<String, Object> box, String key) {
        Object coordObj = box.get(key);
        if (coordObj == null) {
            throw new IllegalStateException("Missing coordinate: " + key);
        }
        return ((Number) coordObj).doubleValue();
    }

    private FaceDetectionResult handleHttpClientError(HttpClientErrorException ex, String prefix) {
        int status = ex.getStatusCode().value();
        return switch (status) {
            case 400 -> FaceDetectionResult.providerError(prefix + "_BAD_REQUEST");
            case 401, 403 -> FaceDetectionResult.providerError(prefix + "_UNAUTHORIZED");
            case 429 -> FaceDetectionResult.providerError(prefix + "_RATE_LIMITED");
            default -> FaceDetectionResult.providerError(prefix + "_CLIENT_ERROR_" + status);
        };
    }

    private static ByteArrayResource namedResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private String getExtension(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}