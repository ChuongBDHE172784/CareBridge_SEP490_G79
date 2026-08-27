package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The fixed clinical question catalogue, loaded from the canonical contract.
 *
 * <p>The medical content of every question is frozen in
 * {@code Contracts/triage/question_catalog_v1.json}. An LLM may later rephrase for
 * readability but can never invent a question, change what one asks, or add an option.
 * Business logic keys off {@code optionCode}/{@code questionId}, never display text.
 *
 * <p>Loaded from a shared file rather than hand-written twice: two runtimes with two
 * catalogues would eventually ask different questions of the same patient.
 */
@Component
public class QuestionCatalog {

    public static final String CATALOG_RESOURCE = "triage/question_catalog_v1.json";

    public record Option(String optionCode, String displayText) {
    }

    public record Question(
            String questionId,
            String text,
            String answerType,
            List<Option> options,
            List<String> resolvesSignals,
            List<String> resolvesFields,
            boolean measurement,
            List<String> pivotTo,
            List<TargetEntity> targetEntities,
            List<CareStage> applicableStages,
            List<IntentType> applicableIntents,
            int priority,
            List<String> escalationSignals,
            boolean requiresResolvedTarget,
            boolean requiresResolvedStage,
            boolean isTargetClarification,
            boolean isStageClarification,
            boolean isIntentClarification,
            /**
             * An entity-agnostic danger screen. Seizure, altered consciousness, severe breathing
             * difficulty and cyanosis mean the same thing for a mother and for a baby, so this
             * question cannot be asked of the "wrong" entity and may run before the target is
             * resolved — an emergency must not wait for a clarification round.
             */
            boolean isGlobalDangerScreen) {

        public boolean isClarification() {
            return isTargetClarification || isStageClarification || isIntentClarification;
        }

        /** Questions allowed to run before the target entity is known. */
        public boolean mayRunWithoutResolvedTarget() {
            return isClarification() || isGlobalDangerScreen;
        }
    }

    private final Map<String, Question> questions;
    private final int maxQuestionsPerTurn;
    private final int maxRounds;

    public QuestionCatalog() {
        this(CATALOG_RESOURCE);
    }

    QuestionCatalog(String resourcePath) {
        JsonNode document;
        try (InputStream stream = new ClassPathResource(resourcePath).getInputStream()) {
            document = new ObjectMapper().readTree(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read question catalogue " + resourcePath, exception);
        }

        Map<String, Question> loaded = new LinkedHashMap<>();
        for (JsonNode payload : document.get("questions")) {
            Question question = toQuestion(payload);
            loaded.put(question.questionId(), question);
        }
        this.questions = Map.copyOf(loaded);
        this.maxQuestionsPerTurn = document.path("maxQuestionsPerTurn").asInt(3);
        this.maxRounds = document.path("maxRounds").asInt(3);
    }

    private static Question toQuestion(JsonNode payload) {
        List<Option> options = new ArrayList<>();
        payload.path("options").forEach(node ->
                options.add(new Option(node.get("optionCode").asText(),
                        node.get("displayText").asText())));

        return new Question(
                payload.get("questionId").asText(),
                payload.get("text").asText(),
                payload.get("answerType").asText(),
                List.copyOf(options),
                textList(payload.path("resolvesSignals")),
                textList(payload.path("resolvesFields")),
                payload.path("measurement").asBoolean(false),
                textList(payload.path("pivotTo")),
                enumList(payload.path("targetEntities"), TargetEntity::valueOf),
                enumList(payload.path("applicableStages"), CareStage::valueOf),
                enumList(payload.path("applicableIntents"), IntentType::valueOf),
                payload.path("priority").asInt(50),
                textList(payload.path("escalationSignals")),
                payload.path("requiresResolvedTarget").asBoolean(true),
                payload.path("requiresResolvedStage").asBoolean(false),
                payload.path("isTargetClarification").asBoolean(false),
                payload.path("isStageClarification").asBoolean(false),
                payload.path("isIntentClarification").asBoolean(false),
                payload.path("isGlobalDangerScreen").asBoolean(false));
    }

    private static List<String> textList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(element -> values.add(element.asText()));
        }
        return List.copyOf(values);
    }

    private static <T> List<T> enumList(JsonNode node, java.util.function.Function<String, T> parser) {
        List<T> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(element -> values.add(parser.apply(element.asText())));
        }
        return List.copyOf(values);
    }

    public Optional<Question> question(String questionId) {
        return Optional.ofNullable(questions.get(questionId));
    }

    public Map<String, Question> questions() {
        return questions;
    }

    public int maxQuestionsPerTurn() {
        return maxQuestionsPerTurn;
    }

    public int maxRounds() {
        return maxRounds;
    }
}
