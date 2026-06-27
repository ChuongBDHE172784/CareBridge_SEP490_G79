package com.carebridge.backend.exercise.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.exercise.dto.ExerciseDetailResponse;
import com.carebridge.backend.exercise.dto.ExerciseSummaryResponse;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.service.IExerciseDetailQueryService;
import com.carebridge.backend.exercise.service.IExerciseQueryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final IExerciseQueryService exerciseQueryService;
    private final IExerciseDetailQueryService exerciseDetailQueryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<PaginatedResponse<ExerciseSummaryResponse>> listExercises(
            @RequestParam(required = false) TrimesterScope trimester,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                exerciseQueryService.listPublishedExercises(trimester, difficulty, page, size));
    }

    @GetMapping("/{exerciseId}")
    @PreAuthorize("hasAnyRole('MOTHER', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<ApiResponse<ExerciseDetailResponse>> getExerciseDetail(
            @PathVariable UUID exerciseId) {
        return ResponseEntity.ok(exerciseDetailQueryService.getExerciseDetail(exerciseId));
    }
}
