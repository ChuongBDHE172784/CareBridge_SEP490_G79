package com.carebridge.backend.moderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.service.ModerationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.UUID;
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

// MOD-TC-116, MOD-TC-117
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ModerateContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String ACTIONS_URL = "/api/v1/admin/moderation/actions";
    private static final UUID QUESTION_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    // MOD-TC-116: missing required field (targetType=null) → 400 (exact error.code is Open per TDS §9.2 —
    // asserting status + field reference as the stable minimum oracle, per Logic Issue guidance)
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MODERATOR")
    void moderateContent_missingTargetType_shouldReturn400() throws Exception {
        String bodyMissingTargetType = "{\"targetId\":\"" + QUESTION_ID + "\",\"actionType\":\"APPROVE\"}";

        mockMvc.perform(post(ACTIONS_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyMissingTargetType))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("targetType"));
    }

    // MOD-TC-117: unexpected exception → 500 INTERNAL_ERROR (NOT dead-code MOD-005 — regression guard)
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MODERATOR")
    void moderateContent_unexpectedException_shouldReturn500WithInternalError() throws Exception {
        when(moderationService.moderateContent(any(), any()))
                .thenThrow(new RuntimeException("simulated failure"));

        String validBody = "{\"targetId\":\"" + QUESTION_ID
                + "\",\"targetType\":\"QUESTION\",\"actionType\":\"APPROVE\"}";

        mockMvc.perform(post(ACTIONS_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }
}
