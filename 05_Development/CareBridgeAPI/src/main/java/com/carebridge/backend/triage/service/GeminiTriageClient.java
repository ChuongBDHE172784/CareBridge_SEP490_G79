package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.RiskLevel;

/**
 * LEGACY_V1_ONLY — do not use in Triage V2.
 *
 * <p>This interface lets a model return the triage colour directly, which V2 forbids
 * (invariant 1: the LLM never decides the outcome). It survives only for the V1 path and is
 * fenced off by {@code GeminiOutcomeBoundaryTest}. The V2 engine under
 * {@code com.carebridge.backend.triage.rules} must never reference it.
 *
 * <p>Implementations must fail closed: an unavailable or unparseable model raises
 * {@link com.carebridge.backend.triage.exception.AiOutcomeUnavailableException} rather than
 * returning a colour. Callers map that to NEEDS_MORE_INFO, never to GREEN.
 *
 * @deprecated LEGACY_V1_ONLY. Scheduled for removal once the V1 path is retired.
 */
@Deprecated
public interface GeminiTriageClient {

    record AiTriageResult(RiskLevel riskLevel, String disclaimer) {}

    AiTriageResult analyzeSymptoms(String constrainedPrompt);
}
