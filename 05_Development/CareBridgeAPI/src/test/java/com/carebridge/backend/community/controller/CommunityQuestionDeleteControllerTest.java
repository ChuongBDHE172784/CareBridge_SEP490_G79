package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.community.exception.QuestionLockedException;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.service.CommunityQuestionSearchService;
import com.carebridge.backend.community.service.CommunityQuestionService;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// UC-170: Delete Community Post
@WebMvcTest(
        value = CommunityQuestionController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CommunityQuestionDeleteControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CommunityQuestionService questionService;
    @MockitoBean CommunityQuestionSearchService searchService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserRepository userRepository;

    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String URL = "/api/v1/community/questions/{id}";

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "MOTHER")
    void deleteQuestion_validRequest_returns204() throws Exception {
        mockMvc.perform(delete(URL, QUESTION_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(questionService).deleteQuestion(eq(QUESTION_ID), any(), eq(false));
    }

    @Test
    void deleteQuestion_noJwt_returns401() throws Exception {
        mockMvc.perform(delete(URL, QUESTION_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "MODERATOR")
    void deleteQuestion_moderator_passesModeratorFlagTrue() throws Exception {
        mockMvc.perform(delete(URL, QUESTION_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(questionService).deleteQuestion(eq(QUESTION_ID), any(), eq(true));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "MOTHER")
    void deleteQuestion_nonOwner_returns403() throws Exception {
        doThrow(new AccessDeniedException("You do not own this question"))
                .when(questionService).deleteQuestion(eq(QUESTION_ID), any(), eq(false));

        mockMvc.perform(delete(URL, QUESTION_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "MOTHER")
    void deleteQuestion_locked_returns409() throws Exception {
        doThrow(new QuestionLockedException(QUESTION_ID.toString()))
                .when(questionService).deleteQuestion(eq(QUESTION_ID), any(), eq(false));

        mockMvc.perform(delete(URL, QUESTION_ID).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("COM-012"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "MOTHER")
    void deleteQuestion_notFound_returns404() throws Exception {
        doThrow(new QuestionNotFoundException(QUESTION_ID.toString()))
                .when(questionService).deleteQuestion(eq(QUESTION_ID), any(), eq(false));

        mockMvc.perform(delete(URL, QUESTION_ID).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
