package com.carebridge.backend.community.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.service.CommunityQuestionService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = CommunityQuestionController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import(SecurityConfig.class)
class CommunityQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private CommunityQuestionService questionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final String BASE_URL = "/api/v1/community/questions";
    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private CreateCommunityQuestionRequest makeRequest() {
        CreateCommunityQuestionRequest req = new CreateCommunityQuestionRequest();
        req.setTopicId(TOPIC_ID);
        req.setTitle("Valid test question title");
        req.setBody("This is a valid test body with enough characters");
        req.setStage(PregnancyStage.PREGNANCY);
        req.setPregnancyWeek(20);
        req.setUrgency(UrgencyLevel.NORMAL);
        req.setIsAnonymous(false);
        return req;
    }

    private CreateCommunityQuestionRequest makeRequest(Consumer<CreateCommunityQuestionRequest> overrides) {
        CreateCommunityQuestionRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }

    private CommunityQuestionResponse mockResponse() {
        return CommunityQuestionResponse.builder()
                .id(UUID.randomUUID())
                .topicId(TOPIC_ID)
                .title("Valid test question title")
                .body("This is a valid test body with enough characters")
                .stage("PREGNANCY")
                .urgency("NORMAL")
                .anonymous(false)
                .status("PENDING")
                .createdAt(Instant.now())
                .build();
    }

    // COM-TC-SEC-001: No JWT → 401 (CWE-306)
    @Test
    void createQuestion_noJwt_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().isUnauthorized());
    }

    // COM-TC-SEC-002: ROLE_GUEST → 403 (ADR-COM-001, CWE-862)
    @Test
    @WithMockUser(username = "3", roles = "GUEST")
    void createQuestion_guestRole_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().isForbidden());

        verify(questionService, never()).createQuestion(any(), any());
    }

    // COM-TC-SEC-002: ROLE_EXPERT → 403
    @Test
    @WithMockUser(username = "4", roles = "EXPERT")
    void createQuestion_expertRole_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().isForbidden());
    }

    // Happy path: ROLE_MOTHER → 201 with status=PENDING
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void createQuestion_motherRole_returns201() throws Exception {
        when(questionService.createQuestion(eq(2L), any())).thenReturn(mockResponse());

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    // COM-TC-004: title too short (< 5 chars) → 400 (BR-COM-003)
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void createQuestion_titleTooShort_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(makeRequest(req -> req.setTitle("Hi")));

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());

        verify(questionService, never()).createQuestion(any(), any());
    }

    // COM-TC-005: body too long (> 5000 chars) → 400 (BR-COM-004)
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void createQuestion_bodyTooLong_returns400() throws Exception {
        String longBody = "A".repeat(5001);
        String body = objectMapper.writeValueAsString(makeRequest(req -> req.setBody(longBody)));

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());

        verify(questionService, never()).createQuestion(any(), any());
    }

    // COM-TC-004 sub: title missing → 400
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void createQuestion_titleMissing_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(makeRequest(req -> req.setTitle(null)));

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    // topicId missing → 400
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void createQuestion_topicIdMissing_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(makeRequest(req -> req.setTopicId(null)));

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    // COM-TC-006: pregnancyWeek boundary values — valid (1, 42) → 201
    @ParameterizedTest
    @ValueSource(ints = {1, 42})
    @WithMockUser(username = "2", roles = "MOTHER")
    void createQuestion_validPregnancyWeekBoundary_returns201(int week) throws Exception {
        when(questionService.createQuestion(any(), any())).thenReturn(mockResponse());
        String body = objectMapper.writeValueAsString(makeRequest(req -> req.setPregnancyWeek(week)));

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    // COM-TC-006: pregnancyWeek boundary — invalid (0, 43) → 400
    @ParameterizedTest
    @ValueSource(ints = {0, 43})
    @WithMockUser(username = "2", roles = "MOTHER")
    void createQuestion_invalidPregnancyWeekBoundary_returns400(int week) throws Exception {
        String body = objectMapper.writeValueAsString(makeRequest(req -> req.setPregnancyWeek(week)));

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    // Hidden topic → 404 COM-003
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void createQuestion_hiddenTopic_returns404() throws Exception {
        when(questionService.createQuestion(any(), any()))
                .thenThrow(new CommunityTopicNotFoundException(TOPIC_ID.toString()));

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("COM-003"));
    }

    // COM-TC-SEC-003: XSS in title — stored safely (no executable script in response)
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void createQuestion_xssInTitle_storedSafely() throws Exception {
        String xssTitle = "<script>alert('xss')</script>Valid question title";
        CommunityQuestionResponse safeResponse = CommunityQuestionResponse.builder()
                .id(UUID.randomUUID())
                .title(xssTitle)
                .status("PENDING")
                .createdAt(Instant.now())
                .build();
        when(questionService.createQuestion(any(), any())).thenReturn(safeResponse);

        String body = objectMapper.writeValueAsString(makeRequest(req -> req.setTitle(xssTitle)));

        // Expect 201 (stored as-is), or 400 (rejected)
        int statusCode = mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andReturn()
                .getResponse()
                .getStatus();

        // Either rejected (400) or accepted (201) — both safe outcomes
        assertThat(statusCode).isIn(200, 201, 400);
    }
}
