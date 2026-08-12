package com.carebridge.backend.triage.rules;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-T3 — intent resolution, mirroring {@code tests/test_intent_resolver.py}.
 *
 * <p>Intent decides whether a colour may be produced at all.
 */
class IntentResolverTest {

    private static IntentResolver resolver;

    @BeforeAll
    static void setUp() {
        resolver = new IntentResolver();
    }

    private static IntentType intentOf(String message) {
        return resolver.resolve(message, null, null).intent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Nguồn này từ đâu vậy?", "Tài liệu tham khảo là gì", "Có đáng tin không?"})
    @DisplayName("Questions about provenance are SOURCE_LOOKUP")
    void sourceQuestionsAreSourceLookup(String message) {
        assertThat(intentOf(message)).isEqualTo(IntentType.SOURCE_LOOKUP);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Dấu hiệu cảnh báo thai kỳ là gì?",
            "Khi nào cần đi khám thai?",
            "Tiền sản giật là sao?",
            "Nên ăn gì khi mang thai?"})
    @DisplayName("General questions are never triaged")
    void generalQuestionsAreNotTriaged(String message) {
        assertThat(intentOf(message)).isEqualTo(IntentType.GENERAL_HEALTH_INFORMATION);
    }

    @Test
    @DisplayName("'dấu' (sign) is not read as 'đau' (pain)")
    void dauHieuIsNotReadAsPain() {
        // The two collapse to the same string once accents are stripped.
        assertThat(intentOf("Dấu hiệu cảnh báo thai kỳ là gì?"))
                .isEqualTo(IntentType.GENERAL_HEALTH_INFORMATION);
        assertThat(intentOf("Tôi đau bụng dữ dội")).isEqualTo(IntentType.SYMPTOM_TRIAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tôi bị ra máu từ sáng", "Em đang chóng mặt", "Bé bị sốt mấy ngày nay"})
    @DisplayName("First-person symptom reports are triage")
    void firstPersonReportsAreTriage(String message) {
        assertThat(intentOf(message)).isEqualTo(IntentType.SYMPTOM_TRIAGE);
    }

    @Test
    @DisplayName("A symptom report wins over a general question in the same message")
    void symptomReportWinsOverGeneralQuestion() {
        // Leaving a real symptom untriaged is the worse error.
        assertThat(intentOf("Tôi bị ra máu, dấu hiệu cảnh báo là gì?"))
                .isEqualTo(IntentType.SYMPTOM_TRIAGE);
    }

    @Test
    @DisplayName("An explicit emergency call routes but does not decide RED")
    void emergencyCallDoesNotDecideRed() {
        var resolution = resolver.resolve("Cấp cứu, phải làm gì ngay?", null, null);
        assertThat(resolution.intent()).isEqualTo(IntentType.EMERGENCY_HELP);
        assertThat(resolution.mayProduceTriageOutcome())
                .as("the Global Safety Gate decides RED, not the intent classifier")
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Kê đơn thuốc giúp tôi", "Uống thuốc gì cho hết?", "Chẩn đoán giúp em với"})
    @DisplayName("Requests CareBridge does not serve are OUT_OF_SCOPE_REQUEST")
    void unsupportedRequestsAreOutOfScope(String message) {
        assertThat(intentOf(message)).isEqualTo(IntentType.OUT_OF_SCOPE_REQUEST);
    }

    @Test
    @DisplayName("An answered question is structurally a follow-up")
    void answeredQuestionIsFollowUp() {
        var resolution = resolver.resolve("Dấu hiệu cảnh báo là gì?",
                List.of("BLEEDING_HEAVY"), null);
        assertThat(resolution.intent()).isEqualTo(IntentType.FOLLOW_UP_ANSWER);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER);
        assertThat(resolution.mayProduceTriageOutcome()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "ok", "vâng"})
    @DisplayName("Unclassifiable messages stay UNKNOWN")
    void unclassifiableMessagesStayUnknown(String message) {
        assertThat(intentOf(message)).isEqualTo(IntentType.UNKNOWN);
    }

    @Test
    @DisplayName("Only triage intents may produce a colour")
    void onlyTriageIntentsMayProduceAColour() {
        assertThat(resolver.resolve("Tôi bị ra máu", null, null).mayProduceTriageOutcome()).isTrue();
        for (String message : List.of("Nguồn này từ đâu?", "Dấu hiệu cảnh báo là gì?",
                "Kê đơn giúp tôi", "")) {
            assertThat(resolver.resolve(message, null, null).mayProduceTriageOutcome())
                    .as(message).isFalse();
        }
    }

    @Test
    @DisplayName("Evidence is recorded for the audit trail")
    void evidenceIsRecorded() {
        assertThat(resolver.resolve("Nguồn này từ đâu?", null, null).evidence()).isNotEmpty();
    }

    @Test
    @DisplayName("Confirmed conversation intent survives an ambiguous follow-up")
    void confirmedIntentSurvivesAmbiguousFollowUp() {
        var resolution = resolver.resolve(
                "không biết", null, null, IntentType.SYMPTOM_TRIAGE);

        assertThat(resolution.intent()).isEqualTo(IntentType.SYMPTOM_TRIAGE);
        assertThat(resolution.source())
                .isEqualTo(ResolutionSource.CONFIRMED_CONVERSATION_INTENT);
    }

    @Test
    @DisplayName("Explicit latest intent outranks confirmed conversation intent")
    void explicitLatestIntentOutranksConfirmedIntent() {
        var resolution = resolver.resolve(
                "Nguồn này từ đâu vậy?", null, null, IntentType.SYMPTOM_TRIAGE);

        assertThat(resolution.intent()).isEqualTo(IntentType.SOURCE_LOOKUP);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE);
    }

    @Test
    @DisplayName("Structural answer outranks confirmed conversation intent")
    void structuralAnswerOutranksConfirmedIntent() {
        var resolution = resolver.resolve(
                "không biết", List.of("UNSURE"), null, IntentType.SYMPTOM_TRIAGE);

        assertThat(resolution.intent()).isEqualTo(IntentType.FOLLOW_UP_ANSWER);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER);
    }
}
