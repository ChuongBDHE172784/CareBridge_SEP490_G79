package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.exercise.dto.AdminExerciseResponse;
import com.carebridge.backend.exercise.dto.CreateExerciseRequest;
import com.carebridge.backend.exercise.dto.UpdateExerciseRequest;
import com.carebridge.backend.exercise.entity.AnalysisMode;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.exception.InvalidExerciseStateException;
import com.carebridge.backend.exercise.inference.PostureInferenceConfigResolver;
import com.carebridge.backend.exercise.mapper.ExerciseMapper;
import com.carebridge.backend.exercise.policy.ExercisePublishReadinessPolicy;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import com.carebridge.backend.exercise.service.impl.AdminExerciseServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

@ExtendWith(MockitoExtension.class)
class AdminExerciseServiceTest {

    private static final UUID EXERCISE_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock
    private ExerciseRepository exerciseRepository;

    @Spy
    private ExerciseMapper exerciseMapper;

    @Mock
    private AuditService auditService;

    private AdminExerciseServiceImpl service;

    @Mock
    private PostureAnalysisConfigRepository postureConfigRepository;

    @Mock
    private PostureInferenceConfigResolver inferenceConfigResolver;

    private ExercisePublishReadinessPolicy publishReadinessPolicy;

    private static final Instant POLICY_NOW = Instant.parse("2026-08-10T00:00:00Z");

    @BeforeEach
    void setUpService() {
        publishReadinessPolicy = new ExercisePublishReadinessPolicy(
                postureConfigRepository,
                inferenceConfigResolver,
                Clock.fixed(POLICY_NOW, ZoneOffset.UTC));
        service = new AdminExerciseServiceImpl(
                exerciseRepository, exerciseMapper, auditService, publishReadinessPolicy);
    }

    private PregnancyExercise existingExercise(ExerciseStatus status) {
        PregnancyExercise entity = new PregnancyExercise();
        entity.setExerciseId(EXERCISE_ID);
        entity.setCreatedBy(ADMIN_ID);
        entity.setTitle("Pelvic Tilt Stretch");
        entity.setTrimesterScope(TrimesterScope.SECOND);
        entity.setDifficultyLevel(DifficultyLevel.EASY);
        entity.setDurationMinutes((short) 10);
        entity.setSafetyWarning("Stop if dizzy.");
        entity.setSupportsPostureAnalysis(false);
        entity.setStatus(status);
        entity.setVersionNo(1);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        return entity;
    }

    private CreateExerciseRequest createRequest() {
        CreateExerciseRequest request = new CreateExerciseRequest();
        request.setTitle("New Exercise");
        request.setTrimesterScope(TrimesterScope.FIRST);
        request.setDifficultyLevel(DifficultyLevel.MEDIUM);
        request.setDurationMinutes((short) 15);
        request.setSafetyWarning("Consult your doctor first.");
        request.setSupportsPostureAnalysis(false);
        return request;
    }

    private UpdateExerciseRequest updateRequest(String safetyWarning) {
        UpdateExerciseRequest request = new UpdateExerciseRequest();
        request.setTitle("Updated Title");
        request.setTrimesterScope(TrimesterScope.THIRD);
        request.setDifficultyLevel(DifficultyLevel.HARD);
        request.setDurationMinutes((short) 20);
        request.setSafetyWarning(safetyWarning);
        request.setSupportsPostureAnalysis(true);
        return request;
    }

    // US-EXERCISE-ADMIN-002 — create defaults to DRAFT
    @Test
    @DisplayName("create: new exercise defaults to DRAFT, versionNo=1, audit logged")
    void create_defaultsToDraft() {
        when(exerciseRepository.save(any(PregnancyExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminExerciseResponse response = service.create(createRequest(), ADMIN_ID);

        assertThat(response.getStatus()).isEqualTo("DRAFT");
        assertThat(response.getVersionNo()).isEqualTo(1);
        verify(auditService).log(eq(AuditAction.EXERCISE_CREATED), eq(ADMIN_ID), any(), any(), any());
    }

    @Test
    @DisplayName("create: omitted posture flag is persisted as false")
    void create_omittedPostureFlag_defaultsToFalse() {
        CreateExerciseRequest request = createRequest();
        request.setSupportsPostureAnalysis(null);
        when(exerciseRepository.save(any(PregnancyExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminExerciseResponse response = service.create(request, ADMIN_ID);

        assertThat(response.getSupportsPostureAnalysis()).isFalse();
    }

    // US-EXERCISE-ADMIN-002 — update happy path, versionNo bumped
    @Test
    @DisplayName("update: applies fields, bumps versionNo, audit logged")
    void update_happyPath_bumpsVersion() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(exerciseRepository.save(any(PregnancyExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminExerciseResponse response = service.update(EXERCISE_ID, updateRequest("New warning text"), ADMIN_ID);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getSafetyWarning()).isEqualTo("New warning text");
        assertThat(response.getVersionNo()).isEqualTo(2);
        verify(auditService).log(eq(AuditAction.EXERCISE_UPDATED), eq(ADMIN_ID), any(), any(), any());
    }

    // ADR-EXERCISE-ADMIN-004 — null safetyWarning leaves existing value unchanged
    @Test
    @DisplayName("update: null safetyWarning leaves existing warning unchanged")
    void update_nullSafetyWarning_unchanged() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(exerciseRepository.save(any(PregnancyExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminExerciseResponse response = service.update(EXERCISE_ID, updateRequest(null), ADMIN_ID);

        assertThat(response.getSafetyWarning()).isEqualTo("Stop if dizzy.");
    }

    // ADR-EXERCISE-ADMIN-004 — CRITICAL: explicit blank safetyWarning rejected
    @Test
    @DisplayName("update: explicit blank safetyWarning throws EX-ADMIN-002, entity NOT saved")
    void update_blankSafetyWarning_rejected() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(EXERCISE_ID, updateRequest(""), ADMIN_ID))
                .isInstanceOf(InvalidExerciseStateException.class)
                .satisfies(ex -> assertThat(((InvalidExerciseStateException) ex).getCode())
                        .isEqualTo("EX-ADMIN-002"));

        verify(exerciseRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: exercise not found throws EX-001")
    void update_notFound_throws() {
        UUID unknownId = UUID.randomUUID();
        when(exerciseRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(unknownId, updateRequest("text"), ADMIN_ID))
                .isInstanceOf(ExerciseNotFoundException.class);
    }

    // US-EXERCISE-ADMIN-003 — activate DRAFT -> PUBLISHED
    @Test
    @DisplayName("activate: DRAFT transitions to PUBLISHED, audit logged")
    void activate_draftToPublished() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(exerciseRepository.save(any(PregnancyExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminExerciseResponse response = service.activate(EXERCISE_ID, ADMIN_ID);

        assertThat(response.getStatus()).isEqualTo("PUBLISHED");
        verify(auditService).log(eq(AuditAction.EXERCISE_ACTIVATED), eq(ADMIN_ID), any(), any(), any());
        verifyNoInteractions(postureConfigRepository, inferenceConfigResolver);
    }

    @Test
    @DisplayName("activate: posture-enabled exercise without ready config is rejected without mutation")
    void activate_postureNotReady_doesNotMutate() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        existing.setSupportsPostureAnalysis(true);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(EXERCISE_ID, ADMIN_ID))
                .isInstanceOf(InvalidExerciseStateException.class)
                .satisfies(ex -> assertThat(((InvalidExerciseStateException) ex).getCode())
                        .isEqualTo("EXERCISE_POSTURE_NOT_READY"));

        assertThat(existing.getStatus()).isEqualTo(ExerciseStatus.DRAFT);
        verify(exerciseRepository, never()).save(any());
        verify(auditService, never()).log(eq(AuditAction.EXERCISE_ACTIVATED), any(), any(), any(), any());
    }

    @Test
    @DisplayName("activate: invalid model config is rejected without mutation")
    void activate_invalidModelConfig_doesNotMutate() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        existing.setSupportsPostureAnalysis(true);
        PostureAnalysisConfig config = readyConfig(AnalysisMode.MODEL_BASED);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config))
                .thenThrow(new com.carebridge.backend.exercise.inference.PostureInferenceUnavailableException(
                        "CONFIGURATION_INVALID"));

        assertThatThrownBy(() -> service.activate(EXERCISE_ID, ADMIN_ID))
                .isInstanceOf(InvalidExerciseStateException.class)
                .satisfies(ex -> assertThat(((InvalidExerciseStateException) ex).getCode())
                        .isEqualTo("EXERCISE_POSTURE_NOT_READY"));

        assertThat(existing.getStatus()).isEqualTo(ExerciseStatus.DRAFT);
        verify(exerciseRepository, never()).save(any());
        verify(auditService, never()).log(eq(AuditAction.EXERCISE_ACTIVATED), any(), any(), any(), any());
    }

    @Test
    @DisplayName("activate: valid model config publishes and audits exactly once")
    void activate_validModelConfig_publishes() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        existing.setSupportsPostureAnalysis(true);
        PostureAnalysisConfig config = readyConfig(AnalysisMode.MODEL_BASED);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config))
                .thenReturn(new PostureInferenceConfigResolver.ResolvedInferenceConfig(
                        PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(exerciseRepository.save(any(PregnancyExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminExerciseResponse response = service.activate(EXERCISE_ID, ADMIN_ID);

        assertThat(response.getStatus()).isEqualTo("PUBLISHED");
        verify(exerciseRepository).save(existing);
        verify(auditService).log(eq(AuditAction.EXERCISE_ACTIVATED), eq(ADMIN_ID), any(), any(), any());
    }

    @Test
    @DisplayName("activate: valid rule-based config publishes without model resolution")
    void activate_validRuleBasedConfig_publishesWithoutResolver() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        existing.setSupportsPostureAnalysis(true);
        PostureAnalysisConfig config = readyConfig(AnalysisMode.RULE_BASED);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(exerciseRepository.save(any(PregnancyExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminExerciseResponse response = service.activate(EXERCISE_ID, ADMIN_ID);

        assertThat(response.getStatus()).isEqualTo("PUBLISHED");
        verifyNoInteractions(inferenceConfigResolver);
        verify(auditService).log(eq(AuditAction.EXERCISE_ACTIVATED), eq(ADMIN_ID), any(), any(), any());
    }

    @Test
    @DisplayName("activate: future posture config is rejected before mutation")
    void activate_futureConfig_doesNotMutate() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        existing.setSupportsPostureAnalysis(true);
        PostureAnalysisConfig config = readyConfig(AnalysisMode.RULE_BASED);
        config.setEffectiveFrom(OffsetDateTime.ofInstant(POLICY_NOW.plusSeconds(60), ZoneOffset.UTC));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));

        assertThatThrownBy(() -> service.activate(EXERCISE_ID, ADMIN_ID))
                .isInstanceOf(InvalidExerciseStateException.class)
                .satisfies(ex -> assertThat(((InvalidExerciseStateException) ex).getCode())
                        .isEqualTo("EXERCISE_POSTURE_NOT_READY"));

        assertThat(existing.getStatus()).isEqualTo(ExerciseStatus.DRAFT);
        verify(exerciseRepository, never()).save(any());
        verifyNoInteractions(inferenceConfigResolver);
        verify(auditService, never()).log(eq(AuditAction.EXERCISE_ACTIVATED), any(), any(), any(), any());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidStaticConfigs")
    @DisplayName("activate: invalid static posture config is rejected before mutation")
    void activate_invalidStaticConfig_doesNotMutate(
            String scenario, PostureAnalysisConfig config) {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        existing.setSupportsPostureAnalysis(true);
        OffsetDateTime originalUpdatedAt = existing.getUpdatedAt();
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));

        assertThatThrownBy(() -> service.activate(EXERCISE_ID, ADMIN_ID))
                .isInstanceOf(InvalidExerciseStateException.class)
                .satisfies(ex -> assertThat(((InvalidExerciseStateException) ex).getCode())
                        .isEqualTo("EXERCISE_POSTURE_NOT_READY"));

        assertThat(existing.getStatus()).isEqualTo(ExerciseStatus.DRAFT);
        assertThat(existing.getVersionNo()).isEqualTo(1);
        assertThat(existing.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        verify(exerciseRepository, never()).save(any());
        verify(auditService, never()).log(eq(AuditAction.EXERCISE_ACTIVATED), any(), any(), any(), any());
    }

    private static Stream<Arguments> invalidStaticConfigs() {
        PostureAnalysisConfig inactive = readyConfig(AnalysisMode.RULE_BASED);
        inactive.setStatus("SUPERSEDED");

        PostureAnalysisConfig expired = readyConfig(AnalysisMode.RULE_BASED);
        expired.setEffectiveTo(OffsetDateTime.ofInstant(POLICY_NOW, ZoneOffset.UTC));

        PostureAnalysisConfig unknownMode = readyConfig(AnalysisMode.RULE_BASED);
        unknownMode.setAnalysisMode("UNSUPPORTED");

        PostureAnalysisConfig nullThreshold = readyConfig(AnalysisMode.RULE_BASED);
        nullThreshold.setConfidenceThreshold(null);

        PostureAnalysisConfig outOfRangeThreshold = readyConfig(AnalysisMode.RULE_BASED);
        outOfRangeThreshold.setConfidenceThreshold(new BigDecimal("1.01"));

        return Stream.of(
                Arguments.of("inactive", inactive),
                Arguments.of("expired", expired),
                Arguments.of("unknown mode", unknownMode),
                Arguments.of("null threshold", nullThreshold),
                Arguments.of("out-of-range threshold", outOfRangeThreshold));
    }

    @Test
    @DisplayName("activate: duplicate active configs return stable readiness conflict")
    void activate_duplicateActiveConfig_returnsReadinessConflict() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        existing.setSupportsPostureAnalysis(true);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenThrow(new IncorrectResultSizeDataAccessException(2));

        assertThatThrownBy(() -> service.activate(EXERCISE_ID, ADMIN_ID))
                .isInstanceOf(InvalidExerciseStateException.class)
                .satisfies(ex -> assertThat(((InvalidExerciseStateException) ex).getCode())
                        .isEqualTo("EXERCISE_POSTURE_NOT_READY"));
        verify(exerciseRepository, never()).save(any());
        verify(auditService, never()).log(eq(AuditAction.EXERCISE_ACTIVATED), any(), any(), any(), any());
    }

    @Test
    @DisplayName("activate: valid hybrid config uses the resolver and publishes")
    void activate_validHybridConfig_publishes() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        existing.setSupportsPostureAnalysis(true);
        PostureAnalysisConfig config = readyConfig(AnalysisMode.HYBRID);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config))
                .thenReturn(new PostureInferenceConfigResolver.ResolvedInferenceConfig(
                        PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(exerciseRepository.save(any(PregnancyExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminExerciseResponse response = service.activate(EXERCISE_ID, ADMIN_ID);

        assertThat(response.getStatus()).isEqualTo("PUBLISHED");
        verify(inferenceConfigResolver).resolve(config);
        verify(auditService).log(eq(AuditAction.EXERCISE_ACTIVATED), eq(ADMIN_ID), any(), any(), any());
    }

    @Test
    @DisplayName("activate: already published remains an idempotent no-op before readiness checks")
    void activate_published_doesNotCheckReadiness() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.PUBLISHED);
        existing.setSupportsPostureAnalysis(true);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));

        service.activate(EXERCISE_ID, ADMIN_ID);

        verifyNoInteractions(postureConfigRepository, inferenceConfigResolver);
        verify(exerciseRepository, never()).save(any());
    }

    private static PostureAnalysisConfig readyConfig(AnalysisMode mode) {
        return PostureAnalysisConfig.builder()
                .postureConfigId(UUID.randomUUID())
                .exerciseId(EXERCISE_ID)
                .analysisMode(mode.name())
                .ruleOrModelVersion(PostureInferenceConfigResolver.PINNED_MODEL_VERSION)
                .confidenceThreshold(new BigDecimal("0.70"))
                .configJson("{\"exerciseKey\":\"squat\"}")
                .effectiveFrom(OffsetDateTime.ofInstant(POLICY_NOW.minusSeconds(60), ZoneOffset.UTC))
                .effectiveTo(null)
                .status("ACTIVE")
                .build();
    }

    // Idempotent activate
    @Test
    @DisplayName("activate: already PUBLISHED is idempotent no-op, no audit entry")
    void activate_alreadyPublished_idempotent() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.PUBLISHED);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));

        AdminExerciseResponse response = service.activate(EXERCISE_ID, ADMIN_ID);

        assertThat(response.getStatus()).isEqualTo("PUBLISHED");
        verify(exerciseRepository, never()).save(any());
        verify(auditService, never()).log(eq(AuditAction.EXERCISE_ACTIVATED), any(), any(), any(), any());
    }

    // US-EXERCISE-ADMIN-004 — disable PUBLISHED -> ARCHIVED
    @Test
    @DisplayName("disable: PUBLISHED transitions to ARCHIVED, audit logged")
    void disable_publishedToArchived() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.PUBLISHED);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));
        when(exerciseRepository.save(any(PregnancyExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminExerciseResponse response = service.disable(EXERCISE_ID, ADMIN_ID);

        assertThat(response.getStatus()).isEqualTo("ARCHIVED");
        verify(auditService).log(eq(AuditAction.EXERCISE_DISABLED), eq(ADMIN_ID), any(), any(), any());
    }

    @Test
    @DisplayName("getById: returns exercise regardless of status (DRAFT)")
    void getById_returnsDraftExercise() {
        PregnancyExercise existing = existingExercise(ExerciseStatus.DRAFT);
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(existing));

        AdminExerciseResponse response = service.getById(EXERCISE_ID);

        assertThat(response.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("getById: not found throws EX-001")
    void getById_notFound_throws() {
        UUID unknownId = UUID.randomUUID();
        when(exerciseRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(unknownId)).isInstanceOf(ExerciseNotFoundException.class);
    }

    @Test
    @DisplayName("list: returns all statuses (admin view), status filter optional")
    void list_returnsAllStatuses() {
        PregnancyExercise draft = existingExercise(ExerciseStatus.DRAFT);
        Page<PregnancyExercise> page = new PageImpl<>(java.util.List.of(draft), PageRequest.of(0, 20), 1);
        when(exerciseRepository.findAllByFilters(null, null, null, PageRequest.of(0, 20)))
                .thenReturn(page);

        PaginatedResponse<AdminExerciseResponse> response = service.list(null, null, null, 0, 20);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getStatus()).isEqualTo("DRAFT");
    }
}
