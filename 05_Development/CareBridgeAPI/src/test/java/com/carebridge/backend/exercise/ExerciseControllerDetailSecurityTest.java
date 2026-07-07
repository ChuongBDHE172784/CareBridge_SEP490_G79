package com.carebridge.backend.exercise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.exercise.controller.ExerciseController;
import com.carebridge.backend.exercise.service.IExerciseDetailQueryService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = ExerciseController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ExerciseControllerDetailSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IExerciseDetailQueryService exerciseDetailQueryService;

    @MockitoBean
    private com.carebridge.backend.exercise.service.IExerciseQueryService exerciseQueryService;

    @MockitoBean
    private com.carebridge.backend.exercise.service.IExerciseSafetyCheckService safetyCheckService;

    @MockitoBean
    private com.carebridge.backend.exercise.service.IExerciseSessionService sessionService;

    @MockitoBean
    private com.carebridge.backend.exercise.service.IPostureConfigService postureConfigService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String DETAIL_URL = "/api/v1/exercises/" + ExerciseDetailTestFactory.UUID_PUBLISHED_1;

    // VPED-TC-006 — No JWT returns 401
    @Test
    @DisplayName("VPED-TC-006: no JWT returns 401 Unauthorized")
    void getExerciseDetail_noJwt_returns401() throws Exception {
        mockMvc.perform(get(DETAIL_URL))
                .andExpect(status().isUnauthorized());
    }

    // VPED-TC-SEC-001 — Non-MOTHER role (EXPERT) cannot access — CRITICAL
    @Test
    @WithMockUser(roles = "EXPERT")
    @DisplayName("VPED-TC-SEC-001: EXPERT role returns 403 Forbidden")
    void getExerciseDetail_expertRole_returns403() throws Exception {
        mockMvc.perform(get(DETAIL_URL))
                .andExpect(status().isForbidden());
    }

    // VPED-TC-SEC-001 variant — USER role (not MOTHER) also blocked
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("VPED-TC-SEC-001-variant: USER role returns 403 Forbidden")
    void getExerciseDetail_userRole_returns403() throws Exception {
        mockMvc.perform(get(DETAIL_URL))
                .andExpect(status().isForbidden());
    }

    // VPED-TC-007 — Invalid UUID format returns 400
    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("VPED-TC-007: invalid UUID format returns 400")
    void getExerciseDetail_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════
    // UC29 — List endpoint security tests
    // ═══════════════════════════════════════════════════════════

    private static final String LIST_URL = "/api/v1/exercises";

    // EX-TC-029-005 — No JWT on list endpoint returns 401
    @Test
    @DisplayName("EX-TC-029-005: list exercises without JWT returns 401")
    void listExercises_noJwt_returns401() throws Exception {
        mockMvc.perform(get(LIST_URL))
                .andExpect(status().isUnauthorized());
    }

    // EX-TC-029-SEC-001 — Non-MOTHER role on list endpoint returns 403
    @Test
    @WithMockUser(roles = "EXPERT")
    @DisplayName("EX-TC-029-SEC-001: EXPERT role on list returns 403")
    void listExercises_expertRole_returns403() throws Exception {
        mockMvc.perform(get(LIST_URL))
                .andExpect(status().isForbidden());
    }
}
