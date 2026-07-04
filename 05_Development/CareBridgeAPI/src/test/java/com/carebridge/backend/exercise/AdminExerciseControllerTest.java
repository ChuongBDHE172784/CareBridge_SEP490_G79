package com.carebridge.backend.exercise;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.exercise.controller.AdminExerciseController;
import com.carebridge.backend.exercise.exception.InvalidExerciseStateException;
import com.carebridge.backend.exercise.service.IAdminExerciseService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
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
        value = AdminExerciseController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminExerciseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAdminExerciseService adminExerciseService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID EXERCISE_ID = UUID.randomUUID();

    // C7 — durationMinutes out of [1,180] range
    @Test
    @WithMockUser(roles = "CONTENT_ADMIN")
    @DisplayName("createExercise: durationMinutes=200 (above 180) returns 400 EX-ADMIN-001, service never invoked")
    void createExercise_durationAboveMax_returns400() throws Exception {
        String body = """
                {"title":"Test","trimesterScope":"FIRST","difficultyLevel":"EASY",
                 "durationMinutes":200,"safetyWarning":"warn","supportsPostureAnalysis":false}
                """;
        mockMvc.perform(post("/api/v1/admin/exercises")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminExerciseService);
    }

    @Test
    @WithMockUser(roles = "CONTENT_ADMIN")
    @DisplayName("createExercise: durationMinutes=0 (below 1) returns 400")
    void createExercise_durationBelowMin_returns400() throws Exception {
        String body = """
                {"title":"Test","trimesterScope":"FIRST","difficultyLevel":"EASY",
                 "durationMinutes":0,"safetyWarning":"warn","supportsPostureAnalysis":false}
                """;
        mockMvc.perform(post("/api/v1/admin/exercises")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CONTENT_ADMIN")
    @DisplayName("createExercise: missing safetyWarning returns 400 (required on create)")
    void createExercise_missingSafetyWarning_returns400() throws Exception {
        String body = """
                {"title":"Test","trimesterScope":"FIRST","difficultyLevel":"EASY",
                 "durationMinutes":10,"supportsPostureAnalysis":false}
                """;
        mockMvc.perform(post("/api/v1/admin/exercises")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ADR-EXERCISE-ADMIN-004 — HTTP-level blank safetyWarning rejection
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    @DisplayName("updateExercise: explicit blank safetyWarning surfaces EX-ADMIN-002 via HTTP")
    void updateExercise_blankSafetyWarning_returns400() throws Exception {
        when(adminExerciseService.update(any(), any(), any()))
                .thenThrow(InvalidExerciseStateException.safetyWarningCannotBeBlanked());

        String body = """
                {"title":"Test","trimesterScope":"FIRST","difficultyLevel":"EASY",
                 "durationMinutes":10,"safetyWarning":"","supportsPostureAnalysis":false}
                """;
        mockMvc.perform(put("/api/v1/admin/exercises/" + EXERCISE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("EX-ADMIN-002"));
    }
}
