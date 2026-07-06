package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.exercise.dto.AdminExerciseResponse;
import com.carebridge.backend.exercise.dto.CreateExerciseRequest;
import com.carebridge.backend.exercise.dto.UpdateExerciseRequest;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.exception.InvalidExerciseStateException;
import com.carebridge.backend.exercise.mapper.ExerciseMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.service.impl.AdminExerciseServiceImpl;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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

    @InjectMocks
    private AdminExerciseServiceImpl service;

    private PregnancyExercise existingExercise(ExerciseStatus status) {
        PregnancyExercise entity = new PregnancyExercise();
        entity.setExerciseId(EXERCISE_ID);
        entity.setCreatedBy(ADMIN_ID);
        entity.setTitle("Pelvic Tilt Stretch");
        entity.setTrimesterScope(TrimesterScope.SECOND);
        entity.setDifficultyLevel(DifficultyLevel.EASY);
        entity.setDurationMinutes((short) 10);
        entity.setSafetyWarning("Stop if dizzy.");
        entity.setSupportsPostureAnalysis(true);
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
