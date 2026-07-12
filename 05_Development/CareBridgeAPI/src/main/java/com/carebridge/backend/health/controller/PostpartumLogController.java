package com.carebridge.backend.health.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.health.dto.AddPostpartumLogRequest;
import com.carebridge.backend.health.dto.PostpartumLogResponse;
import com.carebridge.backend.health.dto.UpdatePostpartumLogRequest;
import com.carebridge.backend.health.service.IPostpartumLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostpartumLogController {

    private final IPostpartumLogService postpartumLogService;

    // UC189: View postpartum logs
    @GetMapping("/postpartum-logs")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<PostpartumLogResponse>>> listLogs(
            @RequestParam UUID journeyId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = postpartumLogService.listLogs(journeyId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC189: View postpartum log detail
    @GetMapping("/postpartum-logs/{logId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<PostpartumLogResponse>> getLogDetail(
            @PathVariable UUID logId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = postpartumLogService.getLogDetail(logId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC28: Add postpartum recovery log
    @PostMapping("/journeys/{journeyId}/postpartum-logs")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<PostpartumLogResponse>> addLog(
            @PathVariable UUID journeyId,
            @Valid @RequestBody AddPostpartumLogRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = postpartumLogService.addLog(callerId, journeyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // UC190: Update postpartum log
    @PatchMapping("/postpartum-logs/{logId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<PostpartumLogResponse>> updateLog(
            @PathVariable UUID logId,
            @Valid @RequestBody UpdatePostpartumLogRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = postpartumLogService.updateLog(logId, callerId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC191: Delete postpartum log
    @DeleteMapping("/postpartum-logs/{logId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<Void> deleteLog(
            @PathVariable UUID logId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        postpartumLogService.deleteLog(logId, callerId);
        return ResponseEntity.noContent().build();
    }
}
