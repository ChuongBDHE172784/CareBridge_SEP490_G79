package com.carebridge.backend.aimoderation.controller;

import com.carebridge.backend.aimoderation.dto.request.AiFeedbackRequest;
import com.carebridge.backend.aimoderation.dto.response.AiAssessmentResponse;
import com.carebridge.backend.aimoderation.dto.response.AiFeedbackResponse;
import com.carebridge.backend.aimoderation.service.AiAssessmentModeratorService;
import com.carebridge.backend.common.util.SecurityUtils;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Moderator-facing AI assessment endpoints, sharing the /api/v1/admin/moderation base and
 * the raw-DTO convention of ModerationController. Read-only evidence view + agree/disagree
 * feedback — policy management stays SYSTEM_ADMIN-only in AiModerationAdminController.
 */
@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
public class AiModerationModeratorController {

    private final AiAssessmentModeratorService moderatorService;

    @GetMapping("/reports/{reportId}/assessment")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<AiAssessmentResponse> getAssessment(@PathVariable UUID reportId, Principal principal) {
        UUID moderatorUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(moderatorService.getAssessmentForReport(reportId, moderatorUserId));
    }

    @PostMapping("/assessments/{assessmentId}/feedback")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<AiFeedbackResponse> submitFeedback(
            @PathVariable UUID assessmentId,
            @Valid @RequestBody AiFeedbackRequest request,
            Principal principal) {
        UUID moderatorUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moderatorService.submitFeedback(assessmentId, request, moderatorUserId));
    }
}
