package com.carebridge.backend.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.SecurityIncidentService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * UC174 (SEC174-TC-005/006) + UC175 (SEC175-TC-005) — gap-check tests: RBAC across
 * all endpoints on SecurityIncidentController, plus the new meta-audit wiring.
 */
@WebMvcTest(
        value = SecurityIncidentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class SecurityIncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityIncidentService securityIncidentService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/admin/security-events";
    private static final String ADMIN_ID = "00000000-0000-0000-0000-000000000001";

    // SEC174-TC-005 / SEC175-TC-005 — all endpoints RBAC
    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"SYSTEM_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void searchEvents_nonAdminRole_isRejected(Role role) throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(SecurityMockMvcRequestPostProcessors.user(UUID.randomUUID().toString()).roles(role.name())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(securityIncidentService);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"SYSTEM_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void reviewEvent_nonAdminRole_isRejected(Role role) throws Exception {
        mockMvc.perform(put(BASE_URL + "/1/review")
                        .with(SecurityMockMvcRequestPostProcessors.user(UUID.randomUUID().toString()).roles(role.name()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(securityIncidentService);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"SYSTEM_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void addNote_nonAdminRole_isRejected(Role role) throws Exception {
        mockMvc.perform(post(BASE_URL + "/1/notes")
                        .with(SecurityMockMvcRequestPostProcessors.user(UUID.randomUUID().toString()).roles(role.name()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noteText\":\"x\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(securityIncidentService);
    }

    // SEC174-TC-006 — meta-audit wiring
    @Test
    @WithMockUser(username = ADMIN_ID, roles = "SYSTEM_ADMIN")
    void searchEvents_emitsMetaAuditEntry() throws Exception {
        org.mockito.Mockito.when(securityIncidentService.searchEvents(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "10"))
                .andExpect(status().isOk());

        verify(auditService, times(1)).log(
                eq(AuditAction.VIEW_AUDIT_LOG), any(UUID.class), eq("SECURITY_EVENT_QUERY"), isNull(), any());
    }

    @Test
    @WithMockUser(username = ADMIN_ID, roles = "SYSTEM_ADMIN")
    void searchEvents_asSystemAdmin_returns200() throws Exception {
        org.mockito.Mockito.when(securityIncidentService.searchEvents(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }
}
