package com.carebridge.backend.content;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

// UCT-TC-908
@WebMvcTest(
        value = AdminContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class UpdateContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminContentService adminContentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID C1 = UUID.fromString("f1500000-0000-0000-0000-000000000001");

    private static String url() {
        return "/api/v1/admin/content/" + C1;
    }

    // UCT-TC-908a: blank title -> 400
    @Test
    @WithMockUser(username = "f1400000-0000-0000-0000-0000000000ad", roles = "CONTENT_ADMIN")
    void updateContent_blankTitle_shouldReturn400() throws Exception {
        String body = "{\"title\":\"\",\"body\":\"x\",\"stage\":\"PREGNANCY\",\"status\":\"APPROVED\"}";

        mockMvc.perform(put(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // UCT-TC-908b: oversized body -> 400
    @Test
    @WithMockUser(username = "f1400000-0000-0000-0000-0000000000ad", roles = "CONTENT_ADMIN")
    void updateContent_oversizedBody_shouldReturn400() throws Exception {
        String oversized = "x".repeat(50001);
        String body = "{\"title\":\"A\",\"body\":\"" + oversized + "\",\"stage\":\"PREGNANCY\",\"status\":\"APPROVED\"}";

        mockMvc.perform(put(url()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
