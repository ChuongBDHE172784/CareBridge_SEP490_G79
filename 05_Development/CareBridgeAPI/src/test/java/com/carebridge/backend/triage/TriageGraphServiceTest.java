package com.carebridge.backend.triage;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.engine.ChildTriageResult;
import com.carebridge.backend.triage.engine.PediatricRiskRules;
import com.carebridge.backend.triage.engine.SourceRetriever;
import com.carebridge.backend.triage.engine.SymptomNormalizer;
import com.carebridge.backend.triage.engine.TriageGraphService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriageGraphServiceTest {

    private final TriageGraphService graph = new TriageGraphService(
            new SymptomNormalizer(),
            new SourceRetriever(),
            new PediatricRiskRules());

    @Test
    void mildFeverFeedingWell_shouldReturnGreen() {
        ChildTriageResult result = graph.run(base()
                .symptomList(List.of("sốt nhẹ"))
                .temperatureC(37.8)
                .build());

        assertThat(result.getRiskLevel()).isEqualTo("GREEN");
        assertThat(result.isEmergencyActionRequired()).isFalse();
    }

    @Test
    void feverAndCoughNormalBreathing_shouldReturnYellow() {
        ChildTriageResult result = graph.run(base()
                .symptomList(List.of("sốt", "ho"))
                .temperatureC(38.2)
                .build());

        assertThat(result.getRiskLevel()).isEqualTo("YELLOW");
        assertThat(result.getMatchedRules()).contains("YELLOW_RESPIRATORY_NO_DISTRESS");
    }

    @Test
    void breathingDifficulty_shouldReturnRed() {
        ChildTriageResult result = graph.run(base()
                .symptomList(List.of("khó thở"))
                .breathingStatus("khó thở")
                .build());

        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.isEmergencyActionRequired()).isTrue();
        assertThat(result.getMatchedRules()).contains("RED_BREATHING_DISTRESS");
    }

    @Test
    void breathingDifficultyAndCyanosis_shouldAttachAgeMatchedContentDeepLink() {
        ChildTriageResult result = graph.run(base()
                .childAgeMonths(8)
                .symptomList(List.of("kho tho", "moi tim"))
                .breathingStatus("kho tho")
                .build());

        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.getCitations())
                .anySatisfy(citation -> {
                    assertThat(citation.getTitle()).isEqualTo("Một số dấu hiệu cha mẹ cần biết để đưa trẻ đi khám sớm");
                    assertThat(citation.getUrl()).isEqualTo(
                            "https://benhviennhitrunguong.gov.vn/mot-so-dau-hieu-cha-me-can-biet-de-dua-tre-di-kham-som.html");
                })
                .allSatisfy(citation -> assertThat(citation.getUrl()).contains("/"));
    }

    @Test
    void pediatricEvidence_shouldNotBeAttachedOutsideDeclaredAgeRange() {
        ChildTriageResult result = graph.run(base()
                .childAgeMonths(60)
                .symptomList(List.of("kho tho", "moi tim"))
                .breathingStatus("kho tho")
                .build());

        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.getCitations()).isEmpty();
        assertThat(result.getWarning()).isNotBlank();
    }

    @Test
    void seizure_shouldReturnRed() {
        ChildTriageResult result = graph.run(base()
                .symptomList(List.of("co giật"))
                .seizure(true)
                .build());

        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.getMatchedRules()).contains("RED_SEIZURE");
    }

    @Test
    void lethargy_shouldReturnRed() {
        ChildTriageResult result = graph.run(base()
                .symptomList(List.of("li bì"))
                .consciousnessStatus("li bì khó đánh thức")
                .build());

        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.getMatchedRules()).contains("RED_LETHARGY");
    }

    @Test
    void difficultToWake_shouldUseCanonicalSymptomAndReturnRed() {
        ChildTriageResult result = graph.run(base()
                .symptomList(List.of("kho danh thuc"))
                .consciousnessStatus("kho danh thuc")
                .build());

        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.isEmergencyActionRequired()).isTrue();
        assertThat(result.getMatchedRules()).contains("RED_LETHARGY");
        assertThat(result.getNormalizedSymptoms()).contains("difficult_to_wake");
    }

    @Test
    void diarrheaWithDehydration_shouldReturnRed() {
        ChildTriageResult result = graph.run(base()
                .symptomList(List.of("tiêu chảy"))
                .diarrhea("tiêu chảy nhiều")
                .dehydrationSigns(List.of("môi khô", "tiểu ít"))
                .build());

        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.getMatchedRules()).contains("RED_DIARRHEA_DEHYDRATION");
    }

    @Test
    void missingChildAge_shouldNeedMoreInfo() {
        ChildTriageResult result = graph.run(base()
                .childAgeMonths(null)
                .build());

        assertThat(result.getStatus()).isEqualTo("NEED_MORE_INFO");
        assertThat(result.getRiskLevel()).isNull();
        assertThat(result.getQuestions()).anyMatch(q -> q.contains("bao nhiêu tháng"));
    }

    @Test
    void immediateRedFlagWithMissingAge_shouldStillReturnRed() {
        ChildTriageResult result = graph.run(base()
                .childAgeMonths(null)
                .symptomList(List.of("kho tho"))
                .breathingStatus("kho tho")
                .build());

        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.isEmergencyActionRequired()).isTrue();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void postpartumWithoutUniversalDangerSignal_shouldNeverUsePediatricCopyOrEvidence() {
        ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                .stage(TriageStage.POSTPARTUM)
                .symptomList(List.of("chong_mat"))
                .duration("1 ngày")
                .parentFreeText("Tôi thấy chóng mặt")
                .breathingStatus("bình thường")
                .consciousnessStatus("tỉnh táo")
                .seizure(false)
                .build());

        assertThat(result.getStatus()).isEqualTo("NEED_MORE_INFO");
        assertThat(result.getRiskLevel()).isNull();
        assertThat(result.getCitations()).isEmpty();
        assertThat(result.getSummary()).doesNotContain("trẻ", "bé", "bú");
        assertThat(result.getPossibleConcern()).doesNotContain("trẻ", "bé", "bú");
        assertThat(result.getRecommendedAction()).doesNotContain("trẻ", "bé", "bú");
        assertThat(result.getDisclaimer()).doesNotContain("trẻ", "bé", "bú");
    }

    @Test
    void postpartumBreathingDifficulty_shouldReturnDeterministicRedWithoutPediatricEvidence() {
        ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                .stage(TriageStage.POSTPARTUM)
                .symptomList(List.of("kho_tho"))
                .duration("vừa xuất hiện")
                .parentFreeText("Tôi thấy khó thở")
                .breathingStatus("khó thở")
                .consciousnessStatus("tỉnh táo")
                .seizure(false)
                .build());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.isEmergencyActionRequired()).isTrue();
        assertThat(result.getMatchedRules()).contains("RED_POSTPARTUM_BREATHING_DISTRESS");
        assertThat(result.getCitations()).isEmpty();
        assertThat(result.getRecommendedAction()).contains("115").doesNotContain("trẻ", "bé");
    }

    @Test
    void postpartumUrgentFreeText_shouldReturnRedWithoutStructuredAnswers() {
        ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                .stage(TriageStage.POSTPARTUM)
                .parentFreeText("Tôi khó thở, chảy máu nhiều và muốn tự làm hại bản thân")
                .symptomList(List.of())
                .duration("vừa xuất hiện")
                .build());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.isEmergencyActionRequired()).isTrue();
        assertThat(result.getMatchedRules()).contains(
                "RED_POSTPARTUM_BREATHING_DISTRESS",
                "RED_POSTPARTUM_HEAVY_BLEEDING",
                "RED_POSTPARTUM_SELF_HARM");
    }

    @Test
    void postpartumUrgentSuppliedSymptoms_shouldRecognizeCanonicalTokens() {
        ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                .stage(TriageStage.POSTPARTUM)
                .parentFreeText("Cần hỗ trợ")
                .symptomList(List.of("tim_tai", "co_giat", "bat_tinh"))
                .duration("vừa xuất hiện")
                .build());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.getMatchedRules()).contains(
                "RED_POSTPARTUM_CYANOSIS",
                "RED_POSTPARTUM_SEIZURE",
                "RED_POSTPARTUM_ALTERED_CONSCIOUSNESS");
    }

    @Test
    void preconceptionAndPregnancyUniversalDangerSigns_shouldReturnDeterministicRed() {
        List<MaternalDangerVector> vectors = List.of(
                new MaternalDangerVector("breathing distress", "Tôi đang khó thở", null, null, false,
                        "BREATHING_DISTRESS"),
                new MaternalDangerVector("cyanosis", "Môi tôi tím tái", "tím tái", null, false,
                        "CYANOSIS"),
                new MaternalDangerVector("seizure", "Tôi vừa bị co giật", null, null, true,
                        "SEIZURE"),
                new MaternalDangerVector("altered consciousness", "Tôi vừa ngất xỉu", null, "ngất", false,
                        "ALTERED_CONSCIOUSNESS"),
                new MaternalDangerVector("heavy bleeding", "Tôi đang chảy máu nhiều", null, null, false,
                        "HEAVY_BLEEDING"),
                new MaternalDangerVector("self harm", "Tôi muốn tự sát ngay", null, null, false,
                        "SELF_HARM"));

        for (TriageStage stage : List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY)) {
            for (MaternalDangerVector vector : vectors) {
                ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                        .stage(stage)
                        .duration("vừa xuất hiện")
                        .parentFreeText(vector.parentFreeText())
                        .breathingStatus(vector.breathingStatus())
                        .consciousnessStatus(vector.consciousnessStatus())
                        .seizure(vector.seizure())
                        .build());

                assertThat(result.getStatus()).as(stage + " / " + vector.name()).isEqualTo("COMPLETED");
                assertThat(result.getRiskLevel()).as(stage + " / " + vector.name()).isEqualTo("RED");
                assertThat(result.isEmergencyActionRequired()).as(stage + " / " + vector.name()).isTrue();
                assertThat(result.getMatchedRules()).as(stage + " / " + vector.name())
                        .containsExactly("RED_" + stage.name() + "_" + vector.expectedRuleSuffix());
                assertThat(result.getRecommendedAction()).as(stage + " / " + vector.name())
                        .contains("115")
                        .doesNotContain("trẻ", "bé", "bú");
                assertThat(result.getDisclaimer()).as(stage + " / " + vector.name())
                        .doesNotContain("trẻ", "bé", "bú");
            }
        }
    }

    @Test
    void preconceptionAndPregnancyNegatedUniversalSigns_shouldRemainNeedMoreInfo() {
        for (TriageStage stage : List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY)) {
            ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                    .stage(stage)
                    .duration("1 ngày")
                    .parentFreeText("Tôi không khó thở, không co giật, không ngất, không chảy máu nhiều "
                            + "và không muốn tự làm hại bản thân")
                    .breathingStatus("bình thường")
                    .consciousnessStatus("tỉnh táo")
                    .seizure(false)
                    .build());

            assertThat(result.getStatus()).as(stage.name()).isEqualTo("NEED_MORE_INFO");
            assertThat(result.getRiskLevel()).as(stage.name()).isNull();
            assertThat(result.isEmergencyActionRequired()).as(stage.name()).isFalse();
            assertThat(result.getMatchedRules()).as(stage.name())
                    .noneMatch(rule -> rule.startsWith("RED_" + stage.name() + "_"));
        }
    }

    @Test
    void mixedNegatedAndAffirmedMaternalClauses_shouldPreserveLaterHeavyBleedingRed() {
        for (TriageStage stage : List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY)) {
            for (String report : List.of(
                    "Tôi không khó thở và chảy máu nhiều",
                    "I have no breathing difficulty and heavy bleeding")) {
                ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                        .stage(stage)
                        .duration("vừa xuất hiện")
                        .parentFreeText(report)
                        .build());

                assertThat(result.getRiskLevel()).as(stage + " / " + report).isEqualTo("RED");
                assertThat(result.getMatchedRules()).as(stage + " / " + report)
                        .containsExactly("RED_" + stage.name() + "_HEAVY_BLEEDING");
            }
        }
    }

    @Test
    void independentlyNegatedMaternalClauses_shouldRemainNeedMoreInfo() {
        for (TriageStage stage : List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY)) {
            for (String report : List.of(
                    "Tôi không khó thở và không chảy máu nhiều",
                    "I have no breathing difficulty and no heavy bleeding")) {
                ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                        .stage(stage)
                        .duration("1 ngày")
                        .parentFreeText(report)
                        .build());

                assertThat(result.getStatus()).as(stage + " / " + report).isEqualTo("NEED_MORE_INFO");
                assertThat(result.getRiskLevel()).as(stage + " / " + report).isNull();
                assertThat(result.isEmergencyActionRequired()).as(stage + " / " + report).isFalse();
            }
        }
    }

    // Regression fix: UniversalMaternalRedRules previously had no "ma" ("but") clause-boundary
    // word, so the leading "không" (no) negation scope leaked across "mà" and swallowed the
    // later, affirmed red sign — a missed RED. Parity with Python's risk_rules.py clause split.
    @Test
    void clauseBoundaryMaAfterNegation_shouldStillDetectLaterRedSign() {
        for (TriageStage stage : List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY, TriageStage.POSTPARTUM)) {
            ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                    .stage(stage)
                    .duration("vừa xuất hiện")
                    .parentFreeText("Tôi không khó thở mà ngất")
                    .build());

            assertThat(result.getRiskLevel()).as(stage.name()).isEqualTo("RED");
            assertThat(result.getMatchedRules()).as(stage.name())
                    .containsExactly("RED_" + stage.name() + "_ALTERED_CONSCIOUSNESS");
        }
    }

    // Regression fix: CLAUSE_BOUNDARIES had no multi-word "tuy nhien" ("however") entry; it is
    // now folded into the same pipe-delimiter treatment as punctuation before scanning.
    @Test
    void clauseBoundaryTuyNhienAfterNegation_shouldStillDetectLaterRedSign() {
        for (TriageStage stage : List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY, TriageStage.POSTPARTUM)) {
            ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                    .stage(stage)
                    .duration("vừa xuất hiện")
                    .parentFreeText("Tôi không khó thở, tuy nhiên tôi bị co giật")
                    .build());

            assertThat(result.getRiskLevel()).as(stage.name()).isEqualTo("RED");
            assertThat(result.getMatchedRules()).as(stage.name())
                    .containsExactly("RED_" + stage.name() + "_SEIZURE");
        }
    }

    // Regression fix: NEGATIONS previously omitted "never", so "I never had difficulty
    // breathing" was misread as an affirmed red sign instead of a negated one (false RED).
    @Test
    void neverNegationWord_shouldKeepRedSignSuppressed() {
        for (TriageStage stage : List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY, TriageStage.POSTPARTUM)) {
            ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                    .stage(stage)
                    .duration("1 ngày")
                    .parentFreeText("I never had difficulty breathing")
                    .build());

            assertThat(result.getStatus()).as(stage.name()).isEqualTo("NEED_MORE_INFO");
            assertThat(result.getRiskLevel()).as(stage.name()).isNull();
        }
    }

    // Regression fix: NEGATIONS previously omitted "cha" (colloquial Vietnamese negation, e.g.
    // "chả khó thở"), so it was misread as an affirmed red sign instead of a negated one.
    @Test
    void chaNegationWord_shouldKeepRedSignSuppressed() {
        for (TriageStage stage : List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY, TriageStage.POSTPARTUM)) {
            ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                    .stage(stage)
                    .duration("1 ngày")
                    .parentFreeText("Tôi chả khó thở")
                    .build());

            assertThat(result.getStatus()).as(stage.name()).isEqualTo("NEED_MORE_INFO");
            assertThat(result.getRiskLevel()).as(stage.name()).isNull();
        }
    }

    @Test
    void preconceptionAndPregnancyParitySynonyms_shouldEmitExactlyOneStageSpecificRule() {
        List<MaternalDangerVector> vectors = List.of(
                new MaternalDangerVector("unable to breathe", "I am unable to breathe", null, null, false,
                        "BREATHING_DISTRESS"),
                new MaternalDangerVector("cyanotic", "I am cyanotic", null, null, false,
                        "CYANOSIS"),
                new MaternalDangerVector("fainting", "I am fainting", null, null, false,
                        "ALTERED_CONSCIOUSNESS"),
                new MaternalDangerVector("loss of consciousness", "I had loss of consciousness", null, null, false,
                        "ALTERED_CONSCIOUSNESS"),
                new MaternalDangerVector("bleeding heavily", "I am bleeding heavily", null, null, false,
                        "HEAVY_BLEEDING"),
                new MaternalDangerVector("tự tử", "Tôi muốn tự tử", null, null, false,
                        "SELF_HARM"));

        for (TriageStage stage : List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY)) {
            for (MaternalDangerVector vector : vectors) {
                ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                        .stage(stage)
                        .duration("vừa xuất hiện")
                        .parentFreeText(vector.parentFreeText())
                        .build());

                assertThat(result.getRiskLevel()).as(stage + " / " + vector.name()).isEqualTo("RED");
                assertThat(result.getMatchedRules()).as(stage + " / " + vector.name())
                        .containsExactly("RED_" + stage.name() + "_" + vector.expectedRuleSuffix());
            }
        }
    }

    @Test
    void negatedParitySynonyms_shouldRemainNonRedWithoutRuleLeakage() {
        for (TriageStage stage : List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY)) {
            for (String report : List.of(
                    "I am not unable to breathe, not cyanotic, not fainting, no loss of consciousness, "
                            + "not bleeding heavily and no self harm",
                    "Tôi không không thở được",
                    "Tôi không tự tử")) {
                ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                        .stage(stage)
                        .duration("1 ngày")
                        .parentFreeText(report)
                        .build());

                assertThat(result.getStatus()).as(stage + " / " + report).isEqualTo("NEED_MORE_INFO");
                assertThat(result.getRiskLevel()).as(stage + " / " + report).isNull();
                assertThat(result.getMatchedRules()).as(stage + " / " + report)
                        .noneMatch(rule -> rule.startsWith("RED_" + stage.name() + "_"));
            }
        }
    }

    @Test
    void maternalFallbackConsciousnessOption_shouldReturnExactAlteredConsciousnessRed() {
        for (TriageStage stage : List.of(
                TriageStage.PRECONCEPTION, TriageStage.PREGNANCY, TriageStage.POSTPARTUM)) {
            ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                    .stage(stage)
                    .duration("vừa xuất hiện")
                    .consciousnessStatus("Khó giữ tỉnh táo")
                    .build());

            assertThat(result.getStatus()).as(stage.name()).isEqualTo("COMPLETED");
            assertThat(result.getRiskLevel()).as(stage.name()).isEqualTo("RED");
            assertThat(result.getMatchedRules()).as(stage.name())
                    .containsExactly("RED_" + stage.name() + "_ALTERED_CONSCIOUSNESS");
            assertThat(result.getNormalizedSymptoms()).as(stage.name()).contains("difficult_to_wake");
        }
    }

    @Test
    void negatedMaternalFallbackConsciousnessOption_shouldRemainNonRed() {
        for (TriageStage stage : List.of(
                TriageStage.PRECONCEPTION, TriageStage.PREGNANCY, TriageStage.POSTPARTUM)) {
            ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                    .stage(stage)
                    .duration("1 ngày")
                    .consciousnessStatus("Không khó giữ tỉnh táo")
                    .parentFreeText("Tôi không khó giữ tỉnh táo")
                    .build());

            assertThat(result.getStatus()).as(stage.name()).isEqualTo("NEED_MORE_INFO");
            assertThat(result.getRiskLevel()).as(stage.name()).isNull();
            assertThat(result.getMatchedRules()).as(stage.name())
                    .noneMatch(rule -> rule.endsWith("_ALTERED_CONSCIOUSNESS"));
        }
    }

    @Test
    void unsignedPregnancySpecificSigns_shouldRemainInactive() {
        ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                .stage(TriageStage.PREGNANCY)
                .duration("2 giờ")
                .parentFreeText("Tôi đau bụng dữ dội, đau đầu dữ dội, hoa mắt và giảm cử động thai")
                .breathingStatus("bình thường")
                .consciousnessStatus("tỉnh táo")
                .seizure(false)
                .build());

                assertThat(result.getStatus()).isEqualTo("NEED_MORE_INFO");
                assertThat(result.getRiskLevel()).isNull();
                assertThat(result.isEmergencyActionRequired()).isFalse();
                assertThat(result.getMatchedRules()).containsExactly("PREGNANCY_RULES_NEED_CLINICAL_REVIEW");
                assertThat(result.getQuestions()).allSatisfy(question ->
                        assertThat(question).doesNotContain("trẻ", "bé", "bú", "child", "baby"));
                assertThat(result.getPossibleConcern()).doesNotContain("trẻ", "bé", "bú", "child", "baby");
                assertThat(result.getRecommendedAction()).doesNotContain("trẻ", "bé", "bú", "child", "baby");
                assertThat(result.getDisclaimer()).doesNotContain("trẻ", "bé", "bú", "child", "baby");
    }

    @Test
    void preconceptionNonDangerInput_shouldUseOnlyMaternalQuestionsAndCopy() {
        ChildTriageResult result = graph.run(RunIntakeRequest.builder()
                .stage(TriageStage.PRECONCEPTION)
                .duration("1 ngày")
                .parentFreeText("Tôi muốn được tư vấn chuẩn bị sức khỏe")
                .breathingStatus("bình thường")
                .consciousnessStatus("tỉnh táo")
                .seizure(false)
                .build());

        assertThat(result.getStatus()).isEqualTo("NEED_MORE_INFO");
        assertThat(result.getRiskLevel()).isNull();
        assertThat(result.getQuestions()).allSatisfy(question ->
                assertThat(question).doesNotContain("trẻ", "bé", "bú", "child", "baby"));
        assertThat(result.getPossibleConcern()).doesNotContain("trẻ", "bé", "bú", "child", "baby");
        assertThat(result.getRecommendedAction()).doesNotContain("trẻ", "bé", "bú", "child", "baby");
        assertThat(result.getDisclaimer()).doesNotContain("trẻ", "bé", "bú", "child", "baby");
    }

    @Test
    void noCitationMatch_shouldNotFabricateSource() {
        ChildTriageResult result = graph.run(base()
                .symptomList(List.of("ngứa tai"))
                .parentFreeText("ngứa tai nhẹ")
                .build());

        assertThat(result.getCitations()).isEmpty();
        assertThat(result.getWarning()).isEqualTo("Không tìm thấy nguồn phù hợp trong knowledge base");
    }

    private RunIntakeRequest.RunIntakeRequestBuilder base() {
        return RunIntakeRequest.builder()
                .childAgeMonths(18)
                .symptomList(List.of())
                .duration("1 ngày")
                .feedingStatus("bú/uống tốt")
                .breathingStatus("thở bình thường")
                .consciousnessStatus("tỉnh táo")
                .seizure(false)
                .dehydrationSigns(List.of());
    }

    private record MaternalDangerVector(
            String name,
            String parentFreeText,
            String breathingStatus,
            String consciousnessStatus,
            boolean seizure,
            String expectedRuleSuffix) {
    }
}
