package com.carebridge.backend.family.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.family.service.FamilyQuickNoteService;
import com.carebridge.backend.health.dto.MetricTrendResponse;
import com.carebridge.backend.health.entity.MetricType;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/care-groups/{careGroupId}/quick-notes")
@RequiredArgsConstructor
public class FamilyQuickNoteController {

    private final FamilyQuickNoteService quickNoteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<MetricTrendResponse>> getHistory(
            @PathVariable UUID careGroupId,
            @RequestParam MetricType metricType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Principal principal) {
        UUID callerId = SecurityUtils.requireCurrentUserId(principal);
        Instant resolvedTo = to == null ? Instant.now() : to;
        Instant resolvedFrom = from == null ? resolvedTo.minus(90, ChronoUnit.DAYS) : from;
        return ResponseEntity.ok(ApiResponse.success(
                quickNoteService.getHistory(careGroupId, callerId, metricType, resolvedFrom, resolvedTo)));
    }
}
