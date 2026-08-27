package com.carebridge.backend.exercise;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.exercise.controller.ExerciseController;
import com.carebridge.backend.exercise.dto.ExerciseDetailResponse;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.mapper.ExerciseMapper;
import com.carebridge.backend.exercise.service.IExerciseDetailQueryService;
import com.carebridge.backend.exercise.service.IExerciseSafetyCheckService;
import com.carebridge.backend.exercise.service.IExerciseSessionService;
import com.carebridge.backend.exercise.service.IPostureConfigService;
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
class ExerciseDetailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IExerciseDetailQueryService exerciseDetailQueryService;

    @MockitoBean
    private com.carebridge.backend.exercise.service.IExerciseQueryService exerciseQueryService;

    @MockitoBean
    private IExerciseSafetyCheckService safetyCheckService;

    @MockitoBean
    private IExerciseSessionService sessionService;

    @MockitoBean
    private IPostureConfigService postureConfigService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private final ExerciseMapper mapper = new ExerciseMapper();

    // VPED-TC-INT-001 — PUBLISHED exercise detail returned with all fields
    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("VPED-TC-INT-001: PUBLISHED exercise detail with all fields via HTTP")
    void getExerciseDetail_published_returnsFullDetail() throws Exception {
        PregnancyExercise entity = ExerciseDetailTestFactory.makePublishedExerciseWithFullDetail();
        ExerciseDetailResponse dto = mapper.toDetailResponse(entity);

        when(exerciseDetailQueryService.getExerciseDetail(ExerciseDetailTestFactory.UUID_PUBLISHED_1))
                .thenReturn(ApiResponse.success(dto));

        mockMvc.perform(get("/api/v1/exercises/{id}", ExerciseDetailTestFactory.UUID_PUBLISHED_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exerciseId").value(ExerciseDetailTestFactory.UUID_PUBLISHED_1.toString()))
                .andExpect(jsonPath("$.data.title").value("Prenatal Yoga - First Trimester"))
                .andExpect(jsonPath("$.data.instructionContent").isNotEmpty())
                .andExpect(jsonPath("$.data.safetyWarning").value("Stop immediately if you feel dizzy or experience pain."))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.supportsPostureAnalysis").value(true))
                .andExpect(jsonPath("$.data.trimesterScope").value("FIRST"))
                .andExpect(jsonPath("$.data.difficultyLevel").value("EASY"));
    }

    // VPED-TC-INT-002 — null safetyWarning → empty string via HTTP
    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("VPED-TC-INT-002: null safetyWarning in DB maps to empty string in HTTP response")
    void getExerciseDetail_nullSafetyWarning_emptyStringInResponse() throws Exception {
        PregnancyExercise entity = ExerciseDetailTestFactory.makePublishedExerciseWithNullSafetyWarning();
        ExerciseDetailResponse dto = mapper.toDetailResponse(entity);

        when(exerciseDetailQueryService.getExerciseDetail(ExerciseDetailTestFactory.UUID_PUBLISHED_2))
                .thenReturn(ApiResponse.success(dto));

        mockMvc.perform(get("/api/v1/exercises/{id}", ExerciseDetailTestFactory.UUID_PUBLISHED_2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.safetyWarning").value(""))
                .andExpect(jsonPath("$.data.safetyWarning").exists());
    }

    // VPED-TC-INT-003 — DRAFT exercise returns 404 via HTTP — CRITICAL
    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("VPED-TC-INT-003: DRAFT exercise returns 404 EX-001 via HTTP (ADR-VPED-001)")
    void getExerciseDetail_draftExercise_returns404() throws Exception {
        when(exerciseDetailQueryService.getExerciseDetail(ExerciseDetailTestFactory.UUID_DRAFT))
                .thenThrow(ExerciseNotFoundException.notFound());

        mockMvc.perform(get("/api/v1/exercises/{id}", ExerciseDetailTestFactory.UUID_DRAFT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EX-001"))
                .andExpect(jsonPath("$.message").value("Exercise not found"));
    }

    // VPED-TC-SEC-002 — DRAFT detail not leaked via error message
    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("VPED-TC-SEC-002: error message does not reveal DRAFT status")
    void getExerciseDetail_draftExercise_noInfoLeak() throws Exception {
        when(exerciseDetailQueryService.getExerciseDetail(ExerciseDetailTestFactory.UUID_DRAFT))
                .thenThrow(ExerciseNotFoundException.notFound());

        String responseBody = mockMvc.perform(get("/api/v1/exercises/{id}", ExerciseDetailTestFactory.UUID_DRAFT))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(responseBody)
                .doesNotContainIgnoringCase("DRAFT")
                .doesNotContainIgnoringCase("not published")
                .doesNotContainIgnoringCase("archived");
    }

    // ═══════════════════════════════════════════════════════════
    // UC29 — List endpoint integration tests
    // ═══════════════════════════════════════════════════════════

    // EX-TC-029-INT-001 — List returns only PUBLISHED, DRAFT filtered
    @Test
    @WithMockUser(roles = "MOTHER")
    @DisplayName("EX-TC-029-INT-001: list returns 2 PUBLISHED, DRAFT filtered via HTTP")
    void listExercises_mixedStatuses_returnsOnlyPublished() throws Exception {
        PregnancyExercise e1 = ExerciseDetailTestFactory.makePublishedExerciseWithFullDetail();
        PregnancyExercise e2 = ExerciseDetailTestFactory.makePublishedExerciseWithNullSafetyWarning();

        com.carebridge.backend.exercise.dto.ExerciseSummaryResponse s1 = mapper.toSummaryResponse(e1);
        com.carebridge.backend.exercise.dto.ExerciseSummaryResponse s2 = mapper.toSummaryResponse(e2);

        org.springframework.data.domain.Page<com.carebridge.backend.exercise.dto.ExerciseSummaryResponse> page =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(s1, s2),
                        org.springframework.data.domain.PageRequest.of(0, 20), 2);
        com.carebridge.backend.common.response.PaginatedResponse<com.carebridge.backend.exercise.dto.ExerciseSummaryResponse> paged =
                com.carebridge.backend.common.response.PaginatedResponse.of(page);

        when(exerciseQueryService.listPublishedExercises(null, null, 0, 20))
                .thenReturn(paged);

        mockMvc.perform(get("/api/v1/exercises")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].exerciseId").exists())
                .andExpect(jsonPath("$.data[1].safetyWarning").value(""));
    }
}
