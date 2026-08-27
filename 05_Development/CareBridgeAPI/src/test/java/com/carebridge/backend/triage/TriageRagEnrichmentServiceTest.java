package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagSource;
import com.carebridge.backend.integration.gemini.service.RagPolicyService;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import com.carebridge.backend.triage.service.TriageRagEnrichmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TriageRagEnrichmentServiceTest {

    @Mock private RagPolicyService ragPolicyService;
    @Mock private EvidenceSourceService evidenceSourceService;

    private TriageRagEnrichmentService service() {
        return new TriageRagEnrichmentService(
                ragPolicyService, evidenceSourceService, new ObjectMapper());
    }

    @Test
    void terminalGreenUsesActualSymptomsAndCanonicalStageAndAddsApprovedCitation() {
        UUID contentId = UUID.randomUUID();
        when(evidenceSourceService.isApprovedDeepLink(any(URI.class))).thenReturn(true);
        when(ragPolicyService.generateAnswer(any(), any())).thenReturn(RagAnswerResponse.builder()
                .answer("Theo dõi triệu chứng và liên hệ nhân viên y tế nếu nặng hơn.")
                .disclaimer("Không thay thế tư vấn y tế.")
                .fallback(false)
                .sources(List.of(RagSource.builder()
                        .contentId(contentId)
                        .title("Hướng dẫn đau đầu thai kỳ")
                        .url("https://www.who.int/health-topics/maternal-health")
                        .publisher("WHO")
                        .sourceVersion("7")
                        .lastReviewed("2026-08-01T00:00:00Z")
                        .excerpt("Thông tin đã được duyệt")
                        .build()))
                .build());

        Map<String, Object> result = new LinkedHashMap<>(Map.of(
                "status", "COMPLETED", "riskLevel", "YELLOW"));
        service().enrichOneShot(result, TriageStage.PREGNANCY, UUID.randomUUID(), false,
                Map.of("symptoms", "đau đầu và chóng mặt", "duration", "2 ngày"));

        ArgumentCaptor<RagAnswerRequest> request = ArgumentCaptor.forClass(RagAnswerRequest.class);
        verify(ragPolicyService).generateAnswer(request.capture(), any());
        assertThat(request.getValue().getQuery())
                .contains("đau đầu và chóng mặt", "2 ngày", "pregnancy");
        assertThat(result.get("ragAnswer")).isEqualTo(
                "Theo dõi triệu chứng và liên hệ nhân viên y tế nếu nặng hơn.");
        assertThat(result.get("ragFallback")).isEqualTo(false);
        assertThat(result.get("citations")).asList().hasSize(1);
        assertThat(((Map<?, ?>) ((List<?>) result.get("citations")).get(0)).get("url"))
                .isEqualTo("https://www.who.int/health-topics/maternal-health");
    }

    @Test
    void redAndAskMoreNeverInvokeRag() {
        Map<String, Object> red = new LinkedHashMap<>(Map.of(
                "status", "COMPLETED", "riskLevel", "RED"));
        Map<String, Object> askMore = new LinkedHashMap<>(Map.of(
                "status", "NEED_MORE_INFO", "riskLevel", "NEED_MORE_INFO"));

        service().enrichOneShot(red, TriageStage.PREGNANCY, UUID.randomUUID(), true,
                Map.of("symptoms", "khó thở"));
        service().enrichOneShot(askMore, TriageStage.PREGNANCY, UUID.randomUUID(), true,
                Map.of("symptoms", "đau đầu"));

        verify(ragPolicyService, never()).generateAnswer(any(), any());
    }

    @Test
    void invalidSourceUrlIsOmittedAndClassificationRemainsAvailable() {
        when(ragPolicyService.generateAnswer(any(), any())).thenReturn(RagAnswerResponse.builder()
                .answer("Câu trả lời")
                .disclaimer("Disclaimer")
                .fallback(false)
                .sources(List.of(RagSource.builder()
                        .contentId(UUID.randomUUID())
                        .title("Nguồn không hợp lệ")
                        .url("https://example.com/search?q=đau+đầu")
                        .publisher("Unknown")
                        .sourceVersion("1")
                        .lastReviewed("2026-08-01T00:00:00Z")
                        .build()))
                .build());
        Map<String, Object> result = new LinkedHashMap<>(Map.of(
                "status", "COMPLETED", "riskLevel", "GREEN"));

        service().enrichOneShot(result, TriageStage.POSTPARTUM, UUID.randomUUID(), true,
                Map.of("parentFreeText", "đau đầu sau sinh"));

        assertThat(result.get("riskLevel")).isEqualTo("GREEN");
        assertThat(result.get("citations")).isNull();
        assertThat(result.get("ragFallback")).isEqualTo(true);
        assertThat(result.get("warning").toString()).contains("nguồn tham khảo");
    }

    @Test
    void blankSymptomsAreSkippedInsteadOfIssuingAStageOnlyQuery() {
        Map<String, Object> result = new LinkedHashMap<>(Map.of(
                "status", "COMPLETED", "riskLevel", "GREEN"));

        service().enrichOneShot(result, TriageStage.PREGNANCY, UUID.randomUUID(), false,
                Map.of("symptoms", " "));

        verify(ragPolicyService, never()).generateAnswer(any(), any());
        assertThat(result.get("ragFallback")).isEqualTo(true);
    }

    @Test
    void oneCharacterSymptomIsSkippedEvenWhenStructuredFactsArePresent() {
        Map<String, Object> result = new LinkedHashMap<>(Map.of(
                "status", "COMPLETED", "riskLevel", "GREEN"));

        service().enrichOneShot(result, TriageStage.PREGNANCY, UUID.randomUUID(), false,
                Map.of("symptoms", "x", "duration", "2 days"));

        verify(ragPolicyService, never()).generateAnswer(any(), any());
        assertThat(result.get("ragFallback")).isEqualTo(true);
    }

    @Test
    void emergencyFlagWinsOverGreenRiskAndNeverEnriches() {
        Map<String, Object> result = new LinkedHashMap<>(Map.of(
                "status", "COMPLETED", "riskLevel", "GREEN", "emergencyActionRequired", true,
                "ragAnswer", "unsafe"));

        service().enrichOneShot(result, TriageStage.PREGNANCY, UUID.randomUUID(), false,
                Map.of("symptoms", "đau đầu"));

        verify(ragPolicyService, never()).generateAnswer(any(), any());
        assertThat(result).doesNotContainKey("ragAnswer");
    }
}
