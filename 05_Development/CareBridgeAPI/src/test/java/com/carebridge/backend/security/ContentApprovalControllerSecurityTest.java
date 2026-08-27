package com.carebridge.backend.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ContentApprovalController;
import com.carebridge.backend.content.service.ContentApprovalService;
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

// CAV-TC-1008, CAV-TC-1009, CAV-TC-1010
@WebMvcTest(
        value = ContentApprovalController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ContentApprovalControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentApprovalService contentApprovalService;

    @MockitoBean
    private com.carebridge.backend.content.service.ExpertContentApprovalService expertContentApprovalService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID CONTENT_ID = UUID.fromString("f1700000-0000-0000-0000-000000000001");
    private static final UUID AUTHOR_ID = UUID.fromString("f1600000-0000-0000-0000-0000000000a1");

    private static String url() {
        return "/api/v1/admin/content/" + CONTENT_ID + "/decision";
    }

    private static String approveBody() {
        return "{\"decision\":\"APPROVE\"}";
    }

    // CAV-TC-1008: generic non-SYSTEM_ADMIN role -> 403. Body is empty — URL-matcher denial precedes
    // DispatcherServlet (same verified finding as UC-100/101/102/106/107).
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000099", roles = "MOTHER")
    void decide_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody()))
                .andExpect(status().isForbidden());
    }

    // CAV-TC-1009: No JWT -> 401
    @Test
    void decide_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(post(url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody()))
                .andExpect(status().isUnauthorized());
    }

    // CAV-TC-1010 (CRITICAL — separation of duties): CONTENT_ADMIN, including the item's own author,
    // is never in the allowed-role set for this endpoint -> 403. Structural guarantee, not an ownership
    // check — CONTENT_ADMIN never reaches this endpoint regardless of authorship.
    @Test
    @WithMockUser(username = "f1600000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    void decide_asAuthoringContentAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "e1000000-0000-0000-0000-000000000001", roles = "EXPERT")
    void decide_asExpertRole_shouldReturn200() throws Exception {
        mockMvc.perform(post(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody()))
                .andExpect(status().isOk());
    }
}
