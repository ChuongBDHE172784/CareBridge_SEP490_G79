package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.AdminPostureConfigResponse;
import com.carebridge.backend.exercise.dto.CreatePostureConfigRequest;
import com.carebridge.backend.exercise.dto.UpdatePostureConfigRequest;
import com.carebridge.backend.exercise.entity.AnalysisMode;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import com.carebridge.backend.exercise.entity.PostureFeedbackLevel;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.exception.InvalidPostureConfigException;
import com.carebridge.backend.exercise.exception.PostureConfigNotFoundException;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import com.carebridge.backend.exercise.service.impl.PostureConfigServiceImpl;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostureConfigServiceTest {

    private static final UUID EXERCISE_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID ADMIN_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ACTIVE_CONFIG_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID SUPERSEDED_CONFIG_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000c0");

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private PostureAnalysisConfigRepository postureConfigRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PostureConfigServiceImpl service;

    private PregnancyExercise makeExercise(Consumer<PregnancyExercise> overrides) {
        PregnancyExercise exercise = new PregnancyExercise();
        exercise.setExerciseId(EXERCISE_ID);
        exercise.setCreatedBy(ADMIN_ID);
        exercise.setTitle("Pelvic Tilt Stretch");
        exercise.setStatus(ExerciseStatus.PUBLISHED);
        exercise.setSupportsPostureAnalysis(true);
        exercise.setVersionNo(1);
        overrides.accept(exercise);
        return exercise;
    }

    private PostureAnalysisConfig makeActiveConfig(Consumer<PostureAnalysisConfig> overrides) {
        PostureAnalysisConfig config = PostureAnalysisConfig.builder()
                .postureConfigId(ACTIVE_CONFIG_ID)
                .exerciseId(EXERCISE_ID)
                .configuredBy(ADMIN_ID)
                .analysisMode("MODEL_BASED")
                .ruleOrModelVersion("posenet-v2.1.0")
                .confidenceThreshold(new BigDecimal("0.75"))
                .feedbackLevel("DETAILED")
                .effectiveFrom(OffsetDateTime.parse("2026-07-01T00:00:00Z"))
                .effectiveTo(null)
                .status("ACTIVE")
                .build();
        overrides.accept(config);
        return config;
    }

    private CreatePostureConfigRequest makeCreateRequest(Consumer<CreatePostureConfigRequest> overrides) {
        CreatePostureConfigRequest request = new CreatePostureConfigRequest();
        request.setExerciseId(EXERCISE_ID);
        request.setAnalysisMode(AnalysisMode.MODEL_BASED);
        request.setRuleOrModelVersion("posenet-v2.1.0");
        request.setConfidenceThreshold(new BigDecimal("0.75"));
        request.setFeedbackLevel(PostureFeedbackLevel.DETAILED);
        overrides.accept(request);
        return request;
    }

    private UpdatePostureConfigRequest makeUpdateRequest(Consumer<UpdatePostureConfigRequest> overrides) {
        UpdatePostureConfigRequest request = new UpdatePostureConfigRequest();
        request.setAnalysisMode(AnalysisMode.HYBRID);
        request.setRuleOrModelVersion("posenet-v2.2.0");
        request.setConfidenceThreshold(new BigDecimal("0.80"));
        request.setFeedbackLevel(PostureFeedbackLevel.DETAILED);
        overrides.accept(request);
        return request;
    }

    // === PAC-TC-CREATE-001 ===
    @Test
    @DisplayName("PAC-TC-CREATE-001: create initial config happy path")
    void createConfig_happyPath() {
        when(exerciseRepository.findById(EXERCISE_ID))
                .thenReturn(Optional.of(makeExercise(e -> {})));
        when(postureConfigRepository.existsByExerciseId(EXERCISE_ID)).thenReturn(false);
        when(postureConfigRepository.save(any(PostureAnalysisConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<AdminPostureConfigResponse> response =
                service.createConfig(makeCreateRequest(r -> {}), ADMIN_ID);

        assertThat(response.getData().getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getData().getEffectiveTo()).isNull();

        ArgumentCaptor<PostureAnalysisConfig> captor = ArgumentCaptor.forClass(PostureAnalysisConfig.class);
        verify(postureConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().getEffectiveFrom()).isNotNull();

        verify(auditService).log(eq(AuditAction.POSTURE_CONFIG_CREATED), eq(ADMIN_ID), any(), any(), any());
    }

    // === PAC-TC-CREATE-002 ===
    @Test
    @DisplayName("PAC-TC-CREATE-002: create config exercise not found")
    void createConfig_exerciseNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(exerciseRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createConfig(makeCreateRequest(r -> r.setExerciseId(unknownId)), ADMIN_ID))
                .isInstanceOf(ExerciseNotFoundException.class);

        verify(postureConfigRepository, never()).save(any());
    }

    // === PAC-TC-CREATE-003 ===
    @Test
    @DisplayName("PAC-TC-CREATE-003: create config exercise does not support posture analysis")
    void createConfig_exerciseDoesNotSupportPosture() {
        when(exerciseRepository.findById(EXERCISE_ID))
                .thenReturn(Optional.of(makeExercise(e -> e.setSupportsPostureAnalysis(false))));

        assertThatThrownBy(() -> service.createConfig(makeCreateRequest(r -> {}), ADMIN_ID))
                .isInstanceOf(InvalidPostureConfigException.class)
                .satisfies(ex -> assertThat(((InvalidPostureConfigException) ex).getCode()).isEqualTo("PAC-005"));

        verify(postureConfigRepository, never()).save(any());
    }

    // === PAC-TC-CREATE-004 ===
    @Test
    @DisplayName("PAC-TC-CREATE-004: create config already exists")
    void createConfig_alreadyExists() {
        when(exerciseRepository.findById(EXERCISE_ID))
                .thenReturn(Optional.of(makeExercise(e -> {})));
        when(postureConfigRepository.existsByExerciseId(EXERCISE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createConfig(makeCreateRequest(r -> {}), ADMIN_ID))
                .isInstanceOf(InvalidPostureConfigException.class)
                .satisfies(ex -> assertThat(((InvalidPostureConfigException) ex).getCode()).isEqualTo("PAC-006"));

        verify(postureConfigRepository, never()).save(any());
    }

    // === PAC-TC-VER-001 ===
    @Test
    @DisplayName("PAC-TC-VER-001: create new version supersedes current active, prior row unchanged")
    void createNewVersion_supersedesCurrentActive() {
        PostureAnalysisConfig previous = makeActiveConfig(c -> {});
        when(postureConfigRepository.findByExerciseIdAndStatus(EXERCISE_ID, "ACTIVE"))
                .thenReturn(Optional.of(previous));
        when(postureConfigRepository.save(any(PostureAnalysisConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<AdminPostureConfigResponse> response =
                service.createNewVersion(EXERCISE_ID, makeUpdateRequest(r -> {}), ADMIN_ID);

        assertThat(response.getData().getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getData().getRuleOrModelVersion()).isEqualTo("posenet-v2.2.0");
        assertThat(response.getData().getPostureConfigId()).isNotEqualTo(ACTIVE_CONFIG_ID);

        ArgumentCaptor<PostureAnalysisConfig> captor = ArgumentCaptor.forClass(PostureAnalysisConfig.class);
        verify(postureConfigRepository, times(2)).save(captor.capture());

        PostureAnalysisConfig supersededCapture = captor.getAllValues().stream()
                .filter(c -> c.getPostureConfigId().equals(ACTIVE_CONFIG_ID))
                .findFirst().orElseThrow();
        assertThat(supersededCapture.getStatus()).isEqualTo("SUPERSEDED");
        assertThat(supersededCapture.getEffectiveTo()).isNotNull();
        // Original analysis parameters must remain byte-for-byte unchanged.
        assertThat(supersededCapture.getAnalysisMode()).isEqualTo("MODEL_BASED");
        assertThat(supersededCapture.getConfidenceThreshold()).isEqualByComparingTo("0.75");

        verify(auditService).log(eq(AuditAction.POSTURE_CONFIG_UPDATED), eq(ADMIN_ID), any(), any(), any());
    }

    // === PAC-TC-VER-002 ===
    @Test
    @DisplayName("PAC-TC-VER-002: create new version no active config exists")
    void createNewVersion_noActiveConfig() {
        when(postureConfigRepository.findByExerciseIdAndStatus(EXERCISE_ID, "ACTIVE"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createNewVersion(EXERCISE_ID, makeUpdateRequest(r -> {}), ADMIN_ID))
                .isInstanceOf(PostureConfigNotFoundException.class)
                .satisfies(ex -> assertThat(((PostureConfigNotFoundException) ex).getCode()).isEqualTo("PAC-004"));
    }

    // === PAC-TC-ACT-001 ===
    @Test
    @DisplayName("PAC-TC-ACT-001: activate a SUPERSEDED version (rollback)")
    void activateVersion_rollback() {
        PostureAnalysisConfig target = makeActiveConfig(c -> {
            c.setPostureConfigId(SUPERSEDED_CONFIG_ID);
            c.setStatus("SUPERSEDED");
            c.setConfidenceThreshold(new BigDecimal("0.60"));
        });
        PostureAnalysisConfig currentlyActive = makeActiveConfig(c -> {});

        when(postureConfigRepository.findById(SUPERSEDED_CONFIG_ID)).thenReturn(Optional.of(target));
        when(postureConfigRepository.findByExerciseIdAndStatus(EXERCISE_ID, "ACTIVE"))
                .thenReturn(Optional.of(currentlyActive));
        when(postureConfigRepository.save(any(PostureAnalysisConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<AdminPostureConfigResponse> response =
                service.activateVersion(SUPERSEDED_CONFIG_ID, ADMIN_ID);

        assertThat(response.getData().getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getData().getPostureConfigId()).isEqualTo(SUPERSEDED_CONFIG_ID);
        assertThat(currentlyActive.getStatus()).isEqualTo("SUPERSEDED");

        verify(auditService).log(eq(AuditAction.POSTURE_CONFIG_ACTIVATED), eq(ADMIN_ID), any(), any(), any());
    }

    // === PAC-TC-ACT-002 ===
    @Test
    @DisplayName("PAC-TC-ACT-002: activate an already-ACTIVE version is idempotent no-op")
    void activateVersion_idempotentNoOp() {
        PostureAnalysisConfig target = makeActiveConfig(c -> {});
        when(postureConfigRepository.findById(ACTIVE_CONFIG_ID)).thenReturn(Optional.of(target));

        ApiResponse<AdminPostureConfigResponse> response =
                service.activateVersion(ACTIVE_CONFIG_ID, ADMIN_ID);

        assertThat(response.getData().getStatus()).isEqualTo("ACTIVE");
        verify(postureConfigRepository, never()).save(any());
        verify(auditService, never()).log(eq(AuditAction.POSTURE_CONFIG_ACTIVATED), any(), any(), any(), any());
    }

    // === PAC-TC-ACT-003 ===
    @Test
    @DisplayName("PAC-TC-ACT-003: activate nonexistent version")
    void activateVersion_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(postureConfigRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateVersion(unknownId, ADMIN_ID))
                .isInstanceOf(PostureConfigNotFoundException.class)
                .satisfies(ex -> assertThat(((PostureConfigNotFoundException) ex).getCode()).isEqualTo("PAC-004"));
    }

    // === PAC-TC-LIST-001 ===
    @Test
    @DisplayName("PAC-TC-LIST-001: list version history for an exercise")
    void listVersions_returnsHistory() {
        PostureAnalysisConfig active = makeActiveConfig(c -> {});
        PostureAnalysisConfig superseded = makeActiveConfig(c -> {
            c.setPostureConfigId(SUPERSEDED_CONFIG_ID);
            c.setStatus("SUPERSEDED");
        });
        when(postureConfigRepository.findAllByExerciseIdOrderByEffectiveFromDesc(EXERCISE_ID))
                .thenReturn(List.of(active, superseded));

        ApiResponse<List<AdminPostureConfigResponse>> response = service.listVersions(EXERCISE_ID);

        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getData().get(1).getStatus()).isEqualTo("SUPERSEDED");
    }

    // === PAC-TC-REGRESSION-001 ===
    @Test
    @DisplayName("PAC-TC-REGRESSION-001: getActiveConfig() untouched by this feature")
    void getActiveConfig_stillWorksAsExisting() {
        when(exerciseRepository.findByExerciseIdAndStatus(EXERCISE_ID, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.of(makeExercise(e -> {})));
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(makeActiveConfig(c -> {})));

        var response = service.getActiveConfig(EXERCISE_ID);

        assertThat(response.getData().getPostureConfigId()).isEqualTo(ACTIVE_CONFIG_ID);
        assertThat(response.getData().getAnalysisMode()).isEqualTo("MODEL_BASED");
    }
}
