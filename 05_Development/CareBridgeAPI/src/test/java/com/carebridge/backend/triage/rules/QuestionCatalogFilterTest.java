package com.carebridge.backend.triage.rules;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-T6b — the hard question filter, mirroring {@code tests/test_question_catalog_filter.py}.
 *
 * <p>Invariant 12: the planner must never ask a question of the wrong entity.
 */
class QuestionCatalogFilterTest {

    private static QuestionCatalog catalog;
    private static QuestionCatalogFilter filter;

    @BeforeAll
    static void setUp() {
        catalog = new QuestionCatalog();
        filter = new QuestionCatalogFilter(catalog);
    }

    private static QuestionCatalogFilter.FilterContext resolved() {
        return QuestionCatalogFilter.FilterContext.of(
                TargetEntity.MOTHER, CareStage.PREGNANCY, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.RESOLVED,
                Set.of("bleeding_amount", "pain_severity", "gestational_week"),
                Set.of("VAGINAL_BLEEDING", "VISUAL_DISTURBANCE"));
    }

    private static List<String> ids(List<QuestionCatalog.Question> questions) {
        return questions.stream().map(QuestionCatalog.Question::questionId).toList();
    }

    private static QuestionCatalog.Question q(String id) {
        return catalog.question(id).orElseThrow();
    }

    // --------------------------------------------------------- entity enforcement

    @Test
    @DisplayName("A baby question is never offered to a mother")
    void babyQuestionNotOfferedToMother() {
        assertThat(filter.isEligible(q("Q_BABY_AGE_MONTHS"), resolved())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Q_GESTATIONAL_WEEK", "Q_BLEEDING_AMOUNT", "Q_POSTPARTUM_DAY"})
    @DisplayName("A mother question is never offered for a baby")
    void motherQuestionNotOfferedForBaby(String questionId) {
        var context = QuestionCatalogFilter.FilterContext.of(
                TargetEntity.BABY, CareStage.INFANT_0_12M, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.RESOLVED,
                Set.of("gestational_week", "bleeding_amount"), Set.of("VAGINAL_BLEEDING"));
        assertThat(filter.isEligible(q(questionId), context)).isFalse();
    }

    @Test
    @DisplayName("No maternal question survives a baby context")
    void noMaternalQuestionSurvivesBabyContext() {
        var context = QuestionCatalogFilter.FilterContext.of(
                TargetEntity.BABY, CareStage.INFANT_0_12M, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.RESOLVED,
                Set.of("gestational_week", "bleeding_amount", "pain_severity"),
                Set.of("VAGINAL_BLEEDING"));
        for (var question : filter.eligibleQuestions(context, null)) {
            assertThat(question.targetEntities())
                    .as(question.questionId()).contains(TargetEntity.BABY);
        }
    }

    // ------------------------------------------------ unresolved context lockdown

    @Test
    @DisplayName("An unknown target yields only the target clarification")
    void unknownTargetYieldsOnlyClarification() {
        var context = QuestionCatalogFilter.FilterContext.of(
                TargetEntity.UNKNOWN, CareStage.UNKNOWN, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.NEEDS_TARGET_ENTITY, Set.of("bleeding_amount"), Set.of());
        assertThat(ids(filter.eligibleQuestions(context, null)))
                .containsExactly("Q_CLARIFY_TARGET_ENTITY");
    }

    @Test
    @DisplayName("A conflicted target asks which to assess first")
    void conflictedTargetAsksWhichFirst() {
        var context = QuestionCatalogFilter.FilterContext.of(
                TargetEntity.CONFLICTED, CareStage.UNKNOWN, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.CONFLICTED, Set.of(), Set.of());
        assertThat(ids(filter.eligibleQuestions(context, null)))
                .containsExactly("Q_CLARIFY_TARGET_FIRST");
    }

    @Test
    @DisplayName("An unknown intent asks about intent")
    void unknownIntentAsksAboutIntent() {
        var context = QuestionCatalogFilter.FilterContext.of(
                TargetEntity.MOTHER, CareStage.PREGNANCY, IntentType.UNKNOWN,
                ContextResolutionStatus.NEEDS_INTENT, Set.of(), Set.of());
        assertThat(ids(filter.eligibleQuestions(context, null)))
                .containsExactly("Q_CLARIFY_INTENT");
    }

    @Test
    @DisplayName("A caller-supplied candidate list cannot bypass the lockdown")
    void callerCandidatesCannotBypassLockdown() {
        var context = QuestionCatalogFilter.FilterContext.of(
                TargetEntity.UNKNOWN, CareStage.UNKNOWN, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.NEEDS_TARGET_ENTITY, Set.of("bleeding_amount"), Set.of());
        assertThat(ids(filter.eligibleQuestions(context,
                List.of("Q_BLEEDING_AMOUNT", "Q_BABY_AGE_MONTHS"))))
                .containsExactly("Q_CLARIFY_TARGET_ENTITY");
    }

    // -------------------------------------------------------------- stage matching

    @Test
    @DisplayName("A stage-scoped question is skipped outside its stage and offered inside it")
    void stageScopedQuestionRespectsStage() {
        var pregnancy = QuestionCatalogFilter.FilterContext.of(
                TargetEntity.MOTHER, CareStage.PREGNANCY, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.RESOLVED, Set.of("postpartum_day"), Set.of());
        assertThat(filter.isEligible(q("Q_POSTPARTUM_DAY"), pregnancy)).isFalse();

        var postpartum = QuestionCatalogFilter.FilterContext.of(
                TargetEntity.MOTHER, CareStage.POSTPARTUM_MOTHER, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.RESOLVED, Set.of("postpartum_day"), Set.of());
        assertThat(filter.isEligible(q("Q_POSTPARTUM_DAY"), postpartum)).isTrue();
    }

    // ------------------------------------------------------------------ usefulness

    @Test
    @DisplayName("A question that resolves nothing missing is not asked")
    void uselessQuestionIsNotAsked() {
        var context = QuestionCatalogFilter.FilterContext.of(
                TargetEntity.MOTHER, CareStage.PREGNANCY, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.RESOLVED, Set.of(), Set.of());
        assertThat(filter.eligibleQuestions(context, null)).isEmpty();
    }

    @Test
    @DisplayName("An already-answered question is not repeated")
    void answeredQuestionNotRepeated() {
        var base = resolved();
        var context = new QuestionCatalogFilter.FilterContext(
                base.targetEntity(), base.stage(), base.intent(), base.contextStatus(),
                base.missingFields(), base.missingSignals(),
                Set.of("Q_BLEEDING_AMOUNT"), Map.of());
        assertThat(ids(filter.eligibleQuestions(context, null)))
                .doesNotContain("Q_BLEEDING_AMOUNT");
    }

    @Test
    @DisplayName("An unmeasurable question is not re-asked, but a measurable one is offered")
    void unmeasurableQuestionIsNotReAsked() {
        var unmeasurable = new QuestionCatalogFilter.FilterContext(
                TargetEntity.MOTHER, CareStage.PREGNANCY, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.RESOLVED, Set.of("blood_pressure"), Set.of(),
                Set.of(), Map.of("blood_pressure", "UNAWARE_OR_UNMEASURABLE"));
        assertThat(filter.isEligible(q("Q_BP_IF_KNOWN"), unmeasurable)).isFalse();

        var answerable = QuestionCatalogFilter.FilterContext.of(
                TargetEntity.MOTHER, CareStage.PREGNANCY, IntentType.SYMPTOM_TRIAGE,
                ContextResolutionStatus.RESOLVED, Set.of("blood_pressure"), Set.of());
        assertThat(filter.isEligible(q("Q_BP_IF_KNOWN"), answerable)).isTrue();
    }

    // -------------------------------------------------------------------- ordering

    @Test
    @DisplayName("Escalating questions are offered before routine ones")
    void escalatingQuestionsComeFirst() {
        var ordered = ids(filter.eligibleQuestions(resolved(), null));
        assertThat(ordered.indexOf("Q_VISUAL_CHANGE"))
                .isLessThan(ordered.indexOf("Q_PAIN_SEVERITY"));
    }

    // ------------------------------------------------------------ catalogue shape

    @Test
    @DisplayName("The Java catalogue matches the canonical contract")
    void catalogueMatchesCanonicalContract() {
        // 15 original questions plus the deterministic global-danger, self-harm and headache
        // screens, without which those signals were reachable only by free-text extraction.
        assertThat(catalog.questions()).hasSize(18);
        assertThat(catalog.maxQuestionsPerTurn()).isEqualTo(3);
        assertThat(catalog.maxRounds()).isEqualTo(3);
    }

    @Test
    @DisplayName("Every question declares an entity, and only clarifications skip the target")
    void catalogueShapeInvariants() {
        for (var question : catalog.questions().values()) {
            assertThat(question.targetEntities()).as(question.questionId()).isNotEmpty();
            if (!question.requiresResolvedTarget()) {
                // A clarification has no entity yet and an entity-agnostic danger screen means the
                // same thing for either entity; neither can be a wrong-entity question.
                assertThat(question.mayRunWithoutResolvedTarget())
                        .as(question.questionId()).isTrue();
            }
            if (question.isGlobalDangerScreen()) {
                // The exemption is only sound while the screen genuinely applies to both entities.
                assertThat(question.targetEntities()).as(question.questionId())
                        .containsExactlyInAnyOrder(TargetEntity.MOTHER, TargetEntity.BABY);
                assertThat(question.applicableStages()).as(question.questionId()).isEmpty();
            }
            for (var option : question.options()) {
                assertThat(option.optionCode())
                        .as(question.questionId())
                        .isEqualTo(option.optionCode().toUpperCase(java.util.Locale.ROOT));
                assertThat(option.displayText()).isNotBlank();
            }
        }
    }
}
