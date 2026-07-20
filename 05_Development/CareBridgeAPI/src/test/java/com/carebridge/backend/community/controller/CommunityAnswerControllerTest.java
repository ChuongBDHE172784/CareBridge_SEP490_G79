package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.community.dto.request.EditAnswerRequest;
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.exception.AnswerNotEditableException;
import com.carebridge.backend.community.exception.AnswerNotFoundException;
import com.carebridge.backend.community.exception.QuestionNotAnswerableException;
import com.carebridge.backend.community.service.CommunityAnswerService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import org.springframework.security.access.AccessDeniedException;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.Optional;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CommunityAnswerController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CommunityAnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private CommunityAnswerService answerService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ExpertProfileRepository expertProfileRepository;

    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0001-000000000001");
    private static final String BASE_URL = "/api/v1/community/questions/{questionId}/answers";

    private ExpertProfile mockExpertProfile() {
        return ExpertProfile.builder()
                .expertProfileId(UUID.randomUUID())
                .userId(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .build();
    }

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
                .authorId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
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
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void postAnswer_authenticatedUser_returns201() throws Exception {
        when(answerService.postAnswer(eq(UUID.fromString("00000000-0000-0000-0000-000000000002")), eq(QUESTION_ID), any())).thenReturn(mockResponse());

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
    @WithMockUser(username = "00000000-0000-0000-0000-000000000003", roles = "EXPERT")
    void postAnswer_expertRole_returns201() throws Exception {
        when(expertProfileRepository.findByUserId(eq(UUID.fromString("00000000-0000-0000-0000-000000000003"))))
                .thenReturn(Optional.of(mockExpertProfile()));
        when(answerService.postAnswer(eq(UUID.fromString("00000000-0000-0000-0000-000000000003")), eq(QUESTION_ID), any())).thenReturn(mockResponse());

        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().isCreated());
    }

    // COM56-TC-003: body too short → 400 (BR-COM-007)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void postAnswer_bodyTooShort_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest(req -> req.setBody("Short")))))
                .andExpect(status().isBadRequest());

        verify(answerService, never()).postAnswer(any(), any(), any());
    }

    // COM56-TC-004: body too long → 400 (BR-COM-007)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
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
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void postAnswer_isPersonalExperienceMissing_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest(req -> req.setIsPersonalExperience(null)))))
                .andExpect(status().isBadRequest());

        verify(answerService, never()).postAnswer(any(), any(), any());
    }

    // COM56-TC-002: question not APPROVED → 422 COM-007 (ADR-COM-006)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void postAnswer_questionNotApproved_returns422() throws Exception {
        when(answerService.postAnswer(any(), any(), any()))
                .thenThrow(new QuestionNotAnswerableException(QUESTION_ID.toString()));

        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeRequest())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("COM-007"));
    }

    // COM56-TC-SEC-001: isExpertLabeled=true in JSON body — must be ignored by DTO (ADR-COM-005)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void postAnswer_expertLabeledInjectionInJson_fieldIgnoredByDto() throws Exception {
        // Raw JSON with injected isExpertLabeled=true — PostCommunityAnswerRequest has no such field
        String rawJson = "{\"body\":\"Valid answer body with enough characters here\","
                + "\"isPersonalExperience\":true,\"isExpertLabeled\":true}";

        when(answerService.postAnswer(eq(UUID.fromString("00000000-0000-0000-0000-000000000002")), eq(QUESTION_ID), any())).thenReturn(mockResponse());

        mockMvc.perform(post(BASE_URL, QUESTION_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawJson))
                .andExpect(status().isCreated())
                // ADR-COM-005: response always shows expertLabeled=false
                .andExpect(jsonPath("$.data.expertLabeled").value(false));
    }

    // ===================== UC-200: Edit Own Answer =====================

    private static final UUID ANSWER_ID = UUID.fromString("00000000-0000-0000-0002-000000000001");
    private static final String ANSWER_URL = BASE_URL + "/{id}";

    private EditAnswerRequest makeEditRequest() {
        EditAnswerRequest req = new EditAnswerRequest();
        req.setBody("Updated answer body with enough characters");
        req.setIsPersonalExperience(true);
        return req;
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void editAnswer_validRequest_returns200() throws Exception {
        CommunityAnswerResponse response = mockResponse();
        when(answerService.editAnswer(eq(ANSWER_ID), any(), any())).thenReturn(response);

        mockMvc.perform(patch(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeEditRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void editAnswer_noJwt_returns401() throws Exception {
        mockMvc.perform(patch(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeEditRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void editAnswer_nonOwner_returns403() throws Exception {
        when(answerService.editAnswer(eq(ANSWER_ID), any(), any()))
                .thenThrow(new AccessDeniedException("Only the author can edit this answer"));

        mockMvc.perform(patch(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeEditRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void editAnswer_notEditable_returns409() throws Exception {
        when(answerService.editAnswer(eq(ANSWER_ID), any(), any()))
                .thenThrow(new AnswerNotEditableException(ANSWER_ID.toString()));

        mockMvc.perform(patch(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeEditRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("COM-013"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void editAnswer_answerNotFound_returns404() throws Exception {
        when(answerService.editAnswer(eq(ANSWER_ID), any(), any()))
                .thenThrow(new AnswerNotFoundException(ANSWER_ID.toString()));

        mockMvc.perform(patch(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeEditRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void editAnswer_bodyBlank_returns400() throws Exception {
        EditAnswerRequest req = makeEditRequest();
        req.setBody("");

        mockMvc.perform(patch(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void editAnswer_bodyTooLong_returns400() throws Exception {
        EditAnswerRequest req = makeEditRequest();
        req.setBody("A".repeat(2001));

        mockMvc.perform(patch(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ===================== UC-201: Delete Own Answer =====================

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void deleteAnswer_validRequest_returns204() throws Exception {
        mockMvc.perform(delete(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(answerService).deleteAnswer(eq(ANSWER_ID), any(), eq(false));
    }

    @Test
    void deleteAnswer_noJwt_returns401() throws Exception {
        mockMvc.perform(delete(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MODERATOR")
    void deleteAnswer_moderator_passesModeratorFlagTrue() throws Exception {
        mockMvc.perform(delete(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(answerService).deleteAnswer(eq(ANSWER_ID), any(), eq(true));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void deleteAnswer_nonOwner_returns403() throws Exception {
        org.mockito.Mockito.doThrow(new AccessDeniedException("You do not own this answer"))
                .when(answerService).deleteAnswer(eq(ANSWER_ID), any(), eq(false));

        mockMvc.perform(delete(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void deleteAnswer_answerNotFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new AnswerNotFoundException(ANSWER_ID.toString()))
                .when(answerService).deleteAnswer(eq(ANSWER_ID), any(), eq(false));

        mockMvc.perform(delete(ANSWER_URL, QUESTION_ID, ANSWER_ID).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
