package com.carebridge.backend.content.controller;

import com.carebridge.backend.content.dto.request.ModerationQueueFilter;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.service.ModerationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    // C1: RBAC enforcement — MODERATOR only (ADR-002)
    @GetMapping("/queue")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ModerationQueueResponse> getQueue(
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            Principal principal) {

        // C6: Reject size > 50 with MOD-002
        if (size > 50) {
            throw ModerationException.pageSizeExceeded();
        }

        ModerationQueueFilter filter = new ModerationQueueFilter(targetType, status, page, size);
        ModerationQueueResponse response = moderationService.getModerationQueue(filter, principal);
        return ResponseEntity.ok(response);
    }
}
