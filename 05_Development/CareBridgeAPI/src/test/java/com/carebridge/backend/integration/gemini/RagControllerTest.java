package com.carebridge.backend.integration.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.integration.gemini.controller.RagController;
import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagAudienceContext;
import com.carebridge.backend.integration.gemini.dto.RagSource;
import com.carebridge.backend.integration.gemini.dto.UserStage;
import com.carebridge.backend.integration.gemini.service.RagPolicyService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.content.support.Story69TestFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// RAG-TC-007, RAG-TC-008, RAG-TC-INT-001
@WebMvcTest(
        value = RagController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagPolicyService ragPolicyService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String DISCLAIMER =
            "Đây là thông tin hỗ trợ AI — không phải chẩn đoán y tế. Vui lòng tham khảo bác sĩ hoặc chuyên gia y tế.";

    private RagAnswerResponse makeNormalResponse() {
        return RagAnswerResponse.builder()
                .answer("Phù chân nhẹ trong thai kỳ thường là bình thường.")
                .disclaimer(DISCLAIMER)
                .sources(List.of(RagSource.builder()
                        .contentId(UUID.fromString("11111111-0000-0000-0000-000000000001"))
                        .title("Bài viết test 1")
                        .build()))
                .fallback(false)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // RAG-TC-007: Empty query → 400 RAG-001
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void generateAnswer_emptyQuery_shouldReturn400WithRag001() throws Exception {
        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"\", \"maxContextChunks\": 3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RAG-001"));

        verify(ragPolicyService, never()).generateAnswer(any(), any());
    }

    // RAG-TC-007b: query quá ngắn (< 3 chars) → 400
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void generateAnswer_tooShortQuery_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"ab\", \"maxContextChunks\": 3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RAG-001"));
    }

    // RAG-TC-007c: maxContextChunks > 10 → 400 RAG-002
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void generateAnswer_maxContextChunksExceeded_shouldReturn400WithRag002() throws Exception {
        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Phù chân thai kỳ\", \"maxContextChunks\": 11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RAG-002"));
    }

    // RAG-TC-008: Unauthenticated → 401
    @Test
    void generateAnswer_noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Phù chân thai kỳ\", \"maxContextChunks\": 3}"))
                .andExpect(status().isUnauthorized());

        verify(ragPolicyService, never()).generateAnswer(any(), any());
    }

    // RAG-TC-INT-001: Full HTTP flow — 200 với disclaimer và isFallback=false
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void generateAnswer_validRequest_shouldReturn200WithDisclaimerAndSources() throws Exception {
        when(ragPolicyService.generateAnswer(any(), any())).thenReturn(makeNormalResponse());

        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Phù chân khi mang thai 28 tuần\", \"userStage\": \"PREGNANCY\", \"maxContextChunks\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").isNotEmpty())
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty())
                .andExpect(jsonPath("$.data.fallback").value(false))
                .andExpect(jsonPath("$.data.sources").isArray());
    }

    // RAG-TC-INT-001b: Fallback response → 200 với isFallback=true
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void generateAnswer_fallbackResponse_shouldReturn200WithIsFallbackTrue() throws Exception {
        RagAnswerResponse fallback = RagAnswerResponse.builder()
                .answer("Tôi hiện không thể trả lời câu hỏi này.")
                .disclaimer(DISCLAIMER)
                .sources(List.of())
                .fallback(true)
                .generatedAt(LocalDateTime.now())
                .build();
        when(ragPolicyService.generateAnswer(any(), any())).thenReturn(fallback);

        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Câu hỏi test fallback\", \"maxContextChunks\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fallback").value(true))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    // RAG-TC-INT-002: Request without maxContextChunks → 200 (field is optional, server defaults)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void generateAnswer_withoutMaxContextChunks_shouldReturn200() throws Exception {
        when(ragPolicyService.generateAnswer(any(), any())).thenReturn(makeNormalResponse());

        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Phù chân khi mang thai 28 tuần\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // RAG-TC-INT-003: Request with maxContextChunks: 10 (boundary) → 200
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void generateAnswer_withMaxContextChunksBoundary10_shouldReturn200() throws Exception {
        when(ragPolicyService.generateAnswer(any(), any())).thenReturn(makeNormalResponse());

        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Phù chân khi mang thai 28 tuần\", \"maxContextChunks\": 10}"))
                .andExpect(status().isOk());
    }

    // RAG-TC-AUTH-001: staff-only role → 403 (health guidance is for personal-use roles only)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "OPERATIONS")
    void generateAnswer_partnerRole_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Phù chân khi mang thai 28 tuần\", \"maxContextChunks\": 3}"))
                .andExpect(status().isForbidden());

        verify(ragPolicyService, never()).generateAnswer(any(), any());
    }

    @ParameterizedTest(name = "SEC-002/RAG-004 allowed role {0}")
    @MethodSource("allowedRagRoles")
    void uc82_69_sec_002_rag_004_allAllowedRolesReachPolicyWithExactAudienceAndStage(
            String role, boolean expectedMother, UserStage requestedStage) throws Exception {
        UUID callerId = UUID.fromString("69000000-0000-0000-0000-000000000804");
        when(ragPolicyService.generateAnswer(any(), any())).thenReturn(makeNormalResponse());

        mockMvc.perform(post("/api/v1/rag/answer")
                        .with(user(callerId.toString()).roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"synthetic allowed-role question\","
                                + "\"userStage\":\"" + requestedStage + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<RagAnswerRequest> request =
                ArgumentCaptor.forClass(RagAnswerRequest.class);
        ArgumentCaptor<RagAudienceContext> audience =
                ArgumentCaptor.forClass(RagAudienceContext.class);
        verify(ragPolicyService).generateAnswer(request.capture(), audience.capture());
        assertThat(request.getValue().getUserStage()).isEqualTo(requestedStage);
        assertThat(audience.getValue())
                .extracting(RagAudienceContext::callerId, RagAudienceContext::mother)
                .containsExactly(callerId, expectedMother);
    }

    @Test
    void generateAnswer_legacyBabyCareStage_normalizesToPostpartum() throws Exception {
        UUID callerId = UUID.fromString("69000000-0000-0000-0000-000000000805");
        when(ragPolicyService.generateAnswer(any(), any())).thenReturn(makeNormalResponse());

        mockMvc.perform(post("/api/v1/rag/answer")
                        .with(user(callerId.toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"synthetic legacy-stage question","userStage":"BABY_CARE"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<RagAnswerRequest> request =
                ArgumentCaptor.forClass(RagAnswerRequest.class);
        verify(ragPolicyService).generateAnswer(request.capture(), any());
        assertThat(request.getValue().getUserStage()).isEqualTo(UserStage.POSTPARTUM);
    }

    @Test
    void uc82_69_rag_006_validationPrecedesPolicyAndPreservesAllNumericBoundaries() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/integration/gemini/controller/RagController.java");
        int queryValidation = source.indexOf("query.length() < 3");
        int chunkValidation = source.indexOf("getMaxContextChunks() > 10");
        int policyCall = source.indexOf("ragPolicyService.generateAnswer");
        assertThat(queryValidation).isGreaterThanOrEqualTo(0);
        assertThat(chunkValidation).isGreaterThan(queryValidation);
        assertThat(policyCall)
                .as("RAG-006: invalid query/chunk values never reach safety/lifecycle/generator")
                .isGreaterThan(chunkValidation);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void uc82_69_rag_003_missingLifecycleReturnsCnt013BeforeAnyGeneratorResponse() throws Exception {
        when(ragPolicyService.generateAnswer(any(), any()))
                .thenThrow(ContentException.lifecycleContextUnavailable());

        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"synthetic question\",\"userStage\":\"POSTPARTUM\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CNT-013"))
                .andExpect(jsonPath("$.message").value("Lifecycle content context unavailable"))
                .andExpect(jsonPath("$.path").value("/api/v1/rag/answer"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @ParameterizedTest
    @MethodSource("validBoundaryBodies")
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void uc82_69_rag_006_acceptsEveryValidQueryAndChunkBoundary(
            String body, Integer expectedLimit) throws Exception {
        when(ragPolicyService.generateAnswer(any(), any())).thenReturn(makeNormalResponse());

        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<com.carebridge.backend.integration.gemini.dto.RagAnswerRequest> request =
                ArgumentCaptor.forClass(
                        com.carebridge.backend.integration.gemini.dto.RagAnswerRequest.class);
        verify(ragPolicyService).generateAnswer(request.capture(), any());
        assertThat(request.getValue().getQuery()).hasSizeBetween(3, 500);
        assertThat(request.getValue().getMaxContextChunks()).isEqualTo(expectedLimit);
    }

    @ParameterizedTest
    @MethodSource("invalidQueryBodies")
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void uc82_69_rag_006_rejectsEveryInvalidQueryBoundaryBeforePolicy(String body)
            throws Exception {
        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RAG-001"));

        verify(ragPolicyService, never()).generateAnswer(any(), any());
    }

    static Stream<Arguments> validBoundaryBodies() {
        return Stream.of(
                Arguments.of("{\"query\":\"abc\"}", null),
                Arguments.of("{\"query\":\"abc\",\"maxContextChunks\":-1}", -1),
                Arguments.of("{\"query\":\"abc\",\"maxContextChunks\":0}", 0),
                Arguments.of("{\"query\":\"abc\",\"maxContextChunks\":1}", 1),
                Arguments.of("{\"query\":\"abc\",\"maxContextChunks\":10}", 10),
                Arguments.of("{\"query\":\"" + "a".repeat(500)
                        + "\",\"maxContextChunks\":1}", 1));
    }

    static Stream<String> invalidQueryBodies() {
        return Stream.of(
                "{}",
                "{\"query\":\"   \"}",
                "{\"query\":\"ab\"}",
                "{\"query\":\"" + "a".repeat(501) + "\"}");
    }

    static Stream<Arguments> allowedRagRoles() {
        return Stream.of(
                Arguments.of("MOTHER", true, UserStage.POSTPARTUM),
                Arguments.of("FAMILY", false, UserStage.PRE_PREGNANCY),
                Arguments.of("EXPERT", false, UserStage.PREGNANCY),
                Arguments.of("MODERATOR", false, UserStage.POSTPARTUM),
                Arguments.of("CONTENT_ADMIN", false, UserStage.POSTPARTUM),
                Arguments.of("SYSTEM_ADMIN", false, UserStage.PRE_PREGNANCY));
    }
}
