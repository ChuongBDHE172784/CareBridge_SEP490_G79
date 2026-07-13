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

    private static final Map<String, List<String>> KEYWORDS = Map.ofEntries(
            Map.entry("fever", List.of("sot", "nong", "temperature", "fever")),
            Map.entry("cough", List.of(" ho ", "cough")),
            Map.entry("runny_nose", List.of("so mui", "chay mui", "runny")),
            Map.entry("breathing_difficulty", List.of("kho tho", "tho gap", "rut lom", "tim tai", "wheeze")),
            Map.entry("cyanosis", List.of("tim tai", "moi tim", "da tim")),
            Map.entry("seizure", List.of("co giat", "seizure", "convulsion")),
            Map.entry("lethargy", List.of("li bi", "lo mo", "kho danh thuc", "ngu ga")),
            Map.entry("poor_feeding", List.of("bo bu", "khong uong", "khong bu", "uống kém", "an kem")),
            Map.entry("vomiting", List.of("non", "oi", "vomit")),
            Map.entry("diarrhea", List.of("tieu chay", "diarrhea")),
            Map.entry("rash", List.of("phat ban", "noi ban", "rash")),
            Map.entry("dehydration", List.of("mat nuoc", "khoc khong co nuoc mat", "moi kho", "tieu it", "mat trung"))
    );

    public List<String> normalize(RunIntakeRequest request) {
        String text = toSearchText(request);
        Set<String> normalized = new LinkedHashSet<>();
        for (var entry : KEYWORDS.entrySet()) {
            if (entry.getValue().stream().anyMatch(text::contains)) {
                normalized.add(entry.getKey());
            }
        }
        if (request.getTemperatureC() != null && request.getTemperatureC() >= 37.5) {
            normalized.add("fever");
        }
        if (Boolean.TRUE.equals(request.getSeizure())) {
            normalized.add("seizure");
        }
        if (request.getDehydrationSigns() != null && !request.getDehydrationSigns().isEmpty()) {
            normalized.add("dehydration");
        }
        return new ArrayList<>(normalized);
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
