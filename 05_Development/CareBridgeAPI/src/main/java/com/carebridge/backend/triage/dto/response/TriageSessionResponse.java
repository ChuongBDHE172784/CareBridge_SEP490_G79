package com.carebridge.backend.triage.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TriageSessionResponse(
        UUID sessionId,
        int stateVersion,
        String target,
        String intent,
        String stage,
        String outcome,
        String action,
        boolean stop,
        List<String> questions,
        List<TriageQuestionResponse> questionDetails,
        String scope,
        List<String> pendingRisks,
        String completionReason,
        String rulesetVersion,
        String rulesetHash,
        String rationale,
        String evidenceStatus,
        List<Map<String, Object>> citations,
        String disclaimer,
        Map<String, Object> readiness) {
}
