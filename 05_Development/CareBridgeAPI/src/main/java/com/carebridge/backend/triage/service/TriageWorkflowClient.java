package com.carebridge.backend.triage.service;

import java.util.Map;

public interface TriageWorkflowClient {
    WorkflowResult executeTurn(Map<String, Object> request);

    record WorkflowResult(Map<String, Object> state, String readiness,
                          String rulesetVersion, String rulesetHash) {
    }
}
