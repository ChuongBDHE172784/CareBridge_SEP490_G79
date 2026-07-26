package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.dto.HealthMemoryContextItem;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;

import java.util.List;
import java.util.Map;

/**
 * @version 2.0 — additive default overload for server-populated health context
 * (CB-TRIAGE-THMC-IMP-001 §8.1); existing implementations stay source-compatible.
 */
public interface ChildTriageAiClient {
    String triageChild(RunIntakeRequest request);
    String startIntake(Map<String, Object> request);
    String continueIntake(Map<String, Object> request);

    /**
     * NEW (BR-THMC-004/006): one-shot triage with server-populated health context.
     * Default delegates to triageChild(request) so existing implementations
     * (e.g. GeminiTriageClientAdapter) stay source-compatible.
     */
    default String triageChild(RunIntakeRequest request, List<HealthMemoryContextItem> healthContext) {
        return triageChild(request);
    }
}
