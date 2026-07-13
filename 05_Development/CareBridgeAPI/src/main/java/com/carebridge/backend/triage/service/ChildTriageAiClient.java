package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;

import java.util.Map;

public interface ChildTriageAiClient {
    String triageChild(RunIntakeRequest request);
    String startIntake(Map<String, Object> request);
    String continueIntake(Map<String, Object> request);
}
