package com.carebridge.backend.exercise.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.exercise.dto.AdminExerciseResponse;
import com.carebridge.backend.exercise.dto.CreateExerciseRequest;
import com.carebridge.backend.exercise.dto.UpdateExerciseRequest;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.service.IAdminExerciseService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/exercises")
@RequiredArgsConstructor
public class AdminExerciseController {

    private final IAdminExerciseService adminExerciseService;

    @GetMapping
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<PaginatedResponse<AdminExerciseResponse>> listExercises(
            @RequestParam(required = false) ExerciseStatus status,
            @RequestParam(required = false) TrimesterScope trimester,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminExerciseService.list(status, trimester, difficulty, page, size));
    }

    @GetMapping("/{exerciseId}")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminExerciseResponse>> getExercise(@PathVariable UUID exerciseId) {
        return ResponseEntity.ok(ApiResponse.success(adminExerciseService.getById(exerciseId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminExerciseResponse>> createExercise(
            @RequestBody @Valid CreateExerciseRequest request, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminExerciseService.create(request, adminUserId)));
    }

    @PutMapping("/{exerciseId}")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminExerciseResponse>> updateExercise(
            @PathVariable UUID exerciseId,
            @RequestBody @Valid UpdateExerciseRequest request,
            Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(
                ApiResponse.success(adminExerciseService.update(exerciseId, request, adminUserId)));
    }

    @PatchMapping("/{exerciseId}/activate")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminExerciseResponse>> activateExercise(
            @PathVariable UUID exerciseId, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(adminExerciseService.activate(exerciseId, adminUserId)));
    }

    @PatchMapping("/{exerciseId}/disable")
    @PreAuthorize("hasRole('CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<AdminExerciseResponse>> disableExercise(
            @PathVariable UUID exerciseId, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(adminExerciseService.disable(exerciseId, adminUserId)));
    }
}
