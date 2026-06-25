package com.carebridge.backend.integration.gemini.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RagSafetyResult {

    private boolean redFlag;
    private String emergencyGuidance;

    public static RagSafetyResult safe() {
        return new RagSafetyResult(false, null);
    }

    public static RagSafetyResult redFlag(String guidance) {
        return new RagSafetyResult(true, guidance);
    }
}
