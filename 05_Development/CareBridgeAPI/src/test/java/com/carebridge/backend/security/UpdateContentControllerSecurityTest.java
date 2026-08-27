package com.carebridge.backend.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.AdminContentController;
import com.carebridge.backend.content.service.AdminContentService;
import com.carebridge.backend.content.service.ContentService;
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

// UCT-TC-910, UCT-TC-911
@WebMvcTest(
        value = AdminContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class UpdateContentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminContentService adminContentService;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID C1 = UUID.fromString("f1500000-0000-0000-0000-000000000001");

    private static String url() {
        return "/api/v1/admin/content/" + C1;
    }

    private static String validBody() {
        return "{\"title\":\"A\",\"body\":\"x\",\"stage\":\"PREGNANCY\",\"status\":\"APPROVED\"}";
    }

    // UCT-TC-910: Non-CONTENT_ADMIN -> 403. Body is empty — URL-matcher denial precedes DispatcherServlet
    // (same verified finding as UC-100/101/102 and this controller's own createContent() precedent test).
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000099", roles = "MODERATOR")
    void updateContent_asModeratorRole_shouldReturn403() throws Exception {
        mockMvc.perform(put(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    // UCT-TC-911: No JWT -> 401
    @Test
    void updateContent_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(put(url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }
}
