package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PostpartumRiskRules implements StageRiskRules {

    @Override
    public List<String> questions(RunIntakeRequest request) {
        List<String> questions = new ArrayList<>();
        if (request.getParentFreeText() == null || request.getParentFreeText().isBlank()) {
            questions.add("Bạn đang gặp dấu hiệu nào trong quá trình hồi phục sau sinh?");
        }
        if (request.getDuration() == null || request.getDuration().isBlank()) {
            questions.add("Dấu hiệu đã xuất hiện bao lâu và có tăng nhanh không?");
        }
        questions.add("Bạn có khó thở, tím tái, co giật, lơ mơ hoặc khó đánh thức không?");
        return questions.stream().limit(3).toList();
    }

    @Override
    public PediatricRiskRules.RuleOutcome apply(RunIntakeRequest request, List<String> symptoms) {
        PediatricRiskRules.RuleOutcome universalRed = UniversalMaternalRedRules.apply(request, "RED_POSTPARTUM");
        if (universalRed != null) return universalRed;
        return new PediatricRiskRules.RuleOutcome(
                "NEED_MORE_INFO",
                List.of(),
                List.of("POSTPARTUM_RULES_REQUIRE_CLINICAL_REVIEW"));
    }
}
