package com.carebridge.backend.community.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.community.dto.response.QuestionLikeToggleResponse;
import com.carebridge.backend.community.service.CommunityQuestionLikeService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// COMQL-TC-005, COMQL-TC-008
@WebMvcTest(
        value = CommunityQuestionLikeController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CommunityQuestionLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityQuestionLikeService likeService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String MOTHER_UUID = "00000000-0000-0000-0000-000000000001";
    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String LIKE_URL = "/api/v1/community/questions/" + QUESTION_ID + "/like";

    // COMQL-TC-005: no JWT -> 401
    @Test
    void toggleLike_noJwt_returns401() throws Exception {
        mockMvc.perform(post(LIKE_URL))
                .andExpect(status().isUnauthorized());
    }

    // COMQL-TC-008: authenticated -> 200 with QuestionLikeToggleResponse body
    @Test
    @WithMockUser(username = MOTHER_UUID, roles = "MOTHER")
    void toggleLike_authenticated_returns200WithBody() throws Exception {
        QuestionLikeToggleResponse response = QuestionLikeToggleResponse.builder()
                .liked(true)
                .likeCount(3)
                .questionId(QUESTION_ID)
                .build();
        when(likeService.toggleLike(UUID.fromString(MOTHER_UUID), QUESTION_ID)).thenReturn(response);

        mockMvc.perform(post(LIKE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(3))
                .andExpect(jsonPath("$.data.questionId").value(QUESTION_ID.toString()));
    }
}
