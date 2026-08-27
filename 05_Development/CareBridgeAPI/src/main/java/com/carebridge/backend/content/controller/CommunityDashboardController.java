package com.carebridge.backend.content.controller;

import com.carebridge.backend.content.dto.request.DashboardFilter;
import com.carebridge.backend.content.dto.response.CommunityDashboardResponse;
import com.carebridge.backend.content.service.CommunityDashboardService;
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
@RequestMapping("/api/v1/moderator/community/dashboard")
@RequiredArgsConstructor
public class CommunityDashboardController {

    private final CommunityDashboardService communityDashboardService;

    @GetMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<CommunityDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        CommunityDashboardResponse response = communityDashboardService.getDashboard(new DashboardFilter(from, to));
        return ResponseEntity.ok(response);
    }
}
