package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic out-of-scope complaint classification.
 *
 * <p>OUT_OF_SCOPE requires <em>positive evidence</em> that a complaint belongs to another
 * clinical domain. An unrecognised complaint is NEEDS_MORE_INFO — "we did not understand you"
 * is not the same statement as "this is not our field", and conflating them would let the
 * system dismiss anything it failed to parse.
 *
 * <p>Behavioural parity with {@code app/context/complaint_taxonomy.py}.
 */
@Component
public class ComplaintTaxonomy {

    public static final String TAXONOMY_RESOURCE = "triage/oos_complaint_taxonomy_v1.json";

    private final JsonNode taxonomy;

    public ComplaintTaxonomy() {
        this(TAXONOMY_RESOURCE);
    }

    ComplaintTaxonomy(String resourcePath) {
        try (InputStream stream = new ClassPathResource(resourcePath).getInputStream()) {
            this.taxonomy = new ObjectMapper().readTree(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read complaint taxonomy " + resourcePath, exception);
        }
    }

    /** {@code categoryId} is null when nothing matched — which means unknown, not out of scope. */
    public record ComplaintClassification(
            String categoryId, List<String> evidence, List<String> disqualified) {

        public boolean isConfirmedNonReproductive() {
            return categoryId != null;
        }
    }

    public ComplaintClassification classify(String message, Map<String, Object> signals) {
        String folded = TargetEntityResolver.fold(message);
        if (folded.isEmpty()) {
            return new ComplaintClassification(null, List.of(), List.of());
        }

        List<String> disqualified = new ArrayList<>();
        for (JsonNode category : taxonomy.get("categories")) {
            List<String> hits = TargetEntityResolver.containsAny(folded, category.get("phrases"));
            if (hits.isEmpty()) {
                continue;
            }

            boolean blocked = false;
            for (JsonNode code : category.path("excludedWhenAlso")) {
                if (signals != null
                        && Presence.parse(signals.get(code.asText())) == Presence.PRESENT) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) {
                // Matched the words, but a signal present in this session makes the
                // classification unsafe — e.g. bleeding alongside a reported fall.
                disqualified.add(category.get("categoryId").asText());
                continue;
            }
            return new ComplaintClassification(category.get("categoryId").asText(),
                    List.copyOf(hits), List.copyOf(disqualified));
        }
        return new ComplaintClassification(null, List.of(), List.copyOf(disqualified));
    }

    /**
     * All three policy conditions must hold before OUT_OF_SCOPE may be returned. The
     * safety-screen requirement is why "đau cổ tay" is not instantly out of scope: we must
     * first know the person is not also short of breath.
     */
    public boolean mayReturnOutOfScope(
            ComplaintClassification classification,
            boolean safetyScreenComplete,
            boolean hasReproductiveEvidence) {

        if (!classification.isConfirmedNonReproductive()) {
            return false;
        }
        JsonNode policy = taxonomy.path("policy");
        if (policy.path("requiresCompleteGlobalSafetyScreen").asBoolean(true)
                && !safetyScreenComplete) {
            return false;
        }
        return !(policy.path("requiresNoReproductiveEvidence").asBoolean(true)
                && hasReproductiveEvidence);
    }
}
