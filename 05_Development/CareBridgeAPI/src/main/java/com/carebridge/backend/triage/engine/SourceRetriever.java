package com.carebridge.backend.triage.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SourceRetriever {
    private static final Pattern AGE_RANGE_PATTERN = Pattern.compile("^(\\d+)\\s*-\\s*(\\d+)\\s*(months|years)$");

    public List<MedicalSource> retrieve(List<String> symptoms, Integer childAgeMonths) {
        List<MedicalSource> sources = loadSources();
        List<MedicalSource> matched = sources.stream()
                .filter(this::hasDeepLink)
                .filter(source -> appliesToAge(source, childAgeMonths))
                .filter(source -> matches(source, symptoms))
                .toList();
        Map<String, MedicalSource> byUrl = new LinkedHashMap<>();
        matched.forEach(source -> byUrl.putIfAbsent(source.getUrl(), source));
        return byUrl.values().stream().limit(4).toList();
    }

    public List<MedicalSource> retrieve(List<String> symptoms) {
        return retrieve(symptoms, null);
    }

    public List<TriageCitation> citations(List<MedicalSource> sources) {
        return sources.stream()
                .map(source -> TriageCitation.builder()
                        .title(source.getTitle())
                        .source(source.getOrganization())
                        .url(source.getUrl())
                        .excerpt(excerpt(source.getBody()))
                        .retrievedAt(Instant.now().toString())
                        .build())
                .toList();
    }

    private List<MedicalSource> loadSources() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:medical_sources/*.md");
            List<MedicalSource> sources = new ArrayList<>();
            for (Resource resource : resources) {
                sources.add(parse(resource.getContentAsString(StandardCharsets.UTF_8)));
            }
            return sources;
        } catch (IOException e) {
            log.warn("Unable to load triage medical sources: {}", e.getMessage());
            return List.of();
        }
    }

    private MedicalSource parse(String raw) {
        Map<String, String> metadata = new LinkedHashMap<>();
        String body = raw;
        if (raw.startsWith("---")) {
            String[] parts = raw.split("---", 3);
            if (parts.length == 3) {
                for (String line : parts[1].split("\\R")) {
                    int idx = line.indexOf(':');
                    if (idx > 0) {
                        metadata.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                    }
                }
                body = parts[2].trim();
            }
        }
        return MedicalSource.builder()
                .title(metadata.getOrDefault("title", "Medical source"))
                .organization(metadata.getOrDefault("organization", "Hospital Guideline"))
                .url(metadata.getOrDefault("url", ""))
                .lastReviewed(metadata.getOrDefault("lastReviewed", ""))
                .topic(metadata.getOrDefault("topic", ""))
                .ageRange(metadata.getOrDefault("ageRange", ""))
                .body(body)
                .build();
    }

    private boolean matches(MedicalSource source, List<String> symptoms) {
        String haystack = (source.getTopic() + " " + source.getTitle() + " " + source.getBody()).toLowerCase();
        for (String symptom : symptoms) {
            if (haystack.contains(symptom)) return true;
            if (List.of("fever", "high_fever").contains(symptom) && "fever".equals(source.getTopic())) return true;
            if ("diarrhea".equals(symptom) && source.getTopic().contains("diarrhea")) return true;
            if (List.of("mild_dehydration", "severe_dehydration").contains(symptom)
                    && source.getTopic().contains("dehydration")) return true;
            if (("cough".equals(symptom) || "difficulty_breathing".equals(symptom))
                    && List.of("respiratory", "danger_signs").contains(source.getTopic())) return true;
            if ("cyanosis".equals(symptom)
                    && List.of("respiratory", "danger_signs").contains(source.getTopic())) return true;
            if (List.of("vomiting", "persistent_vomiting").contains(symptom) && "vomiting".equals(source.getTopic())) return true;
            if (List.of("seizure", "lethargy", "difficult_to_wake", "poor_feeding", "unable_to_drink").contains(symptom)
                    && "danger_signs".equals(source.getTopic())) return true;
        }
        return false;
    }

    private boolean hasDeepLink(MedicalSource source) {
        try {
            URI uri = URI.create(source.getUrl());
            String path = uri.getPath() == null ? "" : uri.getPath().replace("/", "").trim();
            boolean valid = "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && !path.isBlank()
                    && !List.of("vi", "en").contains(path.toLowerCase());
            if (!valid) {
                log.warn("Skipping triage source without a content deep-link: {}", source.getTitle());
            }
            return valid;
        } catch (IllegalArgumentException exception) {
            log.warn("Skipping triage source with an invalid URL: {}", source.getTitle());
            return false;
        }
    }

    private boolean appliesToAge(MedicalSource source, Integer childAgeMonths) {
        if (childAgeMonths == null || childAgeMonths < 0) {
            return false;
        }
        if (source.getAgeRange() == null || source.getAgeRange().isBlank()) {
            log.warn("Skipping triage source without an age range: {}", source.getTitle());
            return false;
        }
        Matcher matcher = AGE_RANGE_PATTERN.matcher(source.getAgeRange().toLowerCase().trim());
        if (!matcher.matches()) {
            log.warn("Skipping triage source with an invalid age range: {}", source.getTitle());
            return false;
        }
        int minimum = Integer.parseInt(matcher.group(1));
        int maximum = Integer.parseInt(matcher.group(2));
        if ("years".equals(matcher.group(3))) {
            minimum *= 12;
            maximum *= 12;
        }
        return childAgeMonths >= minimum && childAgeMonths <= maximum;
    }

    private String excerpt(String body) {
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 240 ? compact : compact.substring(0, 240);
    }
}
