package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.TriageStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskRuleFactory {
    private final PreconceptionRiskRules preconceptionRiskRules;
    private final MaternalPregnancyRiskRules maternalPregnancyRiskRules;
    private final PediatricInfantRiskRules pediatricInfantRiskRules;
    private final PediatricToddlerRiskRules pediatricToddlerRiskRules;

    public StageRiskRules forStage(TriageStage stage) {
        return switch (stage == null ? TriageStage.INFANT : stage) {
            case PRECONCEPTION -> preconceptionRiskRules;
            case PREGNANCY -> maternalPregnancyRiskRules;
            case TODDLER -> pediatricToddlerRiskRules;
            case INFANT -> pediatricInfantRiskRules;
        };
    }
}
