package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-T9 — shared context parity vectors. Python asserts the same file in
 * {@code tests/test_context_parity_vectors.py}; these are the Phase 1 gate scenarios plus the
 * accent-collision regressions found while building the resolvers.
 */
class ContextParityVectorTest {

    private static final String VECTORS_RESOURCE = "triage/context_parity_vectors_v1.json";

    private static TargetEntityResolver targetResolver;
    private static IntentResolver intentResolver;
    private static StageResolver stageResolver;
    private static ComplaintTaxonomy taxonomy;
    private static QuestionCatalogFilter filter;

    @BeforeAll
    static void setUp() {
        targetResolver = new TargetEntityResolver();
        intentResolver = new IntentResolver();
        stageResolver = new StageResolver();
        taxonomy = new ComplaintTaxonomy();
        filter = new QuestionCatalogFilter(new QuestionCatalog());
    }

    static Stream<JsonNode> vectors() throws IOException {
        try (InputStream stream = new ClassPathResource(VECTORS_RESOURCE).getInputStream()) {
            List<JsonNode> vectors = new ArrayList<>();
            new ObjectMapper().readTree(stream).get("vectors").forEach(vectors::add);
            return vectors.stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vectors")
    @DisplayName("Java resolves the shared context vector identically to Python")
    void javaMatchesSharedContextVector(JsonNode vector) {
        JsonNode input = vector.get("input");
        JsonNode expected = vector.get("expected");
        String label = vector.path("id").asText() + ": " + vector.path("description").asText();
        String message = input.path("message").asText(null);

        var target = targetResolver.resolve(null, message, null, null, null);

        List<String> optionCodes = new ArrayList<>();
        input.path("optionCodes").forEach(node -> optionCodes.add(node.asText()));
        var intent = intentResolver.resolve(message, optionCodes, null);

        CareStage explicit = input.hasNonNull("explicitStage")
                ? CareStage.valueOf(input.get("explicitStage").asText()) : null;
        var stage = stageResolver.resolve(
                target.entity(), explicit, input.path("legacyStage").asText(null), null,
                input.hasNonNull("babyAgeMonths") ? input.get("babyAgeMonths").asInt() : null,
                null, null);

        var status = stageResolver.resolveContextStatus(
                target.entity(), stage.stage(), intent.intent(), stage.conflicts());

        if (expected.hasNonNull("targetEntity")) {
            assertThat(target.entity()).as(label)
                    .isEqualTo(TargetEntity.valueOf(expected.get("targetEntity").asText()));
        }
        if (expected.hasNonNull("intent")) {
            assertThat(intent.intent()).as(label)
                    .isEqualTo(IntentType.valueOf(expected.get("intent").asText()));
        }
        if (expected.hasNonNull("stage")) {
            assertThat(stage.stage()).as(label)
                    .isEqualTo(CareStage.valueOf(expected.get("stage").asText()));
        }
        if (expected.hasNonNull("contextStatus")) {
            assertThat(status.status()).as(label).isEqualTo(
                    ContextResolutionStatus.valueOf(expected.get("contextStatus").asText()));
        }
        if (expected.hasNonNull("mayProduceTriageOutcome")) {
            assertThat(intent.mayProduceTriageOutcome()).as(label)
                    .isEqualTo(expected.get("mayProduceTriageOutcome").asBoolean());
        }
        if (expected.hasNonNull("complaintCategory")) {
            assertThat(taxonomy.classify(message, Map.of()).categoryId()).as(label)
                    .isEqualTo(expected.get("complaintCategory").asText());
        }

        if (expected.path("onlyClarificationQuestions").asBoolean(false)) {
            var context = QuestionCatalogFilter.FilterContext.of(
                    target.entity(), stage.stage(), intent.intent(), status.status(),
                    Set.of("bleeding_amount", "gestational_week"), Set.of("VAGINAL_BLEEDING"));
            var questions = filter.eligibleQuestions(context, null);
            assertThat(questions).as(label).isNotEmpty();
            assertThat(questions).as(label)
                    .anyMatch(QuestionCatalog.Question::isClarification)
                    .anyMatch(QuestionCatalog.Question::isGlobalDangerScreen);
            for (var question : questions) {
                assertThat(question.isClarification() || question.isGlobalDangerScreen())
                        .as("%s -> %s", label, question.questionId()).isTrue();
            }
        }
    }

    @Test
    @DisplayName("Every shared context vector is executed")
    void everyVectorIsExecuted() throws IOException {
        assertThat(vectors().count()).isEqualTo(14);
    }
}
