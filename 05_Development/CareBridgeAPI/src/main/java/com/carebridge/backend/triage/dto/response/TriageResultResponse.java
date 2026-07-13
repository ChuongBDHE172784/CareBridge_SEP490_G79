package com.carebridge.backend.triage.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TriageResultResponse {
    private UUID sessionId;
    private String triageStatus;
    private String riskLevel;
    private String riskColor;
    private String summary;
    private String possibleConcern;
    private String recommendedAction;
    private Boolean emergencyActionRequired;
    private List<String> redFlags;
    private List<String> matchedRules;
    private List<Map<String, Object>> citations;
    private Map<String, Object> evidence;
    private String disclaimer;
    private List<String> questions;
    private String warning;
    private List<String> normalizedSymptoms;
    private List<Map<String, Object>> normalizedSymptomDetails;
    private List<String> evidenceIds;
    private String recommendationCode;
    private Map<String, Object> explainabilityMetrics;
    private String graphVersion;
    private String ruleSetVersion;
    private String ontologyVersion;
    private String responseSchemaVersion;
    private Boolean fallbackUsed;
    private String status;
    private Instant createdAt;
    private Instant completedAt;
}
