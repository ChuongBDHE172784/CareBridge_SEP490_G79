package com.carebridge.backend.recommendation.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.recommendation.dto.RecommendationContentResponse;
import com.carebridge.backend.recommendation.dto.RecommendationProfileResponse;
import com.carebridge.backend.recommendation.service.RecommendationService;
import com.carebridge.backend.recommendation.exception.RecommendationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<RecommendationProfileResponse>> getProfile(Principal principal) {
        UUID owner = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(recommendationService.getProfile(owner)));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<RecommendationProfileResponse>> putProfile(
            @RequestBody Map<String, Object> request, Principal principal) {
        UUID owner = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.putProfile(owner, objectMapper.valueToTree(request))));
    }

    @GetMapping("/content")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<RecommendationContentResponse>> getContent(
            @RequestParam(name = "limit", defaultValue = "3") String rawLimit,
            @RequestParam(name = "careGroupId", required = false) UUID careGroupId,
            Principal principal) {
        final int limit;
        try {
            limit = Integer.parseInt(rawLimit);
        } catch (NumberFormatException ex) {
            throw new RecommendationException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "RECOMMENDATION_LIMIT_INVALID", "limit must be an integer between 1 and 3");
        }
        UUID owner = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(recommendationService.getContent(owner, careGroupId, limit)));
    }
}
