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

// WSA-TC-214, WSA-TC-215
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class WarnOrSuspendAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String URL = "/api/v1/admin/moderation/account-actions";
    private static final UUID TARGET_USER_ID = UUID.fromString("bb000000-0000-0000-0000-000000000001");

    // WSA-TC-214a: missing targetUserId -> 400
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-0000000000f2", roles = "MODERATOR")
    void moderateAccount_missingTargetUserId_shouldReturn400() throws Exception {
        String body = "{\"actionType\":\"WARN\",\"reason\":\"test\"}";

        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("targetUserId"));
    }

    // WSA-TC-214b: missing actionType -> 400
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-0000000000f2", roles = "MODERATOR")
    void moderateAccount_missingActionType_shouldReturn400() throws Exception {
        String body = "{\"targetUserId\":\"" + TARGET_USER_ID + "\",\"reason\":\"test\"}";

        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("actionType"));
    }

    // WSA-TC-215: unexpected exception -> 500 INTERNAL_ERROR (not dead-code MOD-005)
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-0000000000f2", roles = "MODERATOR")
    void moderateAccount_unexpectedException_shouldReturn500WithInternalError() throws Exception {
        when(moderationService.moderateAccount(any(), any()))
                .thenThrow(new RuntimeException("simulated failure"));

        String body = "{\"targetUserId\":\"" + TARGET_USER_ID + "\",\"actionType\":\"WARN\",\"reason\":\"test\"}";

        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }
}
