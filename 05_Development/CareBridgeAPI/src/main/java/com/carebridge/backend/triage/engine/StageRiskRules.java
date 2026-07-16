package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;

import java.util.List;

public interface StageRiskRules {
    List<String> questions(RunIntakeRequest request);
    PediatricRiskRules.RuleOutcome apply(RunIntakeRequest request, List<String> symptoms);
}
