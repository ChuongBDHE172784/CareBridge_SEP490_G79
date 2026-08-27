package com.carebridge.backend.content.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.content.dto.request.CreateReportRequest;
import com.carebridge.backend.content.dto.response.CreateReportResponse;
import com.carebridge.backend.content.service.ReportService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-14 Report Content or Account (CB-MOD-IMP-014 §9). Controller only validates and
 * delegates (CLAUDE.md §Architecture) — no business logic here.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateReportResponse>> createReport(
            Principal principal,
            @Valid @RequestBody CreateReportRequest request) {

        UUID reporterUserId = SecurityUtils.requireCurrentUserId(principal);
        CreateReportResponse response = reportService.createReport(request, reporterUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
