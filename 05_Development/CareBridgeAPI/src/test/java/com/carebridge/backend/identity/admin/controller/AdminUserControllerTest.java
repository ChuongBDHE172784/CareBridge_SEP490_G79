package com.carebridge.backend.identity.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.identity.admin.dto.response.AdminUserSummaryResponse;
import com.carebridge.backend.identity.admin.service.AdminUserService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * UC114 Manage User Accounts — controller RBAC/validation tests.
 * Covers TC-008, TC-009, TC-011.
 */
@WebMvcTest(
        value = AdminUserController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/admin/users";

    // UC114-TC-008
    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"SYSTEM_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void searchUsers_nonAdminRole_isRejected(Role role) throws Exception {
        mockMvc.perform(get(BASE_URL).with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user(UUID.randomUUID().toString()).roles(role.name())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminUserService);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SYSTEM_ADMIN")
    void searchUsers_asSystemAdmin_returns200() throws Exception {
        org.mockito.Mockito.when(adminUserService.searchUsers(any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(
                        AdminUserSummaryResponse.builder()
                                .id(UUID.randomUUID())
                                .email("mother1@example.com")
                                .role(Role.MOTHER)
                                .enabled(true)
                                .createdAt(Instant.now())
                                .build())));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value("mother1@example.com"));
    }

    // UC114-TC-009
    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"SYSTEM_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void updateStatus_nonAdminRole_isRejected(Role role) throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + UUID.randomUUID() + "/status")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user(UUID.randomUUID().toString()).roles(role.name()))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminUserService);
    }

    // UC114-TC-011
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SYSTEM_ADMIN")
    void updateStatus_reasonExceeds500Chars_returns400() throws Exception {
        String reason501 = "a".repeat(501);
        mockMvc.perform(patch(BASE_URL + "/" + UUID.randomUUID() + "/status")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false, \"reason\": \"" + reason501 + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminUserService);
    }
}
