package com.carebridge.backend.integration.gemini.filter;

import com.carebridge.backend.integration.gemini.dto.RagSafetyResult;
import com.carebridge.backend.triage.policy.TriageRedFlagPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TriageRedFlagSafetyFilter implements RagSafetyFilter {

    private final TriageRedFlagPolicy triageRedFlagPolicy;

    @Override
    public RagSafetyResult check(String query) {
        if (triageRedFlagPolicy.isRedFlag(query)) {
            return RagSafetyResult.redFlag(triageRedFlagPolicy.getEmergencyGuidance());
        }
        return RagSafetyResult.safe();
    }
}
