package com.carebridge.backend.exercise.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.exercise.dto.ExerciseSessionHistorySummary;
import com.carebridge.backend.exercise.dto.PostureEventRequest;
import com.carebridge.backend.exercise.dto.PostureFeedbackResponse;
import com.carebridge.backend.exercise.dto.SessionResultResponse;
import com.carebridge.backend.exercise.dto.SessionStateResponse;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.service.IExerciseSessionHistoryService;
import com.carebridge.backend.exercise.service.IExerciseSessionResultService;
import com.carebridge.backend.exercise.service.IExerciseSessionService;
import com.carebridge.backend.exercise.service.IPostureAnalysisService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exercises/sessions")
@RequiredArgsConstructor
public class ExerciseSessionController {

    private final IExerciseSessionService sessionService;
    private final IExerciseSessionResultService sessionResultService;
    private final IExerciseSessionHistoryService sessionHistoryService;
    private final IPostureAnalysisService postureAnalysisService;

    // UC181 — Pause session
    @PatchMapping("/{sessionId}/pause")
    @PreAuthorize("hasAnyRole('MOTHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SessionStateResponse>> pauseSession(
            @PathVariable UUID sessionId, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(
                ApiResponse.success(sessionService.pauseSession(sessionId, userId)));
    }

    // UC181 — Resume session
    @PatchMapping("/{sessionId}/resume")
    @PreAuthorize("hasAnyRole('MOTHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SessionStateResponse>> resumeSession(
            @PathVariable UUID sessionId, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(
                ApiResponse.success(sessionService.resumeSession(sessionId, userId)));
    }

    // UC30 — Analyze Exercise Posture (submit body landmark data, receive real-time feedback)
    @PostMapping("/{sessionId}/posture-events")
    @PreAuthorize("hasAnyRole('MOTHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PostureFeedbackResponse>> analyzePosture(
            @PathVariable UUID sessionId,
            @RequestBody @Valid PostureEventRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(postureAnalysisService.analyzePosture(sessionId, userId, request));
    }

    // UC182 — Complete session
    @PatchMapping("/{sessionId}/complete")
    @PreAuthorize("hasAnyRole('MOTHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SessionResultResponse>> completeSession(
            @PathVariable UUID sessionId, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(
                ApiResponse.success(sessionService.completeSession(sessionId, userId)));
    }

    // UC183 — View session result
    @GetMapping("/{sessionId}/result")
    @PreAuthorize("hasAnyRole('MOTHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SessionResultResponse>> getSessionResult(
            @PathVariable UUID sessionId, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(sessionResultService.getSessionResult(sessionId, userId));
    }

    // UC184 — View pregnancy exercise history
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('MOTHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<PaginatedResponse<ExerciseSessionHistorySummary>> getSessionHistory(
            @RequestParam(required = false) TrimesterScope trimesterScope,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(
                sessionHistoryService.getSessionHistory(userId, trimesterScope, from, to, page, size));
    }
}
