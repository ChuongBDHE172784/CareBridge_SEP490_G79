package com.carebridge.backend.triage.service;

import java.util.Map;

public interface TriageV2WorkflowClient {
    WorkflowResult executeTurn(Map<String, Object> request);

    record WorkflowResult(Map<String, Object> state, String readiness,
                          String rulesetVersion, String rulesetHash) {
    }
}
