package com.carebridge.backend.community.controller;

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
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.exception.QuestionNotAnswerableException;
import com.carebridge.backend.community.service.CommunityAnswerService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
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
        value = CommunityAnswerController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import(SecurityConfig.class)
class CommunityAnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private CommunityAnswerService answerService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0001-000000000001");
    private static final String BASE_URL = "/api/v1/community/questions/{questionId}/answers";

    private PostCommunityAnswerRequest makeRequest() {
        PostCommunityAnswerRequest req = new PostCommunityAnswerRequest();
        req.setBody("This is a valid personal experience answer with enough characters");
        req.setIsPersonalExperience(true);
        return req;
    }

    private PostCommunityAnswerRequest makeRequest(Consumer<PostCommunityAnswerRequest> overrides) {
        PostCommunityAnswerRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }

    private CommunityAnswerResponse mockResponse() {
        return CommunityAnswerResponse.builder()
                .id(UUID.randomUUID())
                .questionId(QUESTION_ID)
                .authorId(2L)
                .body("This is a valid personal experience answer with enough characters")
                .personalExperience(true)
                .expertLabeled(false)
                .status("PENDING")
                .createdAt(Instant.now())
                .build();
    }

    // COM56-TC-SEC-002: No JWT → 401 (CWE-306)
    @Test
    void postAnswer_noJwt_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().isUnauthorized());
    }

    // COM56-TC happy: authenticated user → 201, status=PENDING, isExpertLabeled=false (ADR-COM-004)
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void postAnswer_authenticatedUser_returns201() throws Exception {
        when(answerService.postAnswer(eq(2L), eq(QUESTION_ID), any())).thenReturn(mockResponse());

        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.expertLabeled").value(false));
    }

    // ADR-COM-004: EXPERT role also allowed
    @Test
    @WithMockUser(username = "3", roles = "EXPERT")
    void postAnswer_expertRole_returns201() throws Exception {
        when(answerService.postAnswer(eq(3L), eq(QUESTION_ID), any())).thenReturn(mockResponse());

        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().isCreated());
    }

    // COM56-TC-003: body too short → 400 (BR-COM-007)
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void postAnswer_bodyTooShort_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest(req -> req.setBody("Short")))))
                .andExpect(status().isBadRequest());

        verify(answerService, never()).postAnswer(any(), any(), any());
    }

    // COM56-TC-004: body too long → 400 (BR-COM-007)
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void postAnswer_bodyTooLong_returns400() throws Exception {
        String longBody = "A".repeat(3001);
        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest(req -> req.setBody(longBody)))))
                .andExpect(status().isBadRequest());

        verify(answerService, never()).postAnswer(any(), any(), any());
    }

    // isPersonalExperience missing → 400
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void postAnswer_isPersonalExperienceMissing_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest(req -> req.setIsPersonalExperience(null)))))
                .andExpect(status().isBadRequest());

        verify(answerService, never()).postAnswer(any(), any(), any());
    }

    // COM56-TC-002: question not APPROVED → 422 COM-007 (ADR-COM-006)
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void postAnswer_questionNotApproved_returns422() throws Exception {
        when(answerService.postAnswer(any(), any(), any()))
                .thenThrow(new QuestionNotAnswerableException(QUESTION_ID.toString()));

        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("COM-007"));
    }

    // COM56-TC-SEC-001: isExpertLabeled=true in JSON body — must be ignored by DTO (ADR-COM-005)
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void postAnswer_expertLabeledInjectionInJson_fieldIgnoredByDto() throws Exception {
        // Raw JSON with injected isExpertLabeled=true — PostCommunityAnswerRequest has no such field
        String rawJson = "{\"body\":\"Valid answer body with enough characters here\","
                + "\"isPersonalExperience\":true,\"isExpertLabeled\":true}";

        when(answerService.postAnswer(eq(2L), eq(QUESTION_ID), any())).thenReturn(mockResponse());

        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawJson))
                .andExpect(status().isCreated())
                // ADR-COM-005: response always shows expertLabeled=false
                .andExpect(jsonPath("$.data.expertLabeled").value(false));
    }
}
