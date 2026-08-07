package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.triage.service.TriageV2WorkflowClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class HttpTriageV2WorkflowClient implements TriageV2WorkflowClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String internalKey;
    private final Duration timeout;

    public HttpTriageV2WorkflowClient(
            ObjectMapper objectMapper,
            @Value("${ai.triage-service.url}") String baseUrl,
            @Value("${ai.triage-service.internal-api-key:}") String internalKey,
            @Value("${ai.triage-service.request-timeout-ms:8000}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.timeout = Duration.ofMillis(Math.max(250, timeoutMs));
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .version(HttpClient.Version.HTTP_1_1).build();
    }

    @Override
    public WorkflowResult executeTurn(Map<String, Object> request) {
        if (internalKey == null || internalKey.isBlank()) {
            throw new IllegalStateException("Triage V2 internal client is disabled");
        }
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/internal/triage/v2/turn"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("X-CareBridge-Internal-Key", internalKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Triage V2 workflow returned HTTP " + response.statusCode());
            }
            Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
            return new WorkflowResult(map(body.get("state")), text(body.get("readiness")),
                    text(body.get("rulesetVersion")), text(body.get("rulesetHash")));
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Triage V2 workflow unavailable", failure);
        } catch (Exception failure) {
            throw new IllegalStateException("Triage V2 workflow unavailable", failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?>)) throw new IllegalStateException("Invalid Triage V2 response");
        return (Map<String, Object>) value;
    }

    private static String text(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("Invalid Triage V2 response");
        }
        return text;
    }
}
