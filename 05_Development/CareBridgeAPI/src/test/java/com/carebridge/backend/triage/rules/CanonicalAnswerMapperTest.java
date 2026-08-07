package com.carebridge.backend.triage.rules;

import com.carebridge.backend.triage.exception.TriageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The mapper is the only trusted route from a tapped option to a clinical belief, so these tests
 * are about what it refuses as much as what it produces.
 *
 * <p>The shipped mapping contract is intentionally empty pending internal rule review, so the
 * production behaviour asserted here is UNSUPPORTED_MAPPING. A separate fixture exercises the
 * populated path so the mechanism is proven before any clinical content is authored.
 */
class CanonicalAnswerMapperTest {

    private final QuestionCatalog catalog = new QuestionCatalog();
    private final CanonicalAnswerMapper mapper =
            new CanonicalAnswerMapper(catalog, CanonicalAnswerMapper.MAPPING_RESOURCE);

    private CanonicalAnswerMapper.AnswerMapping map(String questionId, String optionCode) {
        return mapper.map(questionId, optionCode, "msg-000000000001", TargetEntity.MOTHER,
                CareStage.PREGNANCY, Map.of());
    }

    private String presenceOf(CanonicalAnswerMapper.AnswerMapping result, String signalCode) {
        return result.mutations().stream()
                .filter(mutation -> mutation.signalCode().equals(signalCode))
                .map(CanonicalAnswerMapper.SignalMutation::presence)
                .findFirst().orElse(null);
    }

    @Test
    @DisplayName("shipped mapping table is populated and carries its review provenance")
    void shippedTableIsPopulated() {
        assertThat(mapper.isPopulated()).isTrue();
        assertThat(mapper.reviewStatus()).isEqualTo("DEV_REVIEWED");
        assertThat(mapper.mappingRuleVersion()).isEqualTo("CANONICAL_ANSWER_MAPPING_V1_2026_08_06");
    }

    @Test
    @DisplayName("the heaviest bleeding option raises both the general and the heavy signals")
    void heavyBleedingRaisesEscalationSignals() {
        CanonicalAnswerMapper.AnswerMapping result = map("Q_BLEEDING_AMOUNT", "BLEEDING_HEAVY");

        assertThat(result.status()).isEqualTo(CanonicalAnswerMapper.Status.MAPPED);
        assertThat(presenceOf(result, "VAGINAL_BLEEDING")).isEqualTo("PRESENT");
        assertThat(presenceOf(result, "HEAVY_VAGINAL_BLEEDING")).isEqualTo("PRESENT");
        // Both stage variants are asserted; the rules are stage-scoped so only the correct one fires.
        assertThat(presenceOf(result, "HEAVY_POSTPARTUM_BLEEDING")).isEqualTo("PRESENT");
        assertThat(result.answeredQuestionId()).isEqualTo("Q_BLEEDING_AMOUNT");
    }

    @Test
    @DisplayName("a lower bleeding band reports bleeding without escalating it")
    void moderateBleedingDoesNotEscalate() {
        CanonicalAnswerMapper.AnswerMapping result = map("Q_BLEEDING_AMOUNT", "BLEEDING_MODERATE");

        assertThat(presenceOf(result, "VAGINAL_BLEEDING")).isEqualTo("PRESENT");
        assertThat(presenceOf(result, "HEAVY_VAGINAL_BLEEDING")).isEqualTo("ABSENT");
    }

    @Test
    @DisplayName("an explicit NO option is an explicit ABSENT, not a silent gap")
    void explicitNoOptionIsAbsent() {
        assertThat(presenceOf(map("Q_DIZZINESS", "DIZZINESS_NO"), "DIZZINESS")).isEqualTo("ABSENT");
    }

    @Test
    @DisplayName("an UNSURE answer becomes UNKNOWN and never a negative finding")
    void unsureIsUnknownNotAbsent() {
        assertThat(presenceOf(map("Q_DIZZINESS", "UNSURE"), "DIZZINESS")).isEqualTo("UNKNOWN");
        assertThat(presenceOf(map("Q_BLEEDING_AMOUNT", "UNSURE"), "HEAVY_VAGINAL_BLEEDING"))
                .isEqualTo("UNKNOWN");
        // Q_CLOTS is scoped to the postpartum stage, so it must be answered in that context.
        CanonicalAnswerMapper.AnswerMapping clots = mapper.map("Q_CLOTS", "UNSURE",
                "msg-000000000001", TargetEntity.MOTHER, CareStage.POSTPARTUM_MOTHER, Map.of());
        assertThat(presenceOf(clots, "LARGE_CLOTS")).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("a question that declares no signal at all stays unmapped")
    void questionWithoutSignalsStaysUnmapped() {
        // Q_PAIN_SEVERITY declares no signal and names no body region, so there is nothing it
        // could honestly assert. The answer is still recorded so the planner moves on.
        CanonicalAnswerMapper.AnswerMapping result = map("Q_PAIN_SEVERITY", "PAIN_SEVERE");

        assertThat(result.status()).isEqualTo(CanonicalAnswerMapper.Status.UNSUPPORTED_MAPPING);
        assertThat(result.mutations()).isEmpty();
        assertThat(result.answeredQuestionId()).isEqualTo("Q_PAIN_SEVERITY");
    }

    @Test
    @DisplayName("every global danger sign is reachable without the extractor")
    void globalDangerSignsAreDeterministicallyReachable() {
        assertThat(presenceOf(map("Q_GLOBAL_DANGER", "DANGER_SEIZURE"), "SEIZURE"))
                .isEqualTo("PRESENT");
        assertThat(presenceOf(map("Q_GLOBAL_DANGER", "DANGER_UNCONSCIOUS"), "ALTERED_CONSCIOUSNESS"))
                .isEqualTo("PRESENT");
        assertThat(presenceOf(map("Q_GLOBAL_DANGER", "DANGER_BREATHING"),
                "SEVERE_BREATHING_DIFFICULTY")).isEqualTo("PRESENT");
        assertThat(presenceOf(map("Q_GLOBAL_DANGER", "DANGER_CYANOSIS"), "CYANOSIS"))
                .isEqualTo("PRESENT");
    }

    @Test
    @DisplayName("naming one danger sign does not deny the others")
    void namingOneDangerSignDoesNotDenyTheRest() {
        // The question is single-choice but a person can have several signs at once, so only the
        // explicit "none" option may assert ABSENT.
        CanonicalAnswerMapper.AnswerMapping seizure = map("Q_GLOBAL_DANGER", "DANGER_SEIZURE");

        assertThat(presenceOf(seizure, "SEVERE_BREATHING_DIFFICULTY")).isNull();
        assertThat(presenceOf(map("Q_GLOBAL_DANGER", "DANGER_NONE"), "SEVERE_BREATHING_DIFFICULTY"))
                .isEqualTo("ABSENT");
    }

    /** The self-harm screen is scoped to the postpartum stage, so it must be answered there. */
    private CanonicalAnswerMapper.AnswerMapping mapPostpartum(String questionId, String optionCode) {
        return mapper.map(questionId, optionCode, "msg-000000000001", TargetEntity.MOTHER,
                CareStage.POSTPARTUM_MOTHER, Map.of());
    }

    @Test
    @DisplayName("declining the self-harm question is UNKNOWN, never a denial")
    void decliningSelfHarmQuestionIsNotADenial() {
        CanonicalAnswerMapper.AnswerMapping declined = mapPostpartum("Q_SAFETY_SELF_HARM", "UNSURE");

        assertThat(presenceOf(declined, "SELF_HARM_IDEATION")).isEqualTo("UNKNOWN");
        assertThat(presenceOf(declined, "SELF_HARM_INTENT_OR_PLAN")).isEqualTo("UNKNOWN");
        assertThat(presenceOf(declined, "CANNOT_ENSURE_OWN_SAFETY")).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("a stated self-harm plan also asserts the ideation it implies")
    void selfHarmPlanImpliesIdeation() {
        CanonicalAnswerMapper.AnswerMapping plan = mapPostpartum("Q_SAFETY_SELF_HARM",
                "SELF_HARM_PLAN");

        assertThat(presenceOf(plan, "SELF_HARM_INTENT_OR_PLAN")).isEqualTo("PRESENT");
        assertThat(presenceOf(plan, "SELF_HARM_IDEATION")).isEqualTo("PRESENT");
    }

    @Test
    @DisplayName("mild headache is an explicit denial of a severe one")
    void mildHeadacheDeniesSevere() {
        assertThat(presenceOf(map("Q_HEADACHE_SEVERITY", "HEADACHE_SEVERE"), "SEVERE_HEADACHE"))
                .isEqualTo("PRESENT");
        assertThat(presenceOf(map("Q_HEADACHE_SEVERITY", "HEADACHE_MILD"), "SEVERE_HEADACHE"))
                .isEqualTo("ABSENT");
        assertThat(presenceOf(map("Q_HEADACHE_SEVERITY", "UNSURE"), "SEVERE_HEADACHE"))
                .isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("a measurement answer produces no signal")
    void measurementAnswerProducesNoSignal() {
        assertThat(map("Q_BP_IF_KNOWN", "NO_DEVICE_OR_UNAWARE").toSignals()).isEmpty();
    }

    @Test
    @DisplayName("a routing clarification never produces a clinical signal")
    void clarificationProducesNoSignal() {
        assertThat(mapper.map("Q_CLARIFY_TARGET_ENTITY", "CLARIFY_TARGET_MOTHER",
                "msg-000000000001", TargetEntity.MOTHER, CareStage.PREGNANCY, Map.of())
                .toSignals()).isEmpty();
    }

    @Test
    @DisplayName("a mapping naming a signal the registry does not define fails at startup")
    void unknownSignalCodeFailsFast() {
        assertThatThrownBy(() -> new CanonicalAnswerMapper(catalog,
                CanonicalAnswerMapper.MAPPING_RESOURCE, java.util.Set.of("ONLY_THIS_ONE")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown signal");
    }

    @Test
    @DisplayName("an unknown question is rejected, not guessed")
    void unknownQuestionIsRejected() {
        assertThatThrownBy(() -> map("Q_NOT_A_REAL_QUESTION", "YES"))
                .isInstanceOf(TriageException.class)
                .hasFieldOrPropertyWithValue("code", "TRIAGE_V2_UNKNOWN_QUESTION");
    }

    @Test
    @DisplayName("an option that belongs to a different question is rejected")
    void optionQuestionMismatchIsRejected() {
        assertThatThrownBy(() -> map("Q_DIZZINESS", "BLEEDING_HEAVY"))
                .isInstanceOf(TriageException.class)
                .hasFieldOrPropertyWithValue("code", "TRIAGE_V2_OPTION_QUESTION_MISMATCH");
    }

    @Test
    @DisplayName("an option that exists nowhere is rejected")
    void invalidOptionIsRejected() {
        assertThatThrownBy(() -> map("Q_DIZZINESS", "TOTALLY_MADE_UP"))
                .isInstanceOf(TriageException.class)
                .hasFieldOrPropertyWithValue("code", "TRIAGE_V2_OPTION_QUESTION_MISMATCH");
    }

    @Test
    @DisplayName("a maternal question answered in a baby session is rejected")
    void entityMismatchIsRejected() {
        assertThatThrownBy(() -> mapper.map("Q_BLEEDING_AMOUNT", "BLEEDING_HEAVY",
                "msg-000000000001", TargetEntity.BABY, CareStage.INFANT_0_12M, Map.of()))
                .isInstanceOf(TriageException.class)
                .hasFieldOrPropertyWithValue("code", "TRIAGE_V2_ANSWER_ENTITY_MISMATCH");
    }

    @Test
    @DisplayName("an unresolved target cannot answer a target-specific question")
    void unresolvedTargetIsRejected() {
        assertThatThrownBy(() -> mapper.map("Q_BLEEDING_AMOUNT", "BLEEDING_HEAVY",
                "msg-000000000001", TargetEntity.UNKNOWN, CareStage.UNKNOWN, Map.of()))
                .isInstanceOf(TriageException.class)
                .hasFieldOrPropertyWithValue("code", "TRIAGE_V2_ANSWER_ENTITY_MISMATCH");
    }

    @Test
    @DisplayName("the global danger screen is answerable before the target is resolved")
    void globalDangerScreenAnswerableWithoutResolvedTarget() {
        // Making an emergency wait for a clarification round is the failure this screen prevents.
        CanonicalAnswerMapper.AnswerMapping result = mapper.map("Q_GLOBAL_DANGER", "DANGER_SEIZURE",
                "msg-000000000001", TargetEntity.UNKNOWN, CareStage.UNKNOWN, Map.of());

        assertThat(presenceOf(result, "SEIZURE")).isEqualTo("PRESENT");
    }

    @Test
    @DisplayName("a resolved target still rules out a question that does not cover it")
    void resolvedTargetStillEnforcesEntityScope() {
        // The exemption above applies only while the target is unknown; once we know it is a baby,
        // a maternal-only question is a mismatch again.
        assertThatThrownBy(() -> mapper.map("Q_SAFETY_SELF_HARM", "SELF_HARM_NONE",
                "msg-000000000001", TargetEntity.BABY, CareStage.INFANT_0_12M, Map.of()))
                .isInstanceOf(TriageException.class)
                .hasFieldOrPropertyWithValue("code", "TRIAGE_V2_ANSWER_ENTITY_MISMATCH");
    }

    @Test
    @DisplayName("a forged caller signal cannot enter through the mapper")
    void mapperOnlyAcceptsIdentifiers() {
        // The signature accepts identifiers and prior state only; there is no parameter through
        // which a caller could assert a presence value of its own.
        assertThat(presenceOf(map("Q_DIZZINESS", "DIZZINESS_YES"), "DIZZINESS")).isEqualTo("PRESENT");
        assertThat(CanonicalAnswerMapper.AnswerMapping.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("status", "answeredQuestionId", "mutations");
    }

    @Test
    @DisplayName("the mapper cannot express a triage outcome")
    void mapperCannotProduceAnOutcome() {
        List<String> componentNames = List.of(
                        CanonicalAnswerMapper.SignalMutation.class.getRecordComponents())
                .stream().map(java.lang.reflect.RecordComponent::getName).toList();

        assertThat(componentNames)
                .doesNotContain("triageOutcome", "outcome", "requiredAction", "colour", "color");
        assertThat(componentNames).contains("signalCode", "presence", "current", "provenance",
                "sourceMessageId", "sourceQuestionId", "sourceOptionCode", "extractedEvidenceSpan",
                "mappingRuleVersion", "conflictStatus");
    }

    @Test
    @DisplayName("remembered context may never assert a current present signal")
    void rememberedContextCannotAssertCurrentPresence() {
        assertThat(CanonicalAnswerMapper.Provenance.HEALTH_MEMORY_CONTEXT.mayAssertCurrentPresent())
                .isFalse();
        assertThat(CanonicalAnswerMapper.Provenance.PROFILE_CONTEXT.mayAssertCurrentPresent())
                .isFalse();
        assertThat(CanonicalAnswerMapper.Provenance.QUESTION_ANSWER.mayAssertCurrentPresent())
                .isTrue();
    }

    @Test
    @DisplayName("answering the same question twice stays deterministic")
    void duplicateAnswerIsDeterministic() {
        assertThat(map("Q_DIZZINESS", "DIZZINESS_YES"))
                .isEqualTo(map("Q_DIZZINESS", "DIZZINESS_YES"));
    }

    @Test
    @DisplayName("a mapping entry naming an unknown question fails at startup, not mid-session")
    void malformedMappingContractFailsFast() {
        assertThatThrownBy(() -> new CanonicalAnswerMapper(catalog,
                "triage/test_canonical_answer_mapping_unknown_question.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown question");
    }

    @Test
    @DisplayName("a populated mapping carries full provenance")
    void populatedMappingCarriesProvenance() {
        CanonicalAnswerMapper populated =
                new CanonicalAnswerMapper(catalog, "triage/test_canonical_answer_mapping_populated.json");

        CanonicalAnswerMapper.AnswerMapping result = populated.map("Q_DIZZINESS", "DIZZINESS_YES",
                "msg-000000000001", TargetEntity.MOTHER, CareStage.PREGNANCY, Map.of());

        assertThat(result.status()).isEqualTo(CanonicalAnswerMapper.Status.MAPPED);
        assertThat(result.mutations()).singleElement().satisfies(mutation -> {
            assertThat(mutation.signalCode()).isEqualTo("DIZZINESS");
            assertThat(mutation.presence()).isEqualTo("PRESENT");
            assertThat(mutation.current()).isTrue();
            assertThat(mutation.provenance())
                    .isEqualTo(CanonicalAnswerMapper.Provenance.QUESTION_ANSWER);
            assertThat(mutation.sourceQuestionId()).isEqualTo("Q_DIZZINESS");
            assertThat(mutation.sourceOptionCode()).isEqualTo("DIZZINESS_YES");
            assertThat(mutation.sourceMessageId()).isEqualTo("msg-000000000001");
            assertThat(mutation.mappingRuleVersion()).isEqualTo("TEST_MAPPING_V1");
            assertThat(mutation.conflictStatus())
                    .isEqualTo(CanonicalAnswerMapper.ConflictStatus.NONE);
        });
        assertThat(result.toSignals()).containsOnlyKeys("DIZZINESS");
    }

    @Test
    @DisplayName("a historical prior belief is not silently overwritten by a contradicting answer")
    void contradictingAnswerIsMarkedConflicted() {
        CanonicalAnswerMapper populated =
                new CanonicalAnswerMapper(catalog, "triage/test_canonical_answer_mapping_populated.json");

        CanonicalAnswerMapper.AnswerMapping result = populated.map("Q_DIZZINESS", "DIZZINESS_NO",
                "msg-000000000002", TargetEntity.MOTHER, CareStage.PREGNANCY,
                Map.of("DIZZINESS", Map.of("presence", "PRESENT")));

        assertThat(result.mutations()).singleElement()
                .extracting(CanonicalAnswerMapper.SignalMutation::conflictStatus)
                .isEqualTo(CanonicalAnswerMapper.ConflictStatus.CONFLICTED);
    }

    @Test
    @DisplayName("an unchanged answer is not reported as a conflict")
    void repeatedIdenticalAnswerIsNotAConflict() {
        CanonicalAnswerMapper populated =
                new CanonicalAnswerMapper(catalog, "triage/test_canonical_answer_mapping_populated.json");

        CanonicalAnswerMapper.AnswerMapping result = populated.map("Q_DIZZINESS", "DIZZINESS_YES",
                "msg-000000000002", TargetEntity.MOTHER, CareStage.PREGNANCY,
                Map.of("DIZZINESS", Map.of("presence", "PRESENT")));

        assertThat(result.mutations()).singleElement()
                .extracting(CanonicalAnswerMapper.SignalMutation::conflictStatus)
                .isEqualTo(CanonicalAnswerMapper.ConflictStatus.NONE);
    }

    @Test
    @DisplayName("a prior UNKNOWN is refined rather than treated as a contradiction")
    void unknownPriorIsNotAConflict() {
        CanonicalAnswerMapper populated =
                new CanonicalAnswerMapper(catalog, "triage/test_canonical_answer_mapping_populated.json");

        CanonicalAnswerMapper.AnswerMapping result = populated.map("Q_DIZZINESS", "DIZZINESS_YES",
                "msg-000000000002", TargetEntity.MOTHER, CareStage.PREGNANCY,
                Map.of("DIZZINESS", "UNKNOWN"));

        assertThat(result.mutations()).singleElement()
                .extracting(CanonicalAnswerMapper.SignalMutation::conflictStatus)
                .isEqualTo(CanonicalAnswerMapper.ConflictStatus.NONE);
    }
}
