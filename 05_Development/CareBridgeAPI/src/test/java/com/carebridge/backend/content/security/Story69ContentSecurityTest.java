package com.carebridge.backend.content.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.checklist.controller.UserChecklistItemController;
import com.carebridge.backend.checklist.service.IUserChecklistItemService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.AdminContentController;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.dto.response.LifecycleContentEnvelope;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.service.AdminContentService;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Real MockMvc method-security matrix for Story 6.9 SEC-001/002. */
@WebMvcTest(
        value = {ContentController.class, AdminContentController.class, UserChecklistItemController.class},
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class Story69ContentSecurityTest {

    private static final String USER_ID = "69000000-0000-0000-0000-000000000001";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ContentService contentService;
    @MockitoBean private AdminContentService adminContentService;
    @MockitoBean private IUserChecklistItemService checklistService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @ParameterizedTest
    @MethodSource("securedRequests")
    void uc82_69_sec_001_everyStoryRouteRejectsMissingJwt(String method, String path) throws Exception {
        if ("POST".equals(method)) {
            mockMvc.perform(post(path).with(csrf())
                            .contentType("application/json")
                            .content(validImportJson()))
                    .andExpect(status().isUnauthorized());
        } else {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @ParameterizedTest
    @MethodSource("nonMotherRoles")
    void uc82_69_sec_002_nonMotherRolesCannotUseLifecycleOrImport(String role) throws Exception {
        mockMvc.perform(get("/api/v1/content/lifecycle")
                        .with(user(USER_ID).roles(role)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/content/lifecycle/checklists")
                        .with(user(USER_ID).roles(role)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/user-checklist-items/import")
                        .with(csrf()).with(user(USER_ID).roles(role))
                        .contentType("application/json")
                        .content(validImportJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void uc82_69_sec_002_motherCanUseLifecycleAndImport() throws Exception {
        when(contentService.getLifecycleContents(any(), any(), any(), any()))
                .thenReturn(new LifecycleContentEnvelope<>(ContentStage.PRE_PREGNANCY, Page.empty()));
        when(contentService.getLifecycleChecklists(any()))
                .thenReturn(new LifecycleContentEnvelope<>(ContentStage.PRE_PREGNANCY, List.of()));
        when(checklistService.importFromTemplate(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/content/lifecycle")
                        .with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/content/lifecycle/checklists")
                        .with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/user-checklist-items/import")
                        .with(csrf()).with(user(USER_ID).roles("MOTHER"))
                        .contentType("application/json")
                        .content(validImportJson()))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @MethodSource("nonAdminRoles")
    void uc82_69_sec_002_nonAdminRolesCannotListAdminChecklists(String role) throws Exception {
        mockMvc.perform(get("/api/v1/admin/content/checklists")
                        .with(user(USER_ID).roles(role)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @MethodSource("adminRoles")
    void uc82_69_sec_002_onlyContentAndSystemAdminCanListAdminChecklists(String role)
            throws Exception {
        when(contentService.getAdminChecklists(any(), any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/v1/admin/content/checklists")
                        .with(user(USER_ID).roles(role)))
                .andExpect(status().isOk());
    }

    static Stream<Arguments> securedRequests() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/content/lifecycle"),
                Arguments.of("GET", "/api/v1/content/lifecycle/checklists"),
                Arguments.of("GET", "/api/v1/content/lifecycle/69000000-0000-0000-0000-000000000099"),
                Arguments.of("POST", "/api/v1/user-checklist-items/import"),
                Arguments.of("GET", "/api/v1/admin/content/checklists"));
    }

    static Stream<String> nonMotherRoles() {
        return Stream.of("FAMILY", "EXPERT", "MODERATOR", "CONTENT_ADMIN", "SYSTEM_ADMIN", "PARTNER");
    }

    static Stream<String> nonAdminRoles() {
        return Stream.of("MOTHER", "FAMILY", "EXPERT", "MODERATOR", "PARTNER");
    }

    static Stream<String> adminRoles() {
        return Stream.of("CONTENT_ADMIN", "SYSTEM_ADMIN");
    }

    private String validImportJson() {
        return "{\"journeyId\":\"69000000-0000-0000-0000-000000000010\","
                + "\"templateItemIds\":[\"69000000-0000-0000-0000-000000000020\"]}";
    }
}
