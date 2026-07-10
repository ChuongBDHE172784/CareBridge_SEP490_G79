package com.carebridge.backend.content.controller;

import com.carebridge.backend.content.dto.request.ModerateContentRequest;
import com.carebridge.backend.content.dto.request.ModerationHistoryFilter;
import com.carebridge.backend.content.dto.request.ModerationQueueFilter;
import com.carebridge.backend.content.dto.request.PendingContentQueueFilter;
import com.carebridge.backend.content.dto.request.ResolveReportRequest;
import com.carebridge.backend.content.dto.request.WarnOrSuspendAccountRequest;
import com.carebridge.backend.content.dto.response.ModerateContentResponse;
import com.carebridge.backend.content.dto.response.ModerationContentDetailResponse;
import com.carebridge.backend.content.dto.response.ModerationHistoryResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.dto.response.PendingContentQueueResponse;
import com.carebridge.backend.content.dto.response.ResolveReportResponse;
import com.carebridge.backend.content.dto.response.WarnOrSuspendAccountResponse;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.service.ModerationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.ResponseStatus;
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

    // C1: RBAC enforcement — MODERATOR only (ADR-002); CB-MOD-IMP-004 ADR-005/ADR-006
    @GetMapping("/pending-content")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<PendingContentQueueResponse> getPendingContentQueue(
            @RequestParam ReportTargetType targetType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            Principal principal) {

        // C6: Reject size > 50 with MOD-002 (same guard as /queue)
        if (size > 50) {
            throw ModerationException.pageSizeExceeded();
        }

        PendingContentQueueFilter filter = new PendingContentQueueFilter(targetType, page, size);
        PendingContentQueueResponse response = moderationService.getPendingContentQueue(filter, principal);
        return ResponseEntity.ok(response);
    }

    // C1: RBAC enforcement — MODERATOR only (ADR-002); CB-MOD-IMP-004 §16 ADR-007
    @GetMapping("/history")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ModerationHistoryResponse> getModerationHistory(
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            Principal principal) {

        // C6: Reject size > 50 with MOD-002 (same guard as /queue, /pending-content)
        if (size > 50) {
            throw ModerationException.pageSizeExceeded();
        }

        ModerationHistoryFilter filter = new ModerationHistoryFilter(targetType, page, size);
        ModerationHistoryResponse response = moderationService.getModerationHistory(filter, principal);
        return ResponseEntity.ok(response);
    }

    // C1: RBAC enforcement — MODERATOR only (ADR-002); controller has no business logic, delegates to service
    @PostMapping("/actions")
    @PreAuthorize("hasRole('MODERATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ModerateContentResponse> moderateContent(
            @Valid @RequestBody ModerateContentRequest request,
            Principal principal) {
        ModerateContentResponse response = moderationService.moderateContent(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // C1: RBAC enforcement — MODERATOR only (ADR-002 of UC-101); controller has no business logic
    @PostMapping("/reports/{reportId}/resolve")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ResolveReportResponse> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request,
            Principal principal) {
        ResolveReportResponse response = moderationService.resolveReport(reportId, request, principal);
        return ResponseEntity.ok(response);
    }

    // C1: RBAC enforcement — MODERATOR only (ADR-002 of UC-102); controller has no business logic
    @PostMapping("/account-actions")
    @PreAuthorize("hasRole('MODERATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<WarnOrSuspendAccountResponse> moderateAccount(
            @Valid @RequestBody WarnOrSuspendAccountRequest request,
            Principal principal) {
        WarnOrSuspendAccountResponse response = moderationService.moderateAccount(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // C1: RBAC enforcement — MODERATOR only (ADR-002); CB-MOD-IMP-008 — controller has no business logic
    @GetMapping("/content/{targetType}/{targetId}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ModerationContentDetailResponse> getContentDetail(
            @PathVariable ReportTargetType targetType,
            @PathVariable UUID targetId,
            Principal principal) {
        ModerationContentDetailResponse response = moderationService.getContentDetail(targetType, targetId, principal);
        return ResponseEntity.ok(response);
    }
}
