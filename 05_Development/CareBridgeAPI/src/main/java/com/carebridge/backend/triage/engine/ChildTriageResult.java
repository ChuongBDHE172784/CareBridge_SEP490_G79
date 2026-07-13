package com.carebridge.backend.triage.engine;

import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value
@Builder
public class ChildTriageResult {
    String status;
    String riskLevel;
    String riskColor;
    String summary;
    String possibleConcern;
    String recommendedAction;
    boolean emergencyActionRequired;
    List<String> redFlags;
    List<String> matchedRules;
    List<String> normalizedSymptoms;
    List<TriageCitation> citations;
    String disclaimer;
    List<String> questions;
    String warning;
}
