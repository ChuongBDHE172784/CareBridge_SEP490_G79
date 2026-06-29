package com.carebridge.backend.exercise.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.exercise.dto.ExerciseDetailResponse;
import com.carebridge.backend.exercise.dto.ExerciseSummaryResponse;
import com.carebridge.backend.exercise.dto.PostureConfigResponse;
import com.carebridge.backend.exercise.dto.SafetyCheckResponse;
import com.carebridge.backend.exercise.dto.StartSessionRequest;
import com.carebridge.backend.exercise.dto.StartSessionResponse;
import com.carebridge.backend.exercise.dto.SubmitSafetyCheckRequest;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.service.IExerciseDetailQueryService;
import com.carebridge.backend.exercise.service.IExerciseQueryService;
import com.carebridge.backend.exercise.service.IExerciseSafetyCheckService;
import com.carebridge.backend.exercise.service.IExerciseSessionService;
import com.carebridge.backend.exercise.service.IPostureConfigService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final IExerciseQueryService exerciseQueryService;
    private final IExerciseDetailQueryService exerciseDetailQueryService;
    private final IExerciseSafetyCheckService safetyCheckService;
    private final IExerciseSessionService sessionService;
    private final IPostureConfigService postureConfigService;

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

    // UC178 — Complete Pre-Exercise Safety Check
    @PostMapping("/{exerciseId}/safety-check")
    @PreAuthorize("hasAnyRole('MOTHER', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<ApiResponse<SafetyCheckResponse>> submitSafetyCheck(
            @PathVariable UUID exerciseId,
            @RequestBody @Valid SubmitSafetyCheckRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(safetyCheckService.submitSafetyCheck(exerciseId, request, userId));
    }

    @GetMapping("/{exerciseId}/safety-check/latest")
    @PreAuthorize("hasAnyRole('MOTHER', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<ApiResponse<SafetyCheckResponse>> getLatestSafetyCheck(
            @PathVariable UUID exerciseId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(safetyCheckService.getLatestSafetyCheck(exerciseId, userId));
    }

    // UC179 — Start Exercise Session
    @PostMapping("/{exerciseId}/sessions")
    @PreAuthorize("hasAnyRole('MOTHER', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<ApiResponse<StartSessionResponse>> startSession(
            @PathVariable UUID exerciseId,
            @RequestBody @Valid StartSessionRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sessionService.startSession(exerciseId, request, userId)));
    }

    // UC180 — Enable Posture Camera (active posture config lookup)
    @GetMapping("/{exerciseId}/posture-config")
    @PreAuthorize("hasAnyRole('MOTHER', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<ApiResponse<PostureConfigResponse>> getPostureConfig(
            @PathVariable UUID exerciseId) {
        return ResponseEntity.ok(postureConfigService.getActiveConfig(exerciseId));
    }
}
