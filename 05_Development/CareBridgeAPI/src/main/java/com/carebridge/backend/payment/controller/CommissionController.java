package com.carebridge.backend.payment.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.payment.dto.request.CreateSettlementRequest;
import com.carebridge.backend.payment.dto.response.CommissionRecordDTO;
import com.carebridge.backend.payment.dto.response.SettlementRecordDTO;
import com.carebridge.backend.payment.entity.SettlementRecord;
import com.carebridge.backend.payment.service.ICommissionService;
import com.carebridge.backend.security.annotation.RequireRoles;
import com.carebridge.backend.security.rbac.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Commission Controller.
 * REST endpoints for expert commission management.
 *
 * TV4 API Spec: /api/v1/commissions, /api/v1/settlements
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommissionController {

    private final ICommissionService commissionService;

    /**
     * GET /api/v1/experts/{expertId}/commissions
     * Get commission records for an expert (P1).
     *
     * Auth: EXPERT (own) or ADMIN
     */
    @GetMapping("/experts/{expertId}/commissions")
    @RequireRoles({Role.EXPERT, Role.SYSTEM_ADMIN})
    public ResponseEntity<ApiResponse<List<CommissionRecordDTO>>> getExpertCommissions(
            @PathVariable("expertId") Long expertId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<CommissionRecordDTO> commissions = commissionService.getExpertCommissions(expertId);
        return ResponseEntity.ok(ApiResponse.success(commissions));
    }

    /**
     * POST /api/v1/experts/{expertId}/settlements
     * Create a settlement for an expert (P1).
     *
     * Auth: SYSTEM_ADMIN only
     */
    @PostMapping("/experts/{expertId}/settlements")
    @RequireRoles(Role.SYSTEM_ADMIN)
    public ResponseEntity<ApiResponse<SettlementRecord>> createSettlement(
            @PathVariable("expertId") Long expertId,
            @Valid @RequestBody CreateSettlementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        SettlementRecord settlement = commissionService.createSettlement(expertId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(settlement, "Settlement created successfully"));
    }

    /**
     * PUT /api/v1/settlements/{settlementId}/complete
     * Complete a settlement (mark as paid) (P1).
     *
     * Auth: SYSTEM_ADMIN only
     */
    @PutMapping("/settlements/{settlementId}/complete")
    @RequireRoles(Role.SYSTEM_ADMIN)
    public ResponseEntity<ApiResponse<SettlementRecord>> completeSettlement(
            @PathVariable("settlementId") Long settlementId,
            @AuthenticationPrincipal UserDetails userDetails) {

        SettlementRecord settlement = commissionService.completeSettlement(settlementId);
        return ResponseEntity.ok(ApiResponse.success(settlement, "Settlement completed"));
    }

    /**
     * GET /api/v1/experts/{expertId}/settlements
     * Get settlement history for an expert (P2).
     *
     * Auth: EXPERT (own) or ADMIN
     */
    @GetMapping("/experts/{expertId}/settlements")
    @RequireRoles({Role.EXPERT, Role.SYSTEM_ADMIN})
    public ResponseEntity<ApiResponse<java.util.List<SettlementRecordDTO>>> getSettlements(
            @PathVariable("expertId") Long expertId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var settlementsPage = commissionService.getSettlements(expertId, pageable);
        return ResponseEntity.ok(ApiResponse.success(settlementsPage.getContent()));
    }

    /**
     * GET /api/v1/settlements/{settlementId}
     * Get settlement detail by ID (P2).
     *
     * Auth: SYSTEM_ADMIN or expert owner
     */
    @GetMapping("/settlements/{settlementId}")
    public ResponseEntity<ApiResponse<SettlementRecordDTO>> getSettlement(
            @PathVariable("settlementId") Long settlementId,
            @AuthenticationPrincipal UserDetails userDetails) {

        SettlementRecordDTO settlement = commissionService.getSettlement(settlementId);
        return ResponseEntity.ok(ApiResponse.success(settlement));
    }
}
