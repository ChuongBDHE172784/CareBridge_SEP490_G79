package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic target-entity resolution: is this session about the mother or the baby?
 *
 * <p>Getting this wrong is not a UX bug. Deciding "mother" for a message about an infant
 * applies maternal thresholds to a baby and asks the wrong questions. The resolver is
 * therefore conservative: UNKNOWN rather than guess, CONFLICTED rather than pick a side.
 *
 * <p>Behavioural parity with {@code app/context/target_entity_resolver.py}; both read the same
 * {@code target_entity_indicators_v1.json}, because two different phrase lists would mean the
 * two runtimes disagree about who the session is about.
 */
@Component
public class TargetEntityResolver {

    public static final String INDICATORS_RESOURCE = "triage/target_entity_indicators_v1.json";

    private final JsonNode indicators;

    public TargetEntityResolver() {
        this(INDICATORS_RESOURCE);
    }

    TargetEntityResolver(String resourcePath) {
        try (InputStream stream = new ClassPathResource(resourcePath).getInputStream()) {
            this.indicators = new ObjectMapper().readTree(stream);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "cannot read target entity indicators " + resourcePath, exception);
        }
    }

    /** The resolved entity plus how it was reached, for the audit trail. */
    public record TargetResolution(
            TargetEntity entity,
            ResolutionSource source,
            List<String> evidence,
            List<String> conflictEvidence) {

        public boolean isResolved() {
            return entity.isResolved();
        }
    }

    public TargetResolution resolve(
            String clarificationAnswer,
            String latestUserMessage,
            TargetEntity selectedProfileEntity,
            TargetEntity confirmedConversationTarget,
            CareStage stage) {

        TargetEntity clarified = fromClarification(clarificationAnswer);
        if (clarified != null) {
            return new TargetResolution(clarified, ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER,
                    List.of(clarificationAnswer), List.of());
        }

        TargetResolution scored = scoreMessage(latestUserMessage);
        if (scored.entity() == TargetEntity.CONFLICTED || scored.entity().isResolved()) {
            return scored;
        }

        if (selectedProfileEntity != null && selectedProfileEntity.isResolved()) {
            return new TargetResolution(selectedProfileEntity,
                    ResolutionSource.EXPLICIT_SELECTED_PROFILE, List.of(), List.of());
        }
        if (confirmedConversationTarget != null && confirmedConversationTarget.isResolved()) {
            return new TargetResolution(confirmedConversationTarget,
                    ResolutionSource.CONFIRMED_CONVERSATION_TARGET, List.of(), List.of());
        }

        TargetEntity fromStage = fromStage(stage);
        if (fromStage != null) {
            return new TargetResolution(fromStage, ResolutionSource.STAGE_SPECIFIC_CONTEXT,
                    List.of(), List.of());
        }
        return new TargetResolution(TargetEntity.UNKNOWN, ResolutionSource.NONE,
                List.of(), List.of());
    }

    /** Score a raw message alone, with no profile or conversation context. */
    public TargetResolution scoreMessage(String message) {
        String folded = fold(message);
        if (folded.isEmpty()) {
            return new TargetResolution(TargetEntity.UNKNOWN, ResolutionSource.NONE,
                    List.of(), List.of());
        }

        List<String> multi = containsAny(folded,
                indicators.get("multiEntityPhrases").get("phrases"));
        if (!multi.isEmpty()) {
            // "Tôi và bé đều bị sốt" — never silently pick one. One session, one target.
            return new TargetResolution(TargetEntity.CONFLICTED,
                    ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE, List.of(), multi);
        }

        boolean thirdParty = !containsAny(folded,
                indicators.get("thirdPartyPhrases").get("phrases")).isEmpty();

        LinkedHashSet<String> motherHits = new LinkedHashSet<>();
        LinkedHashSet<String> babyHits = new LinkedHashSet<>();

        for (String clause : clauses(message)) {
            List<String> babyPossessive = containsAny(clause,
                    indicators.get("baby").get("possessiveOrSubject"));
            List<String> babyStrong = containsAny(clause,
                    indicators.get("baby").get("strongIndicators"));
            if (!babyPossessive.isEmpty() || !babyStrong.isEmpty()) {
                // A possessed child outranks the pronoun that introduced it:
                // "tôi hỏi giúp con tôi, bé bị sốt" is about the baby.
                babyHits.addAll(babyPossessive);
                babyHits.addAll(babyStrong);
                continue;
            }
            List<String> motherStrong = containsAny(clause,
                    indicators.get("mother").get("strongIndicators"));
            if (!motherStrong.isEmpty()) {
                motherHits.addAll(motherStrong);
                continue;
            }
            List<String> motherSubject = containsAny(clause,
                    indicators.get("mother").get("possessiveOrSubject"));
            if (!motherSubject.isEmpty() && !thirdParty) {
                motherHits.addAll(motherSubject);
            }
        }

        if (!motherHits.isEmpty() && !babyHits.isEmpty()) {
            List<String> conflict = new ArrayList<>(motherHits);
            conflict.addAll(babyHits);
            return new TargetResolution(TargetEntity.CONFLICTED,
                    ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE, List.of(), List.copyOf(conflict));
        }
        if (!babyHits.isEmpty()) {
            return new TargetResolution(TargetEntity.BABY,
                    ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE, List.copyOf(babyHits), List.of());
        }
        if (!motherHits.isEmpty()) {
            return new TargetResolution(TargetEntity.MOTHER,
                    ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE, List.copyOf(motherHits), List.of());
        }
        return new TargetResolution(TargetEntity.UNKNOWN, ResolutionSource.NONE,
                List.of(), List.of());
    }

    private static TargetEntity fromClarification(String answer) {
        if (answer == null) {
            return null;
        }
        return switch (answer) {
            case "CLARIFY_TARGET_MOTHER" -> TargetEntity.MOTHER;
            case "CLARIFY_TARGET_BABY" -> TargetEntity.BABY;
            case "CLARIFY_TARGET_BOTH" -> TargetEntity.CONFLICTED;
            default -> null;
        };
    }

    private static TargetEntity fromStage(CareStage stage) {
        if (stage == null || !stage.isResolved()) {
            return null;
        }
        return switch (stage) {
            case INFANT_0_12M, TODDLER_12_24M -> TargetEntity.BABY;
            case PRECONCEPTION, POSSIBLE_PREGNANCY, PREGNANCY -> TargetEntity.MOTHER;
            // POSTPARTUM_MOTHER names the mother's stage, but postpartum is exactly when a
            // newborn question is most likely. Not strong enough evidence on its own.
            default -> null;
        };
    }

    /**
     * Split into clauses on the ACCENTED text, then fold each clause for matching.
     *
     * <p>Splitting on folded text is wrong in Vietnamese: the conjunction "còn" folds to "con",
     * the word for a child, so "giúp con tôi" would be cut in half and the baby reference lost.
     * Accents are only safe to drop once the boundaries are fixed.
     */
    private List<String> clauses(String message) {
        String lowered = message == null ? "" : message.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
        if (lowered.isEmpty()) {
            return List.of();
        }
        List<String> separators = new ArrayList<>();
        indicators.get("matchingRules").get("clauseSeparators")
                .forEach(node -> separators.add(java.util.regex.Pattern.quote(
                        node.asText().toLowerCase(Locale.ROOT))));
        List<String> result = new ArrayList<>();
        for (String part : lowered.split(String.join("|", separators))) {
            String folded = fold(part);
            if (!folded.isEmpty()) {
                result.add(folded);
            }
        }
        return result;
    }

    /**
     * Whole-word phrase matching.
     *
     * <p>Raw substring matching is unsafe here: "tã" (nappy) folds to "ta", which then matches
     * inside "tay" (hand) and "tập" (exercise), so "đau cổ tay sau khi tập" scored as a baby
     * message. Short Vietnamese tokens make this easy to hit, so phrases are anchored.
     */
    static List<String> containsAny(String haystack, JsonNode phrases) {
        List<String> hits = new ArrayList<>();
        for (JsonNode phrase : phrases) {
            String folded = fold(phrase.asText());
            if (folded.isEmpty()) {
                continue;
            }
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?<![a-z0-9])" + java.util.regex.Pattern.quote(folded) + "(?![a-z0-9])");
            if (pattern.matcher(haystack).find()) {
                hits.add(phrase.asText());
            }
        }
        return hits;
    }

    /** Lowercase, strip Vietnamese accents, collapse whitespace. */
    static String fold(String text) {
        if (text == null) {
            return "";
        }
        String lowered = text.toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Exposed for the parity test so both runtimes assert the same source file. */
    Map<String, JsonNode> sections() {
        return Map.of(
                "mother", indicators.get("mother"),
                "baby", indicators.get("baby"),
                "multiEntityPhrases", indicators.get("multiEntityPhrases"),
                "thirdPartyPhrases", indicators.get("thirdPartyPhrases"));
    }
}
