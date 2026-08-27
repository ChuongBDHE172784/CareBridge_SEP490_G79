package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic intent resolution: what is the user actually asking for?
 *
 * <p>Intent decides whether a triage colour may be produced at all. Answering "dấu hiệu cảnh
 * báo thai kỳ là gì?" with RED would report an encyclopaedia question as an assessment of the
 * person asking — alarming, and wrong about what was even said.
 *
 * <p>Behavioural parity with {@code app/context/intent_resolver.py}, reading the same
 * {@code intent_indicators_v1.json}.
 */
@Component
public class IntentResolver {

    public static final String INDICATORS_RESOURCE = "triage/intent_indicators_v1.json";

    private final JsonNode indicators;

    public IntentResolver() {
        this(INDICATORS_RESOURCE);
    }

    IntentResolver(String resourcePath) {
        try (InputStream stream = new ClassPathResource(resourcePath).getInputStream()) {
            this.indicators = new ObjectMapper().readTree(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read intent indicators " + resourcePath, exception);
        }
    }

    public record IntentResolution(IntentType intent, ResolutionSource source, List<String> evidence) {
        public boolean mayProduceTriageOutcome() {
            return intent.mayProduceTriageOutcome();
        }
    }

    public IntentResolution resolve(
            String latestUserMessage,
            List<String> submittedOptionCodes,
            List<String> submittedQuestionIds) {
        return resolve(latestUserMessage, submittedOptionCodes, submittedQuestionIds, null);
    }

    public IntentResolution resolve(
            String latestUserMessage,
            List<String> submittedOptionCodes,
            List<String> submittedQuestionIds,
            IntentType confirmedConversationIntent) {

        // Structural, not textual: if the client answered a question we asked, this is a
        // follow-up regardless of how the free text reads.
        boolean answered = (submittedOptionCodes != null && !submittedOptionCodes.isEmpty())
                || (submittedQuestionIds != null && !submittedQuestionIds.isEmpty());
        if (answered) {
            List<String> evidence = new ArrayList<>();
            if (submittedOptionCodes != null) evidence.addAll(submittedOptionCodes);
            if (submittedQuestionIds != null) evidence.addAll(submittedQuestionIds);
            return new IntentResolution(IntentType.FOLLOW_UP_ANSWER,
                    ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER, List.copyOf(evidence));
        }

        String folded = TargetEntityResolver.fold(latestUserMessage);
        if (folded.isEmpty()) {
            return confirmedOrUnknown(confirmedConversationIntent);
        }

        List<String> symptomHits = new ArrayList<>(
                hits(folded, indicators.get("symptomTriage").get("firstPersonReportMarkers")));
        symptomHits.addAll(accentSensitiveHits(latestUserMessage,
                indicators.get("symptomTriage").get("accentSensitiveMarkers").get("words")));

        List<String> sourceHits = hits(folded, indicators.get("sourceLookup").get("phrases"));
        if (!sourceHits.isEmpty()) {
            return explicit(IntentType.SOURCE_LOOKUP, sourceHits);
        }

        List<String> outOfScopeHits = hits(folded, indicators.get("outOfScopeRequest").get("phrases"));
        if (!outOfScopeHits.isEmpty()) {
            return explicit(IntentType.OUT_OF_SCOPE_REQUEST, outOfScopeHits);
        }

        List<String> generalHits = hits(folded,
                indicators.get("generalHealthInformation").get("phrases"));
        if (!generalHits.isEmpty() && symptomHits.isEmpty()) {
            // A message that ALSO reports a symptom falls through to triage below — an
            // untriaged real symptom is the worse failure.
            return explicit(IntentType.GENERAL_HEALTH_INFORMATION, generalHits);
        }

        List<String> emergencyHits = hits(folded, indicators.get("emergencyHelp").get("phrases"));
        if (!emergencyHits.isEmpty() && symptomHits.isEmpty()) {
            return explicit(IntentType.EMERGENCY_HELP, emergencyHits);
        }

        if (!symptomHits.isEmpty()) {
            return explicit(IntentType.SYMPTOM_TRIAGE, symptomHits);
        }
        return confirmedOrUnknown(confirmedConversationIntent);
    }

    private static IntentResolution confirmedOrUnknown(IntentType confirmed) {
        if (confirmed != null && confirmed.isResolved()) {
            return new IntentResolution(
                    confirmed,
                    ResolutionSource.CONFIRMED_CONVERSATION_INTENT,
                    List.of(confirmed.name()));
        }
        return new IntentResolution(IntentType.UNKNOWN, ResolutionSource.NONE, List.of());
    }

    private static IntentResolution explicit(IntentType intent, List<String> evidence) {
        return new IntentResolution(intent, ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
                List.copyOf(new LinkedHashSet<>(evidence)));
    }

    /** Whole-word phrase matching — see {@link TargetEntityResolver#containsAny}. */
    private static List<String> hits(String folded, JsonNode phrases) {
        return TargetEntityResolver.containsAny(folded, phrases);
    }

    /**
     * Whole-word matches on the ACCENTED text. "dấu" (sign) and "đau" (pain) collapse to the
     * same string once accents are stripped, so folded matching would read "dấu hiệu cảnh báo
     * là gì?" as a report of pain.
     */
    private static List<String> accentSensitiveHits(String message, JsonNode words) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>(Arrays.asList(
                message.toLowerCase(Locale.ROOT).split("[\\s,.;!?]+")));
        List<String> found = new ArrayList<>();
        for (JsonNode word : words) {
            if (tokens.contains(word.asText().toLowerCase(Locale.ROOT))) {
                found.add(word.asText());
            }
        }
        return found;
    }
}
