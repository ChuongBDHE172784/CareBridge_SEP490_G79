package com.carebridge.backend.identity.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.identity.admin.dto.response.UserRoleResponse;
import com.carebridge.backend.identity.admin.service.AdminRoleService;
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
 * UC116 Update Role and Permission — controller RBAC/validation tests.
 * Covers UC116-TC-SEC-002 (non-admin caller rejection) and UC116-TC-005/TC-008 (validation).
 */
@WebMvcTest(
        value = AdminRoleController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRoleService adminRoleService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    // UC116-TC-SEC-002
    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"SYSTEM_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void updateRole_nonSystemAdminRole_isRejected(Role role) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + UUID.randomUUID() + "/role")
                        .with(SecurityMockMvcRequestPostProcessors.user(UUID.randomUUID().toString()).roles(role.name()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newRole\": \"CONTENT_ADMIN\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminRoleService);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SYSTEM_ADMIN")
    void updateRole_asSystemAdmin_returns200() throws Exception {
        org.mockito.Mockito.when(adminRoleService.updateRole(any(), any(), any()))
                .thenReturn(UserRoleResponse.builder()
                        .id(UUID.randomUUID())
                        .previousRole(Role.MODERATOR)
                        .newRole(Role.CONTENT_ADMIN)
                        .locked(false)
                        .updatedAt(Instant.now())
                        .build());

        mockMvc.perform(patch("/api/v1/admin/users/" + UUID.randomUUID() + "/role")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newRole\": \"CONTENT_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newRole").value("CONTENT_ADMIN"));
    }

    // UC116-TC-005
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SYSTEM_ADMIN")
    void updateRole_missingNewRole_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + UUID.randomUUID() + "/role")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminRoleService);
    }

    // UC116-TC-008
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SYSTEM_ADMIN")
    void updateRole_reasonExceeds500Chars_returns400() throws Exception {
        String reason501 = "a".repeat(501);
        mockMvc.perform(patch("/api/v1/admin/users/" + UUID.randomUUID() + "/role")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newRole\": \"CONTENT_ADMIN\", \"reason\": \"" + reason501 + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
