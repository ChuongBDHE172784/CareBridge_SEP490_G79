package com.carebridge.backend.integration.gemini.client;

import com.carebridge.backend.integration.gemini.exception.GeminiConfigurationException;
import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Structured-output Gemini client dedicated to content moderation scans.
 *
 * Separate from {@link GeminiHttpClient} because the moderation worker needs a stricter
 * contract than the legacy generate() path: JSON-schema-constrained output, a longer read
 * timeout, and an error taxonomy that distinguishes non-retryable configuration failures
 * ({@link GeminiConfigurationException}: bad key/model/request, provider safety block)
 * from transient ones ({@link GeminiUnavailableException}: timeout, 429, 5xx).
 *
 * Never logs or embeds the API key or the scanned content in any exception message.
 */
@Component
@Slf4j
public class GeminiModerationClient {

    public enum ConfigState { DISABLED, NOT_CONFIGURED, READY }

    /** Raw structured-call outcome; JSON validation happens in the aimoderation layer. */
    public record ModerationCallResult(String rawJson, long latencyMs, Integer promptTokens, Integer outputTokens) {
    }

    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public GeminiModerationClient(
            @Value("${carebridge.gemini.enabled:false}") boolean enabled,
            @Value("${carebridge.gemini.api-key:}") String apiKey,
            @Value("${carebridge.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${carebridge.gemini.model:gemini-1.5-flash}") String model,
            @Value("${carebridge.gemini.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${carebridge.gemini.moderation.timeout-ms:15000}") int readTimeoutMs) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;

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

    public ConfigState configState() {
        if (!enabled) {
            return ConfigState.DISABLED;
        }
        if (apiKey == null || apiKey.isBlank()) {
            return ConfigState.NOT_CONFIGURED;
        }
        return ConfigState.READY;
    }

    public String model() {
        return model;
    }

    /**
     * Executes a schema-constrained classification call. The system instruction is
     * server-owned; userContent is untrusted data and is only ever sent as model input.
     */
    public ModerationCallResult classify(String systemInstruction, String userContent, Map<String, Object> responseSchema) {
        ConfigState state = configState();
        if (state == ConfigState.DISABLED) {
            throw new GeminiConfigurationException("GEMINI_DISABLED", "Gemini is disabled (GEMINI_ENABLED=false)");
        }
        if (state == ConfigState.NOT_CONFIGURED) {
            throw new GeminiConfigurationException("GEMINI_KEY_MISSING", "Gemini API key is not configured");
        }

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", responseSchema);

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userContent)))),
                "generationConfig", generationConfig);

        long startedAt = System.nanoTime();
        Map<String, Object> response;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            response = raw;
        } catch (HttpClientErrorException ex) {
            int status = ex.getStatusCode().value();
            if (status == 429) {
                throw new GeminiUnavailableException("Gemini rate limited (HTTP 429)");
            }
            if (status == 401 || status == 403) {
                throw new GeminiConfigurationException("GEMINI_AUTH_FAILED",
                        "Gemini authentication failed (HTTP " + status + ") — check GEMINI_API_KEY");
            }
            if (status == 404) {
                throw new GeminiConfigurationException("GEMINI_MODEL_INVALID",
                        "Gemini model '" + model + "' was not accepted by the provider (HTTP 404) — check GEMINI_MODEL");
            }
            throw new GeminiConfigurationException("GEMINI_BAD_REQUEST",
                    "Gemini rejected the moderation request (HTTP " + status + ")");
        } catch (HttpServerErrorException ex) {
            throw new GeminiUnavailableException("Gemini server error (HTTP " + ex.getStatusCode().value() + ")");
        } catch (ResourceAccessException ex) {
            throw new GeminiUnavailableException("Gemini connection failure or timeout");
        }
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;

        String blockReason = extractBlockReason(response);
        if (blockReason != null) {
            // Provider refused to evaluate this input. Terminal for this content version,
            // surfaced as FAILED — never interpreted as SAFE.
            throw new GeminiConfigurationException("GEMINI_SAFETY_BLOCKED",
                    "Gemini blocked the moderation request (blockReason=" + blockReason + ")");
        }

        String text = GeminiHttpClient.extractFirstCandidateText(response);
        if (text == null || text.isBlank()) {
            throw new GeminiUnavailableException("Gemini returned an empty moderation response");
        }
        return new ModerationCallResult(text, latencyMs,
                extractUsage(response, "promptTokenCount"), extractUsage(response, "candidatesTokenCount"));
    }

    @SuppressWarnings("unchecked")
    private static String extractBlockReason(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object feedback = response.get("promptFeedback");
        if (!(feedback instanceof Map<?, ?> map)) {
            return null;
        }
        Object reason = ((Map<String, Object>) map).get("blockReason");
        return reason instanceof String s && !s.isBlank() ? s : null;
    }

    @SuppressWarnings("unchecked")
    private static Integer extractUsage(Map<String, Object> response, String field) {
        if (response == null) {
            return null;
        }
        Object usage = response.get("usageMetadata");
        if (!(usage instanceof Map<?, ?> map)) {
            return null;
        }
        Object value = ((Map<String, Object>) map).get(field);
        return value instanceof Number n ? n.intValue() : null;
    }
}
