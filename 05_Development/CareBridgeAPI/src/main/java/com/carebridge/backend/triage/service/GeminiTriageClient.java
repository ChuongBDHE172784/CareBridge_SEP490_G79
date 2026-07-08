package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.RiskLevel;

public interface GeminiTriageClient {

    record AiTriageResult(RiskLevel riskLevel, String disclaimer) {}

    AiTriageResult analyzeSymptoms(String constrainedPrompt);
}
