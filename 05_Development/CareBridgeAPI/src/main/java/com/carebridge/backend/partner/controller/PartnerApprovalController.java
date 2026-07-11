package com.carebridge.backend.partner.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.partner.dto.request.PartnerDecisionRequest;
import com.carebridge.backend.partner.dto.response.PartnerDecisionResponse;
import com.carebridge.backend.partner.dto.response.PartnerVerificationQueueItemResponse;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.service.PartnerApprovalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/partners")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class PartnerApprovalController {
    private final PartnerApprovalService service;

    @GetMapping
    public ResponseEntity<PaginatedResponse<PartnerVerificationQueueItemResponse>> getVerificationQueue(
            @RequestParam(required = false) OrganizationStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(PaginatedResponse.of(service.getVerificationQueue(status, search,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @PostMapping("/{partnerId}/decision")
    public ResponseEntity<ApiResponse<PartnerDecisionResponse>> decide(@PathVariable UUID partnerId,
            @Valid @RequestBody PartnerDecisionRequest request, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(service.decide(partnerId, request,
                SecurityUtils.requireCurrentUserId(principal)), "Partner decision applied successfully"));
    }
}
