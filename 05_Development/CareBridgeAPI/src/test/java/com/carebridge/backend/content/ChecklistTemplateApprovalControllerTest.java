package com.carebridge.backend.content;

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
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ChecklistTemplateApprovalController;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.service.ChecklistTemplateApprovalService;
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

// CHKTPL-TC-013 (decision part) — only SYSTEM_ADMIN may decide
@WebMvcTest(
        value = ChecklistTemplateApprovalController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ChecklistTemplateApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChecklistTemplateApprovalService checklistTemplateApprovalService;

    @MockitoBean
    private com.carebridge.backend.content.service.ExpertContentApprovalService expertContentApprovalService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static String url() {
        return "/api/v1/admin/checklist-templates/" + ID + "/decision";
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "CONTENT_ADMIN")
    void decide_asContentAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());

        verify(checklistTemplateApprovalService, never()).decide(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void decide_asMother_shouldReturn403() throws Exception {
        mockMvc.perform(post(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void decide_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post(url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SYSTEM_ADMIN")
    void decide_checklistConflictSerializesStableReasonMetadata() throws Exception {
        when(checklistTemplateApprovalService.decide(eq(ID), any(), any()))
                .thenThrow(ContentException.checklistValidationFailed(
                        "displayOrder", "active legacy candidate", "CHECKLIST_ACTIVE_LEGACY_CONFLICT"));

        mockMvc.perform(post(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CNT-001"))
                .andExpect(jsonPath("$.metadata.reasonCode")
                        .value("CHECKLIST_ACTIVE_LEGACY_CONFLICT"));
    }
}
