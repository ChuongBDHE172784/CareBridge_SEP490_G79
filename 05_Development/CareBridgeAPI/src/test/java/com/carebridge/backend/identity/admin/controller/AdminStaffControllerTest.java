package com.carebridge.backend.identity.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.identity.admin.dto.response.StaffAccountResponse;
import com.carebridge.backend.identity.admin.service.AdminStaffService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * UC115 Create Staff Account — controller RBAC tests.
 * Covers UC115-TC-SEC-001 (structural self-escalation prevention) and a happy-path smoke test.
 */
@WebMvcTest(
        value = AdminStaffController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminStaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStaffService adminStaffService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/admin/staff-accounts";
    private static final String BODY = "{\"email\":\"new.mod@carebridge.dev\",\"name\":\"New Mod\",\"role\":\"MODERATOR\"}";

    // UC115-TC-SEC-001
    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"SYSTEM_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void createStaffAccount_nonSystemAdminRole_isRejectedBeforeServiceLayer(Role role) throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(SecurityMockMvcRequestPostProcessors.user(UUID.randomUUID().toString()).roles(role.name()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminStaffService);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SYSTEM_ADMIN")
    void createStaffAccount_asSystemAdmin_returns201() throws Exception {
        org.mockito.Mockito.when(adminStaffService.createStaffAccount(any(), any()))
                .thenReturn(StaffAccountResponse.builder()
                        .id(UUID.randomUUID())
                        .email("new.mod@carebridge.dev")
                        .name("New Mod")
                        .role(Role.MODERATOR)
                        .mustChangePassword(true)
                        .createdAt(Instant.now())
                        .build());

        mockMvc.perform(post(BASE_URL)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("MODERATOR"))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SYSTEM_ADMIN")
    void createStaffAccount_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"name\":\"X\",\"role\":\"MODERATOR\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminStaffService);
    }
}
