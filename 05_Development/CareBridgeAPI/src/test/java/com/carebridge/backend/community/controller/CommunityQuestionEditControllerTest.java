package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.service.CommunityQuestionSearchService;
import com.carebridge.backend.community.service.CommunityQuestionService;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CommunityQuestionController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CommunityQuestionEditControllerTest {

    @Autowired MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean CommunityQuestionService questionService;
    @MockitoBean CommunityQuestionSearchService searchService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserRepository userRepository;

    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String USER_UUID = "00000000-0000-0000-0000-000000000099";

    @Test
    @WithMockUser(username = USER_UUID, roles = "MOTHER")
    void editQuestion_validRequest_returns200() throws Exception {
        CommunityQuestionResponse response = CommunityQuestionResponse.builder()
                .id(QUESTION_ID).title("Updated title").build();
        when(questionService.editQuestion(any(), eq(QUESTION_ID), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/community/questions/" + QUESTION_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated title\",\"body\":\"Updated body content here\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated title"));
    }

    @Test
    @WithMockUser(username = USER_UUID, roles = "MOTHER")
    void editQuestion_questionNotFound_returns404() throws Exception {
        when(questionService.editQuestion(any(), eq(QUESTION_ID), any()))
                .thenThrow(new QuestionNotFoundException(QUESTION_ID.toString()));

        mockMvc.perform(patch("/api/v1/community/questions/" + QUESTION_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated title\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void editQuestion_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/community/questions/" + QUESTION_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated title\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_UUID, roles = "MOTHER")
    void editQuestion_moreThanThreeImages_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/community/questions/" + QUESTION_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageUrls":[
                          "https://res.cloudinary.com/demo/image/upload/1.jpg",
                          "https://res.cloudinary.com/demo/image/upload/2.jpg",
                          "https://res.cloudinary.com/demo/image/upload/3.jpg",
                          "https://res.cloudinary.com/demo/image/upload/4.jpg"
                        ]}
                        """))
                .andExpect(status().isBadRequest());
    }
}
