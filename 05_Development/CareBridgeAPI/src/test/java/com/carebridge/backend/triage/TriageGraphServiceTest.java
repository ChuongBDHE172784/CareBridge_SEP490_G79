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
}
