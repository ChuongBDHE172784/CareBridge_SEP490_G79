package com.carebridge.backend.exercise;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.exercise.controller.AdminPostureConfigController;
import com.carebridge.backend.exercise.dto.AdminPostureConfigResponse;
import com.carebridge.backend.exercise.service.IPostureConfigService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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
        value = AdminPostureConfigController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminPostureConfigControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IPostureConfigService postureConfigService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID EXERCISE_ID = UUID.randomUUID();
    private static final UUID CONFIG_ID = UUID.randomUUID();

    private static final String CREATE_BODY = """
            {"exerciseId":"%s","analysisMode":"MODEL_BASED","confidenceThreshold":0.75,"feedbackLevel":"DETAILED"}
            """.formatted(EXERCISE_ID);

    private static final String VERSION_BODY = """
            {"analysisMode":"HYBRID","confidenceThreshold":0.8,"feedbackLevel":"DETAILED"}
            """;

    private void stubHappyPath() {
        AdminPostureConfigResponse response = AdminPostureConfigResponse.builder()
                .postureConfigId(CONFIG_ID)
                .exerciseId(EXERCISE_ID)
                .status("ACTIVE")
                .build();
        when(postureConfigService.createConfig(any(), any())).thenReturn(ApiResponse.success(response));
        when(postureConfigService.createNewVersion(any(), any(), any())).thenReturn(ApiResponse.success(response));
        when(postureConfigService.activateVersion(any(), any())).thenReturn(ApiResponse.success(response));
        when(postureConfigService.listVersions(any())).thenReturn(ApiResponse.success(List.of(response)));
    }

    // === PAC-TC-SEC-001 — CONTENT_ADMIN denied (regression guard for ADR-PAC-001) ===
    @Test
    @WithMockUser(roles = "CONTENT_ADMIN")
    @DisplayName("PAC-TC-SEC-001: CONTENT_ADMIN denied on createConfig")
    void createConfig_contentAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/posture-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CONTENT_ADMIN")
    @DisplayName("PAC-TC-SEC-001: CONTENT_ADMIN denied on createNewVersion")
    void createNewVersion_contentAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/posture-configs/" + EXERCISE_ID + "/versions")
                        .contentType(MediaType.APPLICATION_JSON).content(VERSION_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CONTENT_ADMIN")
    @DisplayName("PAC-TC-SEC-001: CONTENT_ADMIN denied on activateVersion")
    void activateVersion_contentAdmin_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/posture-configs/" + CONFIG_ID + "/activate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CONTENT_ADMIN")
    @DisplayName("PAC-TC-SEC-001: CONTENT_ADMIN denied on listVersions")
    void listVersions_contentAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/posture-configs/" + EXERCISE_ID))
                .andExpect(status().isForbidden());
    }

    // === PAC-TC-SEC-002 — MOTHER denied ===
    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("PAC-TC-SEC-002: MOTHER denied on createConfig")
    void createConfig_mother_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/posture-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    // === PAC-TC-SEC-003 — no JWT ===
    @Test
    @DisplayName("PAC-TC-SEC-003: no JWT returns 401 on createConfig")
    void createConfig_noJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/posture-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PAC-TC-SEC-003: no JWT returns 401 on listVersions")
    void listVersions_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/posture-configs/" + EXERCISE_ID))
                .andExpect(status().isUnauthorized());
    }

    // === PAC-TC-SEC-004 — SYSTEM_ADMIN succeeds (positive control) ===
    @Test
    @DisplayName("PAC-TC-SEC-004: SYSTEM_ADMIN succeeds on createConfig")
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "SYSTEM_ADMIN")
    void createConfig_systemAdmin_returns201() throws Exception {
        stubHappyPath();
        mockMvc.perform(post("/api/v1/admin/posture-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "SYSTEM_ADMIN")
    @DisplayName("PAC-TC-SEC-004: SYSTEM_ADMIN succeeds on createNewVersion")
    void createNewVersion_systemAdmin_returns201() throws Exception {
        stubHappyPath();
        mockMvc.perform(post("/api/v1/admin/posture-configs/" + EXERCISE_ID + "/versions")
                        .contentType(MediaType.APPLICATION_JSON).content(VERSION_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "SYSTEM_ADMIN")
    @DisplayName("PAC-TC-SEC-004: SYSTEM_ADMIN succeeds on activateVersion")
    void activateVersion_systemAdmin_returns200() throws Exception {
        stubHappyPath();
        mockMvc.perform(patch("/api/v1/admin/posture-configs/" + CONFIG_ID + "/activate"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "SYSTEM_ADMIN")
    @DisplayName("PAC-TC-SEC-004: SYSTEM_ADMIN succeeds on listVersions")
    void listVersions_systemAdmin_returns200() throws Exception {
        stubHappyPath();
        mockMvc.perform(get("/api/v1/admin/posture-configs/" + EXERCISE_ID))
                .andExpect(status().isOk());
    }
}
