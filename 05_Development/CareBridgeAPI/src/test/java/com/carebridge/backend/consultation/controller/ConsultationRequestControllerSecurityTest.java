package com.carebridge.backend.consultation.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.consultation.service.IConsultationRequestService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
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

@WebMvcTest(
        value = ConsultationRequestController.class,
        excludeFilters = @Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ConsultationRequestControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private IConsultationRequestService service;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    @WithMockUser(
            username = "00000000-0000-0000-0000-000000000202",
            roles = "EXPERT")
    void expertCannotCreateConsultationRequest() throws Exception {
        mockMvc.perform(post("/api/v1/consultation-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientRequestId":"00000000-0000-0000-0000-000000000301",
                                  "expertProfileId":"00000000-0000-0000-0000-000000000201",
                                  "topic":"Nutrition",
                                  "description":"Please advise"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCallerCannotCreateConsultationRequest() throws Exception {
        mockMvc.perform(post("/api/v1/consultation-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
