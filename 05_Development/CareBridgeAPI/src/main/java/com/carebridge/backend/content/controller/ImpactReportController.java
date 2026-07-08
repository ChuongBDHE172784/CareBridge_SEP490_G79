package com.carebridge.backend.content.controller;

import com.carebridge.backend.content.dto.request.ImpactReportFilter;
import com.carebridge.backend.content.dto.response.ImpactReportResponse;
import com.carebridge.backend.content.service.ImpactReportService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/impact-report")
@RequiredArgsConstructor
public class ImpactReportController {

    private final ImpactReportService impactReportService;

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ImpactReportResponse> getImpactReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ImpactReportResponse response = impactReportService.getImpactReport(new ImpactReportFilter(from, to));
        return ResponseEntity.ok(response);
    }
}
