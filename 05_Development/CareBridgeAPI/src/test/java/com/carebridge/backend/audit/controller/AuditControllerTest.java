package com.carebridge.backend.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.dto.response.AuditLogResponse;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * UC117 View Audit Logs — Track B (new meta-audit wiring, ADR-AUDIT-001) and a
 * subset of Track A (characterization of the existing search/auth behavior).
 */
@WebMvcTest(
        value = AuditController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/admin/audit-logs";
    private static final String ADMIN_ID = "00000000-0000-0000-0000-000000000001";

    // UC117-TC-001 (Track B)
    @Test
    @WithMockUser(username = ADMIN_ID, roles = "SYSTEM_ADMIN")
    void search_emitsExactlyOneViewAuditLogMetaAuditEntry() throws Exception {
        org.mockito.Mockito.when(auditService.search(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get(BASE_URL).param("action", "MODERATION_ACTION").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());

        verify(auditService, times(1)).log(
                eq(AuditAction.VIEW_AUDIT_LOG), any(UUID.class), eq("AuditLog"), isNull(), any());
    }

    // UC117-TC-002 (Track B)
    @Test
    @WithMockUser(username = ADMIN_ID, roles = "SYSTEM_ADMIN")
    void search_metaAuditPayload_containsOnlyFilterParams() throws Exception {
        UUID resultUserId = UUID.randomUUID();
        org.mockito.Mockito.when(auditService.search(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(
                        AuditLogResponse.builder()
                                .id(UUID.randomUUID())
                                .userId(resultUserId)
                                .action(AuditAction.MODERATION_ACTION)
                                .timestamp(Instant.now())
                                .build())));

        mockMvc.perform(get(BASE_URL)
                        .param("action", "MODERATION_ACTION")
                        .param("page", "0").param("size", "20"))
                .andExpect(status().isOk());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditService).log(eq(AuditAction.VIEW_AUDIT_LOG), any(UUID.class), eq("AuditLog"), isNull(),
                payloadCaptor.capture());
        String payloadStr = payloadCaptor.getValue().toString();
        org.assertj.core.api.Assertions.assertThat(payloadStr).doesNotContain(resultUserId.toString());
    }

    // UC117-TC-003 (Track B) — fail-soft
    @Test
    @WithMockUser(username = ADMIN_ID, roles = "SYSTEM_ADMIN")
    void search_metaAuditFailure_doesNotBlockReadResponse() throws Exception {
        org.mockito.Mockito.when(auditService.search(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(
                        AuditLogResponse.builder().id(UUID.randomUUID()).action(AuditAction.LOGIN).build())));
        doThrow(new RuntimeException("simulated audit write failure"))
                .when(auditService).log(eq(AuditAction.VIEW_AUDIT_LOG), any(UUID.class), eq("AuditLog"), isNull(), any());

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // UC117-TC-005 (Track A characterization) — filter delegation
    @Test
    @WithMockUser(username = ADMIN_ID, roles = "SYSTEM_ADMIN")
    void search_filtersByUserId_delegatesToService() throws Exception {
        UUID knownActorId = UUID.randomUUID();
        org.mockito.Mockito.when(auditService.search(eq(knownActorId), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(
                        AuditLogResponse.builder().id(UUID.randomUUID()).userId(knownActorId)
                                .action(AuditAction.LOGIN).timestamp(Instant.now()).build())));

        mockMvc.perform(get(BASE_URL).param("userId", knownActorId.toString()).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(knownActorId.toString()));
    }

    @Test
    @WithMockUser(username = ADMIN_ID, roles = "OPERATIONS")
    void search_operationsRole_isAllowedAndMetaAudited() throws Exception {
        org.mockito.Mockito.when(auditService.search(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());

        verify(auditService).log(
                eq(AuditAction.VIEW_AUDIT_LOG), any(UUID.class), eq("AuditLog"), isNull(), any());
    }

    // UC117-TC-009 (Track A characterization) — non-admin rejection
    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"SYSTEM_ADMIN", "OPERATIONS"}, mode = EnumSource.Mode.EXCLUDE)
    void search_nonSystemAdminRole_isRejected(Role role) throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(SecurityMockMvcRequestPostProcessors.user(UUID.randomUUID().toString()).roles(role.name())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(auditService);
    }

    // UC117-TC-012 (Track A characterization) — pagination cap
    @Test
    @WithMockUser(username = ADMIN_ID, roles = "SYSTEM_ADMIN")
    void search_pageSizeAbove100_isCapped() throws Exception {
        org.mockito.Mockito.when(auditService.search(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "101"))
                .andExpect(status().isOk());

        ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(auditService).search(any(), any(), any(), any(), pageableCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1&size=20", "page=0&size=0"})
    @WithMockUser(username = ADMIN_ID, roles = "SYSTEM_ADMIN")
    void search_rejectsInvalidPageOrSize(String query) throws Exception {
        mockMvc.perform(get(BASE_URL).queryParam("page", query.split("&")[0].substring(5))
                        .queryParam("size", query.split("&")[1].substring(5)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(auditService);
    }
}
