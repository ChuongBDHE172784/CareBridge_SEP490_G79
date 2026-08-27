package com.carebridge.backend.triage.rules;

import com.carebridge.backend.triage.exception.TriageException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a {@code (questionId, optionCode)} answer into validated {@link SignalMutation}s.
 *
 * <p>This is the server's single trusted route from "what the user tapped" to "what the engine
 * believes". A client is never a source of clinical truth: it sends the identity of the question
 * it was asked and the identity of the option it chose, and this mapper — using the frozen
 * question catalogue and the reviewed mapping contract — decides what that means.
 *
 * <p>The mapper is deliberately incapable of producing a triage outcome. It emits signals only;
 * RED/YELLOW/GREEN remain the deterministic rule engine's alone. An architecture test asserts
 * that no outcome vocabulary appears in this class.
 *
 * <p>{@code Contracts/triage/canonical_answer_mapping_v1.json} currently ships with an empty
 * mapping table, because deciding that an option implies a signal presence is rule authoring and
 * needs internal review first. Until entries exist, every structurally valid answer resolves to
 * {@link Status#UNSUPPORTED_MAPPING}: the question is recorded as answered, no signal is invented,
 * and nothing can be promoted to GREEN.
 */
@Component
public class CanonicalAnswerMapper {

    public static final String MAPPING_RESOURCE = "triage/canonical_answer_mapping_v1.json";

    /** Where a belief came from. Provenance travels with the signal and is never discarded. */
    public enum Provenance {
        USER_REPORTED,
        QUESTION_ANSWER,
        MEASURED,
        LLM_EXTRACTED_VALIDATED,
        PROFILE_CONTEXT,
        HEALTH_MEMORY_CONTEXT;

        /**
         * Remembered background is not an observation of now. Profile and health-memory context
         * may inform questions but must never assert a current PRESENT signal on their own.
         */
        public boolean mayAssertCurrentPresent() {
            return this != PROFILE_CONTEXT && this != HEALTH_MEMORY_CONTEXT;
        }
    }

    public enum ConflictStatus {
        NONE,
        CONFLICTED
    }

    public enum Status {
        MAPPED,
        UNSUPPORTED_MAPPING
    }

    public record SignalMutation(
            String signalCode,
            String presence,
            boolean current,
            Provenance provenance,
            String sourceMessageId,
            String sourceQuestionId,
            String sourceOptionCode,
            String extractedEvidenceSpan,
            String mappingRuleVersion,
            ConflictStatus conflictStatus) {

        /**
         * The shape the Python workflow accepts for a single signal observation, carrying its
         * provenance so the engine and the audit trail can tell an explicitly chosen answer apart
         * from a model-derived guess.
         */
        public Map<String, Object> toObservation() {
            Map<String, Object> observation = new LinkedHashMap<>();
            observation.put("presence", presence);
            observation.put("current", current);
            observation.put("temporalStatus", current ? "CURRENT" : "HISTORICAL");
            observation.put("provenance", provenance.name());
            observation.put("conflictStatus", conflictStatus.name());
            if (sourceQuestionId != null) observation.put("sourceQuestionId", sourceQuestionId);
            if (sourceOptionCode != null) observation.put("sourceOptionCode", sourceOptionCode);
            if (mappingRuleVersion != null) observation.put("mappingRuleVersion", mappingRuleVersion);
            return observation;
        }
    }

    /**
     * @param status            whether an approved mapping existed for the answer
     * @param answeredQuestionId the question the user actually answered, recorded even when the
     *                          mapping table cannot yet interpret the chosen option, so the
     *                          planner stops re-asking a question the user has already dealt with
     * @param mutations         validated signal mutations; empty for {@link Status#UNSUPPORTED_MAPPING}
     */
    public record AnswerMapping(
            Status status, String answeredQuestionId, List<SignalMutation> mutations) {

        public Map<String, Object> toSignals() {
            Map<String, Object> signals = new LinkedHashMap<>();
            mutations.forEach(mutation -> signals.put(mutation.signalCode(), mutation.toObservation()));
            return signals;
        }
    }

    private record MappingKey(String questionId, String optionCode) {
    }

    private record MappingEntry(String signalCode, String presence, boolean current) {
    }

    private static final Set<String> PRESENCE =
            Set.of("PRESENT", "ABSENT", "UNKNOWN", "CONFLICTED", "UNAWARE_OR_UNMEASURABLE");

    private final QuestionCatalog catalog;
    private final Map<MappingKey, List<MappingEntry>> mappings;
    private final String mappingRuleVersion;
    private final String reviewStatus;

    /**
     * The constructor Spring must use. Without this marker the second, test-only constructor
     * makes the class ambiguous to inject and container startup fails.
     */
    @Autowired
    public CanonicalAnswerMapper(QuestionCatalog catalog, TriageReadinessService readinessService) {
        this(catalog, MAPPING_RESOURCE, readinessService.registry()
                .map(registry -> registry.signalDisplayText().keySet())
                .orElse(null));
    }

    /** Loads a named mapping resource without a registry cross-check. Used by tests and fixtures. */
    public CanonicalAnswerMapper(QuestionCatalog catalog, String resourcePath) {
        this(catalog, resourcePath, null);
    }

    /**
     * @param knownSignalCodes signal codes the rule registry recognises, or {@code null} when the
     *     registry is unavailable. A mapping naming a signal no rule can read would be silently
     *     inert, so it is rejected at startup whenever the catalogue can be consulted.
     */
    CanonicalAnswerMapper(QuestionCatalog catalog, String resourcePath, Set<String> knownSignalCodes) {
        this.catalog = catalog;
        JsonNode document;
        try (InputStream stream = new ClassPathResource(resourcePath).getInputStream()) {
            document = new ObjectMapper().readTree(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read answer mapping " + resourcePath, exception);
        }
        this.mappingRuleVersion = document.path("mappingRuleVersion").asText("UNKNOWN");
        this.reviewStatus = document.path("reviewStatus").asText("PENDING_INTERNAL_REVIEW");

        Map<MappingKey, List<MappingEntry>> loaded = new LinkedHashMap<>();
        for (JsonNode entry : document.path("mappings")) {
            String questionId = entry.path("questionId").asText();
            String optionCode = entry.path("optionCode").asText();
            String signalCode = entry.path("signalCode").asText();
            String presence = entry.path("presence").asText();
            if (!PRESENCE.contains(presence)) {
                throw new IllegalStateException("invalid presence in answer mapping: " + presence);
            }
            // A mapping that names a question or option the catalogue does not have would
            // silently never fire. Fail at startup instead of at a patient's turn.
            QuestionCatalog.Question question = catalog.question(questionId)
                    .orElseThrow(() -> new IllegalStateException(
                            "answer mapping references unknown question " + questionId));
            if (question.options().stream().noneMatch(option -> option.optionCode().equals(optionCode))) {
                throw new IllegalStateException(
                        "answer mapping references unknown option " + optionCode + " for " + questionId);
            }
            if (knownSignalCodes != null && !knownSignalCodes.contains(signalCode)) {
                throw new IllegalStateException(
                        "answer mapping references unknown signal " + signalCode);
            }
            loaded.computeIfAbsent(new MappingKey(questionId, optionCode), key -> new ArrayList<>())
                    .add(new MappingEntry(signalCode, presence, entry.path("current").asBoolean(true)));
        }
        this.mappings = Map.copyOf(loaded);
    }

    public String mappingRuleVersion() {
        return mappingRuleVersion;
    }

    public String reviewStatus() {
        return reviewStatus;
    }

    public boolean isPopulated() {
        return !mappings.isEmpty();
    }

    /**
     * Validates an answer against the frozen catalogue and resolves it to signal mutations.
     *
     * @throws TriageException when the answer cannot be trusted at all: an unknown question, an
     *     option that does not belong to that question, or a question that does not apply to this
     *     session's target entity or stage. These indicate a forged or stale client payload, so
     *     they are rejected rather than mapped to a guess.
     */
    public AnswerMapping map(
            String questionId,
            String optionCode,
            String sourceMessageId,
            TargetEntity targetEntity,
            CareStage stage,
            Map<String, Object> existingSignals) {

        QuestionCatalog.Question question = catalog.question(questionId)
                .orElseThrow(() -> reject("TRIAGE_UNKNOWN_QUESTION",
                        "Answered question is not in the canonical catalogue"));

        if (question.options().stream().noneMatch(option -> option.optionCode().equals(optionCode))) {
            throw reject("TRIAGE_OPTION_QUESTION_MISMATCH",
                    "Chosen option does not belong to the answered question");
        }
        if (!question.targetEntities().isEmpty() && !question.targetEntities().contains(targetEntity)) {
            // An entity-agnostic danger screen stays answerable while the target is still
            // unresolved: it means the same thing for either entity, and making an emergency wait
            // for a clarification round is exactly the failure this screen exists to prevent.
            // Once the target IS resolved, a question that does not cover it is still a mismatch.
            boolean answerableWithoutTarget =
                    question.mayRunWithoutResolvedTarget() && !targetEntity.isResolved();
            if (!answerableWithoutTarget) {
                throw reject("TRIAGE_ANSWER_ENTITY_MISMATCH",
                        "Answered question does not apply to this session's target entity");
            }
        }
        if (!question.applicableStages().isEmpty() && !question.applicableStages().contains(stage)) {
            throw reject("TRIAGE_ANSWER_STAGE_MISMATCH",
                    "Answered question does not apply to this session's stage");
        }

        List<MappingEntry> entries = mappings.get(new MappingKey(questionId, optionCode));
        if (entries == null || entries.isEmpty()) {
            // Structurally valid but not yet interpretable. Record that the user answered so the
            // planner moves on, but invent nothing: an unmapped answer yields no signal at all.
            return new AnswerMapping(Status.UNSUPPORTED_MAPPING, questionId, List.of());
        }

        List<SignalMutation> mutations = new ArrayList<>();
        for (MappingEntry entry : entries) {
            mutations.add(new SignalMutation(
                    entry.signalCode(),
                    entry.presence(),
                    entry.current(),
                    Provenance.QUESTION_ANSWER,
                    sourceMessageId,
                    questionId,
                    optionCode,
                    null,
                    mappingRuleVersion,
                    conflictWith(existingSignals, entry)));
        }
        return new AnswerMapping(Status.MAPPED, questionId, List.copyOf(mutations));
    }

    /**
     * A new answer that contradicts what the session already believes is marked CONFLICTED rather
     * than overwriting it. Silently replacing a prior PRESENT with a later ABSENT is exactly how a
     * danger signal disappears between turns.
     */
    private static ConflictStatus conflictWith(Map<String, Object> existingSignals, MappingEntry entry) {
        if (existingSignals == null) {
            return ConflictStatus.NONE;
        }
        String previous = presenceOf(existingSignals.get(entry.signalCode()));
        if (previous == null || previous.equals(entry.presence())) {
            return ConflictStatus.NONE;
        }
        boolean eitherIsUnknown = previous.equals("UNKNOWN") || entry.presence().equals("UNKNOWN");
        return eitherIsUnknown ? ConflictStatus.NONE : ConflictStatus.CONFLICTED;
    }

    private static String presenceOf(Object observation) {
        if (observation instanceof String text) {
            return PRESENCE.contains(text) ? text : null;
        }
        if (observation instanceof List<?> list && !list.isEmpty()) {
            return presenceOf(list.get(list.size() - 1));
        }
        if (observation instanceof Map<?, ?> map && map.get("presence") instanceof String text) {
            return PRESENCE.contains(text) ? text : null;
        }
        return null;
    }

    private static TriageException reject(String code, String message) {
        return new TriageException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
