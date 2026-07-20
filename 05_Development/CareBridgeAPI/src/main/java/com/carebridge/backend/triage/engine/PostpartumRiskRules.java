package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        List<String> redFlags = new ArrayList<>();
        List<String> matchedRules = new ArrayList<>();
        String breathing = normalize(request.getBreathingStatus());
        String consciousness = normalize(request.getConsciousnessStatus());
        String reportedSigns = normalize(String.join(" ",
                request.getParentFreeText() == null ? "" : request.getParentFreeText(),
                request.getSymptomList() == null ? "" : String.join(" ", request.getSymptomList()),
                symptoms == null ? "" : String.join(" ", symptoms)));

        if (containsAny(breathing, "kho tho", "tim tai", "tim moi", "khong tho duoc")
                || containsAny(reportedSigns, "kho tho", "khong tho duoc", "thieu hoi", "nghet tho")) {
            redFlags.add("Khó thở hoặc tím tái");
            matchedRules.add("RED_POSTPARTUM_BREATHING_DISTRESS");
        }
        if (containsAny(breathing, "tim tai", "tim moi")
                || containsAny(reportedSigns, "tim tai", "tim moi", "moi tim")) {
            addUnique(redFlags, "Tím tái");
            addUnique(matchedRules, "RED_POSTPARTUM_CYANOSIS");
        }
        if (Boolean.TRUE.equals(request.getSeizure()) || containsAny(reportedSigns, "co giat")) {
            redFlags.add("Co giật");
            matchedRules.add("RED_POSTPARTUM_SEIZURE");
        }
        if (containsAny(consciousness, "lo mo", "li bi", "kho danh thuc", "bat tinh", "ngat")
                || containsAny(reportedSigns, "lo mo", "li bi", "kho danh thuc", "bat tinh", "ngat xiu")) {
            redFlags.add("Thay đổi ý thức");
            matchedRules.add("RED_POSTPARTUM_ALTERED_CONSCIOUSNESS");
        }
        if (containsAny(reportedSigns,
                "bang huyet", "chay mau nhieu", "ra mau nhieu", "mat mau nhieu", "tham uot bang")) {
            redFlags.add("Chảy máu nhiều");
            matchedRules.add("RED_POSTPARTUM_HEAVY_BLEEDING");
        }
        if (containsAny(reportedSigns,
                "tu lam hai", "lam hai ban than", "tu hai", "tu sat", "muon chet",
                "self harm", "suicid")) {
            redFlags.add("Có ý nghĩ tự làm hại bản thân");
            matchedRules.add("RED_POSTPARTUM_SELF_HARM");
        }
        if (!redFlags.isEmpty()) {
            return new PediatricRiskRules.RuleOutcome("RED", redFlags, matchedRules);
        }
        return new PediatricRiskRules.RuleOutcome(
                "NEED_MORE_INFO",
                List.of(),
                List.of("POSTPARTUM_RULES_REQUIRE_CLINICAL_REVIEW"));
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) return true;
        }
        return false;
    }

    private void addUnique(List<String> values, String value) {
        if (!values.contains(value)) values.add(value);
    }
}
