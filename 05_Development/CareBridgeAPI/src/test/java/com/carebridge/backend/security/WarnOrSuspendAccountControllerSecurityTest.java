package com.carebridge.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.dto.response.WarnOrSuspendAccountResponse;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.service.ModerationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
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

// WSA-TC-216, WSA-TC-217, WSA-TC-218, WSA-TC-219
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class WarnOrSuspendAccountControllerSecurityTest {

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

    private static String warnBody() {
        return "{\"targetUserId\":\"" + TARGET_USER_ID + "\",\"actionType\":\"WARN\",\"reason\":\"test\"}";
    }

    // WSA-TC-216: Non-MODERATOR -> 403 (body empty — URL-matcher denial, same as UC-100/101)
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000099", roles = "MOTHER")
    void moderateAccount_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(warnBody()))
                .andExpect(status().isForbidden());
    }

    // WSA-TC-217: SYSTEM_ADMIN has no implicit access — also 403
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000098", roles = "SYSTEM_ADMIN")
    void moderateAccount_asSystemAdminRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(warnBody()))
                .andExpect(status().isForbidden());
    }

    // WSA-TC-218: No JWT -> 401 (bodiless)
    @Test
    void moderateAccount_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(warnBody()))
                .andExpect(status().isUnauthorized());
    }

    // WSA-TC-219: SQL injection in reason field handled as literal text
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MODERATOR")
    void moderateAccount_sqlInjectionInReason_handledAsLiteralText() throws Exception {
        String maliciousReason = "x'; DROP TABLE users;--";
        when(moderationService.moderateAccount(any(), any())).thenReturn(new WarnOrSuspendAccountResponse(
                UUID.randomUUID(), TARGET_USER_ID, ModerationActionType.WARN,
                UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"), maliciousReason, Instant.now(), null,
                false));

        String body = "{\"targetUserId\":\"" + TARGET_USER_ID + "\",\"actionType\":\"WARN\",\"reason\":\""
                + maliciousReason + "\"}";

        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountSuspended").value(false));
    }
}
