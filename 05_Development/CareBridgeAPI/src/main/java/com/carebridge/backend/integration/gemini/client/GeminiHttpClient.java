package com.carebridge.backend.integration.gemini.client;

import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import java.net.http.HttpClient;
import java.time.Duration;
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
 * Real Gemini generateContent client (RestClient over JDK HttpClient, same shape as the
 * CompreFace adapters). Contract preserved for the three existing consumers (RAG, triage
 * adapter, extraction adapter): any failure — including the feature being disabled — surfaces
 * as {@link GeminiUnavailableException}, which every caller already catches and falls back on.
 * Error messages are sanitized: they never contain the API key or the prompt.
 */
@Component
@Slf4j
public class GeminiHttpClient implements GeminiClient {

    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public GeminiHttpClient(
            @Value("${carebridge.gemini.enabled:false}") boolean enabled,
            @Value("${carebridge.gemini.api-key:}") String apiKey,
            @Value("${carebridge.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${carebridge.gemini.model:gemini-flash-latest}") String model,
            @Value("${carebridge.gemini.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${carebridge.gemini.timeout-ms:5000}") int readTimeoutMs) {
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

    @Override
    public String generate(String prompt) throws GeminiUnavailableException {
        if (!enabled) {
            throw new GeminiUnavailableException("Gemini is disabled (GEMINI_ENABLED=false)");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiUnavailableException("Gemini API key is not configured");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String text = extractFirstCandidateText(response);
            if (text == null || text.isBlank()) {
                throw new GeminiUnavailableException("Gemini returned an empty response");
            }
            return text;
        } catch (HttpClientErrorException ex) {
            // 4xx — configuration/model problem or rate limit; message stays key-free
            int status = ex.getStatusCode().value();
            if (status == 429) {
                throw new GeminiUnavailableException("Gemini rate limited (HTTP 429)");
            }
            throw new GeminiUnavailableException(
                    "Gemini rejected the request (HTTP " + status + ") — check GEMINI_MODEL/GEMINI_API_KEY configuration");
        } catch (HttpServerErrorException ex) {
            throw new GeminiUnavailableException("Gemini server error (HTTP " + ex.getStatusCode().value() + ")");
        } catch (ResourceAccessException ex) {
            throw new GeminiUnavailableException("Gemini connection failure or timeout");
        }
    }

    @SuppressWarnings("unchecked")
    static String extractFirstCandidateText(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object candidatesObj = response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }
        Object first = candidates.get(0);
        if (!(first instanceof Map<?, ?> candidate)) {
            return null;
        }
        Object contentObj = ((Map<String, Object>) candidate).get("content");
        if (!(contentObj instanceof Map<?, ?> content)) {
            return null;
        }
        Object partsObj = ((Map<String, Object>) content).get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return null;
        }
        Object firstPart = parts.get(0);
        if (!(firstPart instanceof Map<?, ?> part)) {
            return null;
        }
        Object text = ((Map<String, Object>) part).get("text");
        return text instanceof String s ? s : null;
    }
}
