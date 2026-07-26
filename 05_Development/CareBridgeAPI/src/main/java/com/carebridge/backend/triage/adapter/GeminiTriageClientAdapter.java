package com.carebridge.backend.triage.adapter;

import com.carebridge.backend.integration.gemini.client.GeminiClient;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class GeminiTriageClientAdapter implements GeminiTriageClient {

    private final GeminiClient geminiClient;

    @Override
    public AiTriageResult analyzeSymptoms(String constrainedPrompt) {
        try {
            String response = geminiClient.generate(constrainedPrompt);
            RiskLevel level = RiskLevel.GREEN;
            if (response.contains("RED")) level = RiskLevel.RED;
            else if (response.contains("YELLOW")) level = RiskLevel.YELLOW;
            return new AiTriageResult(level, "AI-assisted triage result. Seek professional advice.");
        } catch (Exception exception) {
            log.warn("Gemini triage fallback reason=GEMINI_UNAVAILABLE exceptionType={}",
                    exception.getClass().getSimpleName());
            return new AiTriageResult(RiskLevel.GREEN, "AI triage unavailable. Please consult a healthcare professional.");
        }
    }
}
