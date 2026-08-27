package com.carebridge.backend.family.controller;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.family.dto.SharedDataResponse;
import com.carebridge.backend.family.entity.SharedDataCategory;
import com.carebridge.backend.family.service.ISharedDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/care-groups")
@RequiredArgsConstructor
public class SharedDataController {

    private final ISharedDataService sharedDataService;

    // UC84: View shared data (calendar / logs / alerts) with permission filtering
    @GetMapping("/{groupId}/shared-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SharedDataResponse>> getSharedData(
            @PathVariable UUID groupId,
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        SharedDataCategory parsed = parseCategory(category);
        var response = sharedDataService.getSharedData(groupId, callerId, parsed, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private SharedDataCategory parseCategory(String raw) {
        try {
            return SharedDataCategory.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-012",
                    "Unsupported category: " + raw + ". Supported values: calendar, logs, alerts");
        }
    }
}
