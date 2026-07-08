package com.carebridge.backend.content;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.AdminContentController;
import com.carebridge.backend.content.service.AdminContentService;
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

// UCT-TC-1204
@WebMvcTest(
        value = AdminContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class HideContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminContentService adminContentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID D1 = UUID.fromString("f1700000-0000-0000-0000-000000000001");

    private static String url() {
        return "/api/v1/admin/content/" + D1 + "/archive";
    }

    // UCT-TC-1204a: empty reason -> 400
    @Test
    @WithMockUser(username = "f1700000-0000-0000-0000-0000000000ad", roles = "CONTENT_ADMIN")
    void hideContent_emptyReason_shouldReturn400() throws Exception {
        mockMvc.perform(post(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // UCT-TC-1204b: null reason -> 400
    @Test
    @WithMockUser(username = "f1700000-0000-0000-0000-0000000000ad", roles = "CONTENT_ADMIN")
    void hideContent_nullReason_shouldReturn400() throws Exception {
        mockMvc.perform(post(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // UCT-TC-1204c: whitespace-only reason -> 400
    @Test
    @WithMockUser(username = "f1700000-0000-0000-0000-0000000000ad", roles = "CONTENT_ADMIN")
    void hideContent_whitespaceReason_shouldReturn400() throws Exception {
        mockMvc.perform(post(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
