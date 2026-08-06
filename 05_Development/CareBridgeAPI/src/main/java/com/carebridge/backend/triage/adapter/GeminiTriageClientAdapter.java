package com.carebridge.backend.triage.adapter;

import com.carebridge.backend.integration.gemini.client.GeminiClient;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.exception.AiOutcomeUnavailableException;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@Deprecated
public class GeminiTriageClientAdapter implements GeminiTriageClient {

    private static final String DISCLAIMER = "AI-assisted triage result. Seek professional advice.";

    private final GeminiClient geminiClient;

    @Override
    public AiTriageResult analyzeSymptoms(String constrainedPrompt) {
        String response;
        try {
            response = geminiClient.generate(constrainedPrompt);
        } catch (Exception exception) {
            log.warn("Gemini triage fallback reason=GEMINI_UNAVAILABLE exceptionType={}",
                    exception.getClass().getSimpleName());
            // Previously returned GREEN here: an outage produced the most reassuring
            // possible answer. Fail closed instead — the caller must degrade to
            // NEEDS_MORE_INFO, never to a colour.
            throw new AiOutcomeUnavailableException("Gemini triage unavailable");
        }
        if (response != null && response.contains("RED")) {
            return new AiTriageResult(RiskLevel.RED, DISCLAIMER);
        }
        if (response != null && response.contains("YELLOW")) {
            return new AiTriageResult(RiskLevel.YELLOW, DISCLAIMER);
        }
        if (response != null && response.contains("GREEN")) {
            return new AiTriageResult(RiskLevel.GREEN, DISCLAIMER);
        }
        // An unparseable response is not evidence of low risk. GREEN used to be the
        // default branch here, which made any garbled output reassuring.
        log.warn("Gemini triage fallback reason=GEMINI_RESPONSE_UNPARSEABLE");
        throw new AiOutcomeUnavailableException("Gemini triage response was unparseable");
    }
}
