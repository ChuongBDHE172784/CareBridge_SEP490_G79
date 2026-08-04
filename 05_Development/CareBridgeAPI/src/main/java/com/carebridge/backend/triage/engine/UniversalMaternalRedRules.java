package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clinically approved life-threatening indicators shared by all maternal stages.
 * Stage-specific pregnancy thresholds remain outside this helper until separately approved.
 */
final class UniversalMaternalRedRules {

    // Kept in parity with Python's app/risk_rules.py negation set and clause-boundary regex.
    private static final Set<String> NEGATIONS = Set.of(
            "khong", "chua", "chang", "cha", "no", "not", "never", "without", "denies", "denied");
    private static final Set<String> CLAUSE_BOUNDARIES = Set.of(
            "va", "and", "nhung", "song", "ma", "however", "but");

    private UniversalMaternalRedRules() {
    }

    static PediatricRiskRules.RuleOutcome apply(RunIntakeRequest request, String rulePrefix) {
        List<String> redFlags = new ArrayList<>();
        List<String> matchedRules = new ArrayList<>();
        String breathing = normalize(request.getBreathingStatus());
        String consciousness = normalize(request.getConsciousnessStatus());
        String reportedSigns = normalize(String.join(" | ",
                request.getParentFreeText() == null ? "" : request.getParentFreeText(),
                request.getSymptoms() == null ? "" : request.getSymptoms(),
                request.getSymptomList() == null ? "" : String.join(" | ", request.getSymptomList())));

        boolean breathingDistress = containsAffirmed(breathing,
                "kho tho", "khong tho duoc",
                "difficulty breathing", "breathing difficulty", "shortness of breath", "cannot breathe",
                "unable to breathe")
                || containsAffirmed(reportedSigns,
                "kho tho", "khong tho duoc", "thieu hoi", "nghet tho",
                "difficulty breathing", "breathing difficulty", "shortness of breath", "cannot breathe",
                "unable to breathe");
        if (breathingDistress) {
            redFlags.add("Khó thở hoặc tím tái");
            matchedRules.add(rulePrefix + "_BREATHING_DISTRESS");
        }

        boolean cyanosis = containsAffirmed(breathing,
                "tim tai", "tim moi", "moi tim", "blue lips", "cyanosis", "cyanotic")
                || containsAffirmed(reportedSigns,
                "tim tai", "tim moi", "moi tim", "blue lips", "cyanosis", "cyanotic");
        if (cyanosis) {
            addUnique(redFlags, "Tím tái");
            addUnique(matchedRules, rulePrefix + "_CYANOSIS");
        }

        if (Boolean.TRUE.equals(request.getSeizure())
                || containsAffirmed(reportedSigns, "co giat", "seizure", "convulsion")) {
            redFlags.add("Co giật");
            matchedRules.add(rulePrefix + "_SEIZURE");
        }

        if (containsAffirmed(consciousness,
                "lo mo", "li bi", "kho danh thuc", "kho giu tinh tao", "bat tinh", "ngat", "ngat xiu",
                "faint", "fainted", "fainting", "unconscious", "loss of consciousness",
                "altered consciousness", "difficult to wake")
                || containsAffirmed(reportedSigns,
                "lo mo", "li bi", "kho danh thuc", "kho giu tinh tao", "bat tinh", "ngat", "ngat xiu",
                "faint", "fainted", "fainting", "unconscious", "loss of consciousness",
                "altered consciousness", "difficult to wake")) {
            redFlags.add("Thay đổi ý thức");
            matchedRules.add(rulePrefix + "_ALTERED_CONSCIOUSNESS");
        }

        if (containsAffirmed(reportedSigns,
                "bang huyet", "chay mau nhieu", "ra mau nhieu", "mat mau nhieu", "tham uot bang",
                "heavy bleeding", "bleeding heavily", "hemorrhage", "haemorrhage", "soaking pad")) {
            redFlags.add("Chảy máu nhiều");
            matchedRules.add(rulePrefix + "_HEAVY_BLEEDING");
        }

        if (containsAffirmed(reportedSigns,
                "tu lam hai", "lam hai ban than", "tu hai", "tu sat", "tu tu", "muon chet",
                "self harm", "suicide", "suicidal", "kill myself", "want to die")) {
            redFlags.add("Có ý nghĩ tự làm hại bản thân");
            matchedRules.add(rulePrefix + "_SELF_HARM");
        }

        return redFlags.isEmpty()
                ? null
                : new PediatricRiskRules.RuleOutcome("RED", redFlags, matchedRules);
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                // "tuy nhien" (however) is a two-word clause boundary; fold it into the same
                // pipe-delimiter treatment as punctuation so negation scope resets across it,
                // matching Python's risk_rules.py clause-split regex.
                .replaceAll("\\btuy nhien\\b", " | ")
                .replaceAll("[.,;!?]+", " | ")
                .replaceAll("[^a-z0-9|]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private static boolean containsAffirmed(String value, String... phrases) {
        for (String phrase : phrases) {
            Matcher matcher = Pattern.compile(
                    "(?<![a-z0-9])" + Pattern.quote(phrase) + "(?![a-z0-9])").matcher(value);
            while (matcher.find()) {
                if (!isNegated(value, matcher.start())) return true;
            }
        }
        return false;
    }

    private static boolean isNegated(String value, int phraseStart) {
        String prefix = value.substring(0, phraseStart);
        int delimiter = prefix.lastIndexOf('|');
        if (delimiter >= 0) prefix = prefix.substring(delimiter + 1);
        String trimmed = prefix.trim();
        if (trimmed.isEmpty()) return false;
        String[] words = trimmed.split("\\s+");
        int first = Math.max(0, words.length - 5);
        for (int index = words.length - 1; index >= first; index--) {
            if (CLAUSE_BOUNDARIES.contains(words[index])) return false;
            if (NEGATIONS.contains(words[index])) return true;
        }
        return false;
    }

    private static void addUnique(List<String> values, String value) {
        if (!values.contains(value)) values.add(value);
    }
}
