package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.exercise.dto.AdminExerciseResponse;
import com.carebridge.backend.exercise.dto.CreateExerciseRequest;
import com.carebridge.backend.exercise.dto.UpdateExerciseRequest;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import java.util.UUID;

public interface IAdminExerciseService {

    /** Creates a new pregnancy exercise with status=DRAFT. */
    AdminExerciseResponse create(CreateExerciseRequest request, UUID adminUserId);

    /**
     * Updates exercise content fields. Does not change status.
     * @throws com.carebridge.backend.exercise.exception.ExerciseNotFoundException (EX-001) if exerciseId does not exist
     * @throws com.carebridge.backend.exercise.exception.InvalidExerciseStateException (EX-ADMIN-002) if safetyWarning is explicitly blank ("")
     */
    AdminExerciseResponse update(UUID exerciseId, UpdateExerciseRequest request, UUID adminUserId);

    /**
     * Transitions exercise status to PUBLISHED. Idempotent if already PUBLISHED.
     * @throws com.carebridge.backend.exercise.exception.ExerciseNotFoundException (EX-001) if exerciseId does not exist
     */
    AdminExerciseResponse activate(UUID exerciseId, UUID adminUserId);

    /**
     * Transitions exercise status to ARCHIVED. Idempotent if already ARCHIVED.
     * @throws com.carebridge.backend.exercise.exception.ExerciseNotFoundException (EX-001) if exerciseId does not exist
     */
    AdminExerciseResponse disable(UUID exerciseId, UUID adminUserId);

    /**
     * Retrieves a single exercise regardless of status (admin view).
     * @throws com.carebridge.backend.exercise.exception.ExerciseNotFoundException (EX-001) if exerciseId does not exist
     */
    AdminExerciseResponse getById(UUID exerciseId);

    /** Lists all exercises regardless of status, with optional filters, for the admin management screen. */
    PaginatedResponse<AdminExerciseResponse> list(
            ExerciseStatus status, TrimesterScope trimester, DifficultyLevel difficulty, int page, int size);
}
