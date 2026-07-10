package com.carebridge.backend.triage.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SourceRetriever {

    public List<MedicalSource> retrieve(List<String> symptoms) {
        List<MedicalSource> sources = loadSources();
        return sources.stream()
                .filter(source -> matches(source, symptoms))
                .limit(4)
                .toList();
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
            if ("fever".equals(symptom) && "fever".equals(source.getTopic())) return true;
            if ("diarrhea".equals(symptom) && source.getTopic().contains("diarrhea")) return true;
            if ("dehydration".equals(symptom) && source.getTopic().contains("dehydration")) return true;
            if (("cough".equals(symptom) || "breathing_difficulty".equals(symptom)) && "respiratory".equals(source.getTopic())) return true;
            if (List.of("seizure", "lethargy", "poor_feeding", "cyanosis").contains(symptom)
                    && "danger_signs".equals(source.getTopic())) return true;
        }
        return false;
    }

    private String excerpt(String body) {
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 240 ? compact : compact.substring(0, 240);
    }
}
