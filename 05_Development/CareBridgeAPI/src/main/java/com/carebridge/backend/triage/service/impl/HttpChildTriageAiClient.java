package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HttpChildTriageAiClient implements ChildTriageAiClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Value("${ai.triage-service.url}")
    private String aiTriageServiceUrl;

    @Override
    public String triageChild(RunIntakeRequest request) {
        try {
            return postJson("/triage/child", toAiPayload(request));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid triage request payload", e);
        }
    }

    @Override
    public String startIntake(Map<String, Object> request) {
        try {
            return postJson("/triage/intake/start", request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid triage intake start payload", e);
        }
    }

    @Override
    public String continueIntake(Map<String, Object> request) {
        try {
            return postJson("/triage/intake/continue", request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid triage intake continue payload", e);
        }
    }

    private Map<String, Object> toAiPayload(RunIntakeRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("childAgeMonths", request.getChildAgeMonths());
        payload.put("symptomList", request.getSymptomList() == null ? List.of() : request.getSymptomList());
        payload.put("duration", request.getDuration());
        payload.put("temperatureC", request.getTemperatureC());
        payload.put("feedingStatus", request.getFeedingStatus());
        payload.put("breathingStatus", request.getBreathingStatus());
        payload.put("consciousnessStatus", request.getConsciousnessStatus());
        payload.put("vomiting", request.getVomiting());
        payload.put("diarrhea", request.getDiarrhea());
        payload.put("rash", request.getRash());
        payload.put("seizure", request.getSeizure());
        payload.put("dehydrationSigns", request.getDehydrationSigns() == null ? List.of() : request.getDehydrationSigns());
        payload.put("parentFreeText", request.getParentFreeText() != null ? request.getParentFreeText() : request.getSymptoms());
        return payload;
    }

    private String postJson(String path, Object payload) throws JsonProcessingException {
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(aiTriageServiceUrl + path))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI triage service returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("AI triage service unavailable", e);
        }
    }
}
