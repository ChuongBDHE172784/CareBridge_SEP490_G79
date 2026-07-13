package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class PediatricRiskRules {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    public List<String> questions(RunIntakeRequest request) {
        List<String> questions = new ArrayList<>();
        if (request.getChildAgeMonths() == null) {
            questions.add("Trẻ hiện bao nhiêu tháng tuổi?");
        }
        if (request.getBreathingStatus() == null) {
            questions.add("Trẻ thở bình thường hay có khó thở/thở nhanh/tím tái?");
        }
        if (request.getConsciousnessStatus() == null) {
            questions.add("Trẻ tỉnh táo bình thường hay lơ mơ/li bì/khó đánh thức?");
        }
        if (request.getFeedingStatus() == null && questions.size() < 3) {
            questions.add("Trẻ còn bú/uống được như bình thường không?");
        }
        return questions.stream().limit(3).toList();
    }

    public RuleOutcome apply(RunIntakeRequest request, List<String> symptoms) {
        List<String> redFlags = new ArrayList<>();
        List<String> matchedRules = new ArrayList<>();

        addIf(symptoms.contains("difficulty_breathing") || symptoms.contains("chest_indrawing") || symptoms.contains("cyanosis")
                        || containsAny(request.getBreathingStatus(), "kho tho", "rut lom", "tim tai"), matchedRules, redFlags,
                "RED_BREATHING_DISTRESS", "Khó thở, thở bất thường hoặc tím tái");
        addIf(symptoms.contains("seizure"), matchedRules, redFlags, "RED_SEIZURE", "Co giật");
        addIf(symptoms.contains("lethargy") || symptoms.contains("difficult_to_wake"), matchedRules, redFlags,
                "RED_LETHARGY", "Lơ mơ, li bì hoặc khó đánh thức");
        addIf(symptoms.contains("poor_feeding") || symptoms.contains("unable_to_drink"), matchedRules, redFlags,
                "RED_POOR_FEEDING", "Bỏ bú hoặc không uống được");
        addIf((symptoms.contains("mild_dehydration") || symptoms.contains("severe_dehydration")) && symptoms.contains("diarrhea"), matchedRules, redFlags,
                "RED_DIARRHEA_DEHYDRATION", "Tiêu chảy kèm dấu hiệu mất nước");
        addIf(symptoms.contains("severe_dehydration") && !symptoms.contains("diarrhea"), matchedRules, redFlags,
                "RED_SEVERE_DEHYDRATION", "Dấu hiệu mất nước nặng");
        addIf(symptoms.contains("persistent_vomiting"), matchedRules, redFlags,
                "RED_PERSISTENT_VOMITING", "Nôn liên tục");
        addIf(symptoms.contains("rash") && hasHighFever(request), matchedRules, redFlags,
                "RED_RASH_HIGH_FEVER", "Phát ban kèm sốt cao");
        addIf(request.getChildAgeMonths() != null && request.getChildAgeMonths() < 3 && hasFever(request), matchedRules, redFlags,
                "RED_INFANT_FEVER_UNDER_3_MONTHS", "Trẻ dưới 3 tháng có sốt");
        addIf((request.getChildAgeMonths() == null || request.getChildAgeMonths() >= 3) && hasHighFever(request), matchedRules, redFlags,
                "RED_HIGH_FEVER", "Sốt cao");

        if (!redFlags.isEmpty()) {
            return new RuleOutcome("RED", redFlags, matchedRules);
        }

        if (symptoms.contains("fever") && durationPersistent(request.getDuration())) {
            matchedRules.add("YELLOW_PERSISTENT_FEVER");
        }
        if (symptoms.contains("cough") || symptoms.contains("runny_nose")) {
            matchedRules.add("YELLOW_RESPIRATORY_NO_DISTRESS");
        }
        if (symptoms.contains("diarrhea")) {
            matchedRules.add("YELLOW_MILD_MODERATE_DIARRHEA");
        }
        if (symptoms.contains("vomiting") && !symptoms.contains("persistent_vomiting")) {
            matchedRules.add("YELLOW_LIMITED_VOMITING");
        }
        if (symptoms.contains("rash")) {
            matchedRules.add("YELLOW_MILD_RASH");
        }
        if (symptoms.contains("fever") && hasFever(request) && breathingNormal(request) && !feedingBad(request)) {
            matchedRules.add("YELLOW_FEVER_MONITOR");
        }
        if (!matchedRules.isEmpty()) {
            return new RuleOutcome("YELLOW", redFlags, matchedRules);
        }
        matchedRules.add("GREEN_MILD_NO_RED_FLAGS");
        return new RuleOutcome("GREEN", redFlags, matchedRules);
    }

    private void addIf(boolean condition, List<String> rules, List<String> flags, String rule, String flag) {
        if (condition) {
            rules.add(rule);
            flags.add(flag);
        }
    }

    private boolean hasFever(RunIntakeRequest request) {
        return request.getTemperatureC() != null && request.getTemperatureC() >= 38.0;
    }

    private boolean hasHighFever(RunIntakeRequest request) {
        return request.getTemperatureC() != null && request.getTemperatureC() >= 39.0;
    }

    private boolean durationPersistent(String value) {
        return containsAny(value, "3", "keo dai", "nhieu ngay", "hon 2");
    }

    private boolean feedingBad(RunIntakeRequest request) {
        return containsAny(request.getFeedingStatus(), "bo", "khong", "kem");
    }

    private boolean breathingNormal(RunIntakeRequest request) {
        return !containsAny(request.getBreathingStatus(), "kho", "nhanh", "tim", "rut lom");
    }

    private boolean containsAny(String value, String... needles) {
        if (value == null) return false;
        String text = stripAccents(value);
        for (String needle : needles) {
            if (text.contains(needle)) return true;
        }
        return false;
    }

    private String stripAccents(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("");
    }

    public record RuleOutcome(String riskLevel, List<String> redFlags, List<String> matchedRules) {}
}
