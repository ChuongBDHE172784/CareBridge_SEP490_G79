package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SymptomNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    // Negation grammar mirrors Python's app/symptom_normalizer.py::_without_negated_candidate
    // so free-text negation ("không khó thở", "không co giật") is not misread as the symptom
    // being present. Keywords that ARE themselves a negative phrasing (e.g. "khong uong") are
    // exempt, matching Python's same guard.
    private static final String NEGATION_PREFIX_PATTERN =
            "(?<![a-z0-9])(?:khong|chua|not|no|never|without)\\s+"
                    + "(?:(?:co|bi|con|he|have|has|had|feel|feeling|any|signs?|symptoms?|of|currently)\\s+){0,4}";

    // Folk-term synonyms per TDS CB-TRIAGE-IMP-005 §5.3 (S2, S4–S13). Values are the engine's
    // accent-stripped forms; 'đ' (U+0111) has no NFD decomposition, so đ-variants ("lu đu",
    // "đi ngoai") are stored alongside the plain-d forms to match diacritic input.
    private static final Map<String, List<String>> KEYWORDS = Map.ofEntries(
            Map.entry("fever", List.of("sot", "nong", "temperature", "fever", "ham hap")),
            Map.entry("high_fever", List.of("sot cao", "high fever")),
            Map.entry("cough", List.of(" ho ", "cough")),
            Map.entry("runny_nose", List.of("so mui", "chay mui", "runny", "sut sit")),
            Map.entry("difficulty_breathing", List.of("kho tho", "tho gap", "wheeze", "kho khe", "tho rit")),
            Map.entry("chest_indrawing", List.of("rut lom")),
            Map.entry("cyanosis", List.of("tim tai", "moi tim", "da tim")),
            // "seizures"/"convulsions" plural aliases: the switch to word-boundary matching
            // (regression fix) no longer lets "seizure" match inside "seizures" as a substring.
            Map.entry("seizure", List.of("co giat", "seizure", "seizures", "convulsion", "convulsions")),
            Map.entry("lethargy", List.of("li bi", "lo mo", "ngu ga", "lu du", "lu đu")),
            // 'đ' has no NFD decomposition (see class comment); "kho đanh thuc" must be listed
            // explicitly alongside "kho danh thuc" or the diacritic input never matches.
            Map.entry("difficult_to_wake", List.of("kho danh thuc", "kho đanh thuc", "kho giu tinh tao")),
            Map.entry("unable_to_drink", List.of("khong uong", "khong bu")),
            Map.entry("poor_feeding", List.of("bo bu", "uong kem", "an kem", "bieng an")),
            Map.entry("vomiting", List.of("non", "oi", "vomit", "vomiting", "tro sua", "oc sua")),
            Map.entry("persistent_vomiting", List.of("non lien tuc", "non nhieu", "vomiting everything")),
            Map.entry("diarrhea", List.of("tieu chay", "diarrhea", "di ngoai", "đi ngoai", "ia chay")),
            Map.entry("rash", List.of("phat ban", "noi ban", "rash", "rom say")),
            Map.entry("mild_dehydration", List.of("mat nuoc", "moi kho", "tieu it")),
            Map.entry("severe_dehydration", List.of("mat nuoc nang", "khoc khong co nuoc mat", "mat trung"))
    );

    public List<String> normalize(RunIntakeRequest request) {
        String text = toSearchText(request);
        Set<String> normalized = new LinkedHashSet<>();
        for (var entry : KEYWORDS.entrySet()) {
            if (entry.getValue().stream().anyMatch(keyword -> matchesUnnegated(text, keyword))) {
                normalized.add(entry.getKey());
            }
        }
        if (request.getTemperatureC() != null && request.getTemperatureC() >= 37.5) {
            normalized.add("fever");
        }
        if (request.getTemperatureC() != null && request.getTemperatureC() >= 39.0) {
            normalized.add("high_fever");
        }
        if (Boolean.TRUE.equals(request.getSeizure())) {
            normalized.add("seizure");
        }
        if (request.getDehydrationSigns() != null && !request.getDehydrationSigns().isEmpty()) {
            boolean severe = request.getDehydrationSigns().stream()
                    .map(this::stripAccents)
                    .anyMatch(sign -> sign.contains("mat nuoc nang") || sign.contains("mat trung")
                            || sign.contains("khoc khong co nuoc mat"));
            normalized.add(severe ? "severe_dehydration" : "mild_dehydration");
        }
        return new ArrayList<>(normalized);
    }

    /** Word-boundary match that ignores occurrences immediately preceded by a negation phrase. */
    private boolean matchesUnnegated(String text, String rawKeyword) {
        String keyword = rawKeyword.trim();
        if (keyword.isEmpty()) return false;
        String scoped = keyword.startsWith("khong ") || keyword.startsWith("chua ")
                ? text
                : stripNegatedOccurrences(text, keyword);
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(keyword) + "(?![a-z0-9])")
                .matcher(scoped).find();
    }

    private String stripNegatedOccurrences(String text, String keyword) {
        Pattern pattern = Pattern.compile(NEGATION_PREFIX_PATTERN + Pattern.quote(keyword) + "(?![a-z0-9])");
        return pattern.matcher(text).replaceAll(" ");
    }

    private String toSearchText(RunIntakeRequest request) {
        List<String> parts = new ArrayList<>();
        if (request.getSymptomList() != null) parts.addAll(request.getSymptomList());
        parts.add(nullToBlank(request.getSymptoms()));
        parts.add(nullToBlank(request.getDuration()));
        parts.add(nullToBlank(request.getFeedingStatus()));
        parts.add(nullToBlank(request.getBreathingStatus()));
        parts.add(nullToBlank(request.getConsciousnessStatus()));
        parts.add(nullToBlank(request.getVomiting()));
        parts.add(nullToBlank(request.getDiarrhea()));
        parts.add(nullToBlank(request.getRash()));
        parts.add(nullToBlank(request.getParentFreeText()));
        if (request.getDehydrationSigns() != null) parts.addAll(request.getDehydrationSigns());
        return " " + stripAccents(String.join(" ", parts)).replaceAll("\\s+", " ") + " ";
    }

    private String stripAccents(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("");
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
