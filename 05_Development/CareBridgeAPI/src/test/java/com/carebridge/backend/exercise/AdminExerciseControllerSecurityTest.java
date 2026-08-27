package com.carebridge.backend.exercise;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.exercise.controller.AdminExerciseController;
import com.carebridge.backend.exercise.dto.AdminExerciseResponse;
import com.carebridge.backend.exercise.exception.InvalidExerciseStateException;
import com.carebridge.backend.exercise.service.IAdminExerciseService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = AdminExerciseController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminExerciseControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAdminExerciseService adminExerciseService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID EXERCISE_ID = UUID.randomUUID();
    private static final String CREATE_BODY = """
            {"title":"Test","trimesterScope":"FIRST","difficultyLevel":"EASY",
             "durationMinutes":10,"safetyWarning":"warn","supportsPostureAnalysis":false}
            """;
    private static final String UPDATE_BODY = """
            {"title":"Test","trimesterScope":"FIRST","difficultyLevel":"EASY",
             "durationMinutes":10,"supportsPostureAnalysis":false}
            """;

    private void stubHappyPath() {
        AdminExerciseResponse response = AdminExerciseResponse.builder()
                .exerciseId(EXERCISE_ID).status("DRAFT").build();
        when(adminExerciseService.create(any(), any())).thenReturn(response);
        when(adminExerciseService.update(any(), any(), any())).thenReturn(response);
        when(adminExerciseService.activate(any(), any())).thenReturn(response);
        when(adminExerciseService.disable(any(), any())).thenReturn(response);
        when(adminExerciseService.getById(any())).thenReturn(response);
        when(adminExerciseService.list(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(PaginatedResponse.of(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1)));
    }

    // === MOTHER denied on all 6 endpoints ===
    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("MOTHER denied on listExercises")
    void listExercises_mother_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/exercises")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("MOTHER denied on createExercise")
    void createExercise_mother_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/exercises")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("MOTHER denied on updateExercise")
    void updateExercise_mother_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/admin/exercises/" + EXERCISE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("MOTHER denied on activateExercise")
    void activateExercise_mother_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/exercises/" + EXERCISE_ID + "/activate"))
                .andExpect(status().isForbidden());
    }

    // === SYSTEM_ADMIN denied (this is CONTENT_ADMIN's endpoint per ADR-EXERCISE-ADMIN-001) ===
    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("SYSTEM_ADMIN denied on createExercise (belongs to CONTENT_ADMIN, not SYSTEM_ADMIN)")
    void createExercise_systemAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/exercises")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    // === no JWT ===
    @Test
    @DisplayName("no JWT returns 401 on createExercise")
    void createExercise_noJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/exercises")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("no JWT returns 401 on listExercises")
    void listExercises_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/exercises")).andExpect(status().isUnauthorized());
    }

    // === CONTENT_ADMIN succeeds (positive control) ===
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    @DisplayName("CONTENT_ADMIN succeeds on createExercise")
    void createExercise_contentAdmin_returns201() throws Exception {
        stubHappyPath();
        mockMvc.perform(post("/api/v1/admin/exercises")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    @DisplayName("createExercise defaults an omitted posture flag to false")
    void createExercise_omittedPostureFlag_defaultsToFalse() throws Exception {
        AdminExerciseResponse response = AdminExerciseResponse.builder()
                .exerciseId(EXERCISE_ID).status("DRAFT").supportsPostureAnalysis(false).build();
        when(adminExerciseService.create(
                argThat(request -> Boolean.FALSE.equals(request.getSupportsPostureAnalysis())), any()))
                .thenReturn(response);

        String body = """
                {"title":"Test","trimesterScope":"FIRST","difficultyLevel":"EASY",
                 "durationMinutes":10,"safetyWarning":"warn"}
                """;
        mockMvc.perform(post("/api/v1/admin/exercises")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.supportsPostureAnalysis").value(false));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    @DisplayName("createExercise rejects an explicit null posture flag")
    void createExercise_nullPostureFlag_returns400() throws Exception {
        String body = """
                {"title":"Test","trimesterScope":"FIRST","difficultyLevel":"EASY",
                 "durationMinutes":10,"safetyWarning":"warn","supportsPostureAnalysis":null}
                """;

        mockMvc.perform(post("/api/v1/admin/exercises")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verifyNoInteractions(adminExerciseService);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    @DisplayName("CONTENT_ADMIN succeeds on updateExercise")
    void updateExercise_contentAdmin_returns200() throws Exception {
        stubHappyPath();
        mockMvc.perform(put("/api/v1/admin/exercises/" + EXERCISE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    @DisplayName("CONTENT_ADMIN succeeds on activateExercise")
    void activateExercise_contentAdmin_returns200() throws Exception {
        stubHappyPath();
        mockMvc.perform(patch("/api/v1/admin/exercises/" + EXERCISE_ID + "/activate"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    @DisplayName("CONTENT_ADMIN sees stable posture readiness error on activation")
    void activateExercise_postureNotReady_returns409WithStableCode() throws Exception {
        when(adminExerciseService.activate(any(), any()))
                .thenThrow(InvalidExerciseStateException.postureNotReady());

        mockMvc.perform(patch("/api/v1/admin/exercises/" + EXERCISE_ID + "/activate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EXERCISE_POSTURE_NOT_READY"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    @DisplayName("CONTENT_ADMIN succeeds on disableExercise")
    void disableExercise_contentAdmin_returns200() throws Exception {
        stubHappyPath();
        mockMvc.perform(patch("/api/v1/admin/exercises/" + EXERCISE_ID + "/disable"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    @DisplayName("CONTENT_ADMIN succeeds on listExercises")
    void listExercises_contentAdmin_returns200() throws Exception {
        stubHappyPath();
        mockMvc.perform(get("/api/v1/admin/exercises")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1", roles = "CONTENT_ADMIN")
    @DisplayName("CONTENT_ADMIN succeeds on getExercise")
    void getExercise_contentAdmin_returns200() throws Exception {
        stubHappyPath();
        mockMvc.perform(get("/api/v1/admin/exercises/" + EXERCISE_ID)).andExpect(status().isOk());
    }
}
