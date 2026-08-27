package com.carebridge.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.dto.response.ModerateContentResponse;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportTargetType;
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

// MOD-TC-INT-001: Full HTTP stack integration — Security -> Controller -> Service (mocked persistence).
// Note: this codebase has no Testcontainers/real-DB integration harness (verified — zero usages
// project-wide); this test follows the existing project convention (see ModerationQueueIntegrationTest,
// ContentIntegrationTest) of exercising the full Spring MVC + Security filter chain against a mocked
// service layer, rather than a real Postgres instance. DB-level atomicity (MOD-TC-INT-002) is verified
// at the service unit-test level instead — see ModerateContentServiceImplTest.
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ModerateContentIntegrationTest {

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
    private static final UUID MODERATOR_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    // MOD-TC-INT-001: full API flow — POST returns 201 with resultingStatus reflecting the applied action
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MODERATOR")
    void moderateContent_approveQuestion_fullStackReturns201WithUpdatedStatus() throws Exception {
        ModerateContentResponse response = new ModerateContentResponse(
                UUID.randomUUID(), QUESTION_ID, ReportTargetType.QUESTION, ModerationActionType.APPROVE,
                MODERATOR_ID, null, Instant.now(), "APPROVED");
        when(moderationService.moderateContent(any(), any())).thenReturn(response);

        String body = "{\"targetId\":\"" + QUESTION_ID
                + "\",\"targetType\":\"QUESTION\",\"actionType\":\"APPROVE\"}";

        mockMvc.perform(post(ACTIONS_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetId").value(QUESTION_ID.toString()))
                .andExpect(jsonPath("$.targetType").value("QUESTION"))
                .andExpect(jsonPath("$.actionType").value("APPROVE"))
                .andExpect(jsonPath("$.resultingStatus").value("APPROVED"));
    }
}
