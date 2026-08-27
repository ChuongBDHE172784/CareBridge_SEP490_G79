package com.carebridge.backend.recommendation.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.recommendation.dto.RecommendationTagCatalogResponse;
import com.carebridge.backend.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/content")
@PreAuthorize("hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class RecommendationAdminController {
    private final RecommendationService recommendationService;

    @GetMapping("/recommendation-tags")
    public ResponseEntity<ApiResponse<RecommendationTagCatalogResponse>> getCatalog() {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.getCatalog()));
    }
}
