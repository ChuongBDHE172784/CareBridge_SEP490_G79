package com.carebridge.backend.family.controller;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.family.service.ICareGroupService;
import com.carebridge.backend.family.service.ICareTaskService;
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

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CareGroupController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CareGroupControllerPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ICareGroupService careGroupService;
    @MockitoBean private ICareTaskService careTaskService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final UUID GROUP_ID  = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final String PERMISSIONS_URL =
            "/api/v1/care-groups/" + GROUP_ID + "/members/" + MEMBER_ID + "/permissions";

    // ── TC-015: Unauthenticated caller → 401 ─────────────────────────────────

    @Test
    void updateFamilyPermission_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch(PERMISSIONS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendar\": true}"))
                .andExpect(status().isUnauthorized());
    }

    // ── TC-017: Injection-style string in flag field → 400 (type mismatch) ───

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000011", roles = "MOTHER")
    void updateFamilyPermission_injectionStringInBooleanField_returns400() throws Exception {
        mockMvc.perform(patch(PERMISSIONS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendar\": \"'; DROP TABLE care_group_members; --\"}"))
                .andExpect(status().isBadRequest());
    }
}
