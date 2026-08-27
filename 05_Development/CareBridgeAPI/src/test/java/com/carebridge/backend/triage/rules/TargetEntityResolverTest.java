package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-T2 — target-entity resolution, mirroring {@code tests/test_target_entity_resolver.py}.
 *
 * <p>Deciding "mother" for a message about an infant applies maternal thresholds to a baby.
 * These tests pin the conservative behaviour: UNKNOWN rather than guess, CONFLICTED rather
 * than pick a side.
 */
class TargetEntityResolverTest {

    private static TargetEntityResolver resolver;

    @BeforeAll
    static void setUp() {
        resolver = new TargetEntityResolver();
    }

    private static TargetEntity of(String message) {
        return resolver.resolve(null, message, null, null, null).entity();
    }

    @ParameterizedTest
    @CsvSource({
            "'Tôi bị tiêu chảy',MOTHER",
            "'Em đang mang thai 30 tuần',MOTHER",
            "'Tôi sau sinh 5 ngày ra máu nhiều',MOTHER",
            "'Sản dịch có mùi hôi',MOTHER",
            "'Bé bị đi ngoài phân lỏng',BABY",
            "'Con tôi sốt cao',BABY",
            "'Bé nhà em bỏ bú',BABY",
            "'Trẻ sơ sinh bị vàng da',BABY",
    })
    @DisplayName("Unambiguous messages resolve to the right entity")
    void unambiguousMessagesResolve(String message, TargetEntity expected) {
        assertThat(of(message)).isEqualTo(expected);
    }

    @Test
    @DisplayName("A possessed child outranks the first-person pronoun that introduced it")
    void possessedChildOutranksPronoun() {
        var resolution = resolver.resolve(null, "Tôi hỏi giúp con tôi, bé bị sốt", null, null, null);
        assertThat(resolution.entity()).isEqualTo(TargetEntity.BABY);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE);
    }

    @Test
    @DisplayName("The conjunction 'còn' is not mistaken for the word 'con' (child)")
    void conjunctionIsNotMistakenForChild() {
        // "còn" folds to "con" once accents are stripped; splitting clauses on folded text
        // would cut "giúp con tôi" in half and lose the baby reference entirely.
        assertThat(of("Tôi đau bụng còn bé thì sốt")).isEqualTo(TargetEntity.CONFLICTED);
        assertThat(of("Tôi hỏi giúp con tôi")).isEqualTo(TargetEntity.BABY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tôi và bé đều bị sốt", "Cả hai mẹ con đều mệt", "Mẹ và bé cùng bị ho"})
    @DisplayName("A message naming both is CONFLICTED, never guessed")
    void multiEntityMessagesAreConflicted(String message) {
        var resolution = resolver.resolve(null, message, null, null, null);
        assertThat(resolution.entity()).isEqualTo(TargetEntity.CONFLICTED);
        assertThat(resolution.conflictEvidence())
                .as("the UI must be able to say what it saw").isNotEmpty();
    }

    @Test
    @DisplayName("Both entities across separate clauses is CONFLICTED")
    void bothEntitiesAcrossClausesIsConflicted() {
        assertThat(of("Tôi bị sốt, bé cũng bị sốt")).isEqualTo(TargetEntity.CONFLICTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Em lo quá, bé bỏ bú",
            "Bé nóng người nhưng nhà em không có nhiệt kế để đo.",
            "Em thấy bé bú kém hơn hôm qua."
    })
    @DisplayName("A narrator pronoun outside a maternal symptom clause is not a second patient")
    void narratorPronounDoesNotBecomeSecondPatient(String message) {
        assertThat(of(message)).isEqualTo(TargetEntity.BABY);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Em bị sốt và bé cũng bị sốt",
            "Tôi và bé đều bị sốt",
            "Em đang đau đầu còn bé nhà em lại nóng người",
            "Mẹ sau sinh đau đầu, con bú kém hơn hôm qua."
    })
    @DisplayName("A real maternal symptom beside a baby symptom remains CONFLICTED")
    void realMaternalAndBabySymptomsRemainConflicted(String message) {
        assertThat(of(message)).isEqualTo(TargetEntity.CONFLICTED);
    }

    @Test
    @DisplayName("A legacy indicator contract without symptom markers keeps conservative behavior")
    void missingSymptomMarkersKeepConservativeBehavior() {
        assertThat(TargetEntityResolver.clauseReportsSymptom(
                "em lo qua", new ObjectMapper().createObjectNode())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bị sốt 38.5 độ từ sáng", "Đau bụng dưới", "Chóng mặt từ hôm qua"})
    @DisplayName("An ambiguous message stays UNKNOWN rather than being guessed")
    void ambiguousMessagesStayUnknown(String message) {
        var resolution = resolver.resolve(null, message, null, null, null);
        assertThat(resolution.entity()).isEqualTo(TargetEntity.UNKNOWN);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.NONE);
    }

    @Test
    @DisplayName("A third party's symptom does not become the user's triage")
    void thirdPartyIsNotScoredAsTheUser() {
        assertThat(of("Bạn tôi bị đau bụng")).isEqualTo(TargetEntity.UNKNOWN);
    }

    // ------------------------------------------------------------------ precedence

    @Test
    @DisplayName("A clarification answer outranks everything else")
    void clarificationOutranksEverything() {
        var resolution = resolver.resolve("CLARIFY_TARGET_BABY", "Tôi sau sinh 5 ngày",
                TargetEntity.MOTHER, null, null);
        assertThat(resolution.entity()).isEqualTo(TargetEntity.BABY);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER);
    }

    @Test
    @DisplayName("Choosing 'both' becomes CONFLICTED, not a silent pick")
    void clarifyBothBecomesConflicted() {
        assertThat(resolver.resolve("CLARIFY_TARGET_BOTH", null, null, null, null).entity())
                .isEqualTo(TargetEntity.CONFLICTED);
    }

    @Test
    @DisplayName("The latest message outranks a stored profile")
    void latestMessageOutranksStoredProfile() {
        var resolution = resolver.resolve(null, "Bé nhà em bỏ bú", TargetEntity.MOTHER, null, null);
        assertThat(resolution.entity()).isEqualTo(TargetEntity.BABY);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE);
    }

    @Test
    @DisplayName("The profile is used only when the message says nothing")
    void profileUsedOnlyWhenMessageIsSilent() {
        var resolution = resolver.resolve(null, "Bị sốt 38.5 độ", TargetEntity.BABY, null, null);
        assertThat(resolution.entity()).isEqualTo(TargetEntity.BABY);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.EXPLICIT_SELECTED_PROFILE);
    }

    @Test
    @DisplayName("A confirmed conversation target ranks below the profile")
    void confirmedTargetRanksBelowProfile() {
        var resolution = resolver.resolve(null, "Vẫn còn mệt", null, TargetEntity.MOTHER, null);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.CONFIRMED_CONVERSATION_TARGET);
    }

    @ParameterizedTest
    @CsvSource({"PREGNANCY,MOTHER", "PRECONCEPTION,MOTHER", "INFANT_0_12M,BABY", "TODDLER_12_24M,BABY"})
    @DisplayName("Stage context is the weakest resolved signal")
    void stageContextIsWeakest(CareStage stage, TargetEntity expected) {
        var resolution = resolver.resolve(null, "Bị sốt", null, null, stage);
        assertThat(resolution.entity()).isEqualTo(expected);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.STAGE_SPECIFIC_CONTEXT);
    }

    @Test
    @DisplayName("Postpartum stage alone does not resolve the target")
    void postpartumStageAloneDoesNotResolve() {
        // Postpartum is exactly when a newborn question is most likely.
        assertThat(resolver.resolve(null, "Bị sốt 38.5 độ", null, null, CareStage.POSTPARTUM_MOTHER)
                .entity()).isEqualTo(TargetEntity.UNKNOWN);
    }

    @Test
    @DisplayName("Unresolved inputs are ignored rather than trusted")
    void unresolvedInputsAreIgnored() {
        assertThat(resolver.resolve(null, "Bị sốt", TargetEntity.UNKNOWN,
                TargetEntity.CONFLICTED, null).entity()).isEqualTo(TargetEntity.UNKNOWN);
    }

    @Test
    @DisplayName("Evidence is recorded so the decision is explainable")
    void evidenceIsRecorded() {
        var resolution = resolver.resolve(null, "Con tôi sốt cao", null, null, null);
        assertThat(resolution.entity()).isEqualTo(TargetEntity.BABY);
        assertThat(resolution.evidence()).isNotEmpty();
    }
}
