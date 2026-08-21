package com.carebridge.backend.directchat.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.directchat.service.IAdminConsultationCallService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = AdminConsultationCallController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminConsultationCallControllerSecurityTest {

    private static final UUID CALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private IAdminConsultationCallService adminCallService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @ParameterizedTest
    @ValueSource(strings = {"SYSTEM_ADMIN", "ADMIN"})
    void deleteRecording_authorizedAdminReturnsNoContent(String role) throws Exception {
        mockMvc.perform(delete("/api/v1/admin/consultation-calls/{callId}/recording", CALL_ID)
                        .with(csrf())
                        .with(user(ADMIN_ID.toString()).roles(role)))
                .andExpect(status().isNoContent());

        verify(adminCallService).deleteRecording(CALL_ID, ADMIN_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MODERATOR", "CONTENT_ADMIN", "EXPERT", "MOTHER", "FAMILY"})
    void deleteRecording_nonAdminRoleIsForbidden(String role) throws Exception {
        mockMvc.perform(delete("/api/v1/admin/consultation-calls/{callId}/recording", CALL_ID)
                        .with(csrf())
                        .with(user(ADMIN_ID.toString()).roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminCallService);
    }

    @Test
    void deleteRecording_withoutAuthenticationIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/consultation-calls/{callId}/recording", CALL_ID)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminCallService);
    }
}
