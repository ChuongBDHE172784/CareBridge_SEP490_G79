package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PreconceptionRiskRules implements StageRiskRules {
    @Override
    public List<String> questions(RunIntakeRequest request) {
        List<String> questions = new ArrayList<>();
        if (request.getParentFreeText() == null || request.getParentFreeText().isBlank()) {
            questions.add("Bạn muốn được hỗ trợ nội dung nào khi chuẩn bị mang thai?");
        }
        questions.add("Bạn muốn hỏi về tiêm phòng trước thai kỳ, bổ sung vi chất hay chuẩn bị sức khỏe tổng quát?");
        return questions.stream().limit(3).toList();
    }

    @Override
    public PediatricRiskRules.RuleOutcome apply(RunIntakeRequest request, List<String> symptoms) {
        PediatricRiskRules.RuleOutcome universalRed = UniversalMaternalRedRules.apply(request, "RED_PRECONCEPTION");
        if (universalRed != null) return universalRed;
        // Preconception clinical escalation rules require specialist review before production activation.
        return new PediatricRiskRules.RuleOutcome(
                "NEED_MORE_INFO",
                List.of(),
                List.of("PRECONCEPTION_RULES_NEED_CLINICAL_REVIEW"));
    }
}
