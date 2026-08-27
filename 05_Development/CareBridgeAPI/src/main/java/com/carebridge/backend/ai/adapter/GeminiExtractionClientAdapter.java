package com.carebridge.backend.ai.adapter;

import com.carebridge.backend.ai.service.GeminiExtractionClient;
import com.carebridge.backend.integration.gemini.client.GeminiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class GeminiExtractionClientAdapter implements GeminiExtractionClient {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Override
    public ExtractionResult extractStructuredData(String constrainedPrompt) {
        try {
            String response = geminiClient.generate(constrainedPrompt);
            JsonNode root = objectMapper.readTree(response);
            return new ExtractionResult(
                    root.path("symptomList").toString(),
                    root.path("durationDays").asInt(0),
                    root.path("intensity").asText("LOW"),
                    root.path("emergencyFlag").asBoolean(false)
            );
        } catch (Exception exception) {
            log.warn("Gemini extraction fallback reason=GEMINI_UNAVAILABLE exceptionType={}",
                    exception.getClass().getSimpleName());
            return new ExtractionResult("[]", 0, "LOW", false);
        }
    }
}
