package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MaternalPregnancyRiskRules implements StageRiskRules {
    @Override
    public List<String> questions(RunIntakeRequest request) {
        List<String> questions = new ArrayList<>();
        if (request.getParentFreeText() == null || request.getParentFreeText().isBlank()) {
            questions.add("Bạn đang gặp triệu chứng gì trong thai kỳ?");
        }
        if (request.getDuration() == null || request.getDuration().isBlank()) {
            questions.add("Triệu chứng đã xuất hiện bao lâu?");
        }
        questions.add("Bạn có ra máu âm đạo nhiều, đau bụng dữ dội, đau đầu dữ dội/hoa mắt hoặc giảm cử động thai không?");
        return questions.stream().limit(3).toList();
    }

    @Override
    public PediatricRiskRules.RuleOutcome apply(RunIntakeRequest request, List<String> symptoms) {
        PediatricRiskRules.RuleOutcome universalRed = UniversalMaternalRedRules.apply(request, "RED_PREGNANCY");
        if (universalRed != null) return universalRed;
        // Maternal thresholds require obstetric clinical sign-off before production activation.
        return new PediatricRiskRules.RuleOutcome(
                "NEED_MORE_INFO",
                List.of(),
                List.of("PREGNANCY_RULES_NEED_CLINICAL_REVIEW"));
    }
}
