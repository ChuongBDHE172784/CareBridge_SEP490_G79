package com.carebridge.backend.consultation.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.consultation.dto.request.CreateConsultationRequestRequest;
import com.carebridge.backend.consultation.dto.request.RejectConsultationRequestRequest;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestPendingSummaryResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestSummaryResponse;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import com.carebridge.backend.consultation.service.IConsultationRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.carebridge.backend.consultation.matching.ExpertMatchingService;
import com.carebridge.backend.consultation.matching.ExpertMatchingService.SweepResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consultation-requests")
@RequiredArgsConstructor
@Validated
public class ConsultationRequestController {

    private final IConsultationRequestService service;
    private final ExpertMatchingService expertMatchingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<ConsultationRequestResponse>> create(
            @Valid @RequestBody CreateConsultationRequestRequest request,
            Principal principal) {
        var result = service.create(request, SecurityUtils.requireCurrentUserId(principal));
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.success(result.response()));
    }

    /**
     * Hàm vét điều phối: gợi ý chuyên gia đang còn lịch trống, ưu tiên Chuyên gia Hệ thống,
     * hết mới rơi xuống Chuyên gia Y tế Cộng đồng. Chỉ gợi ý — người dùng vẫn phải tạo yêu cầu
     * qua POST ở trên và chuyên gia vẫn phải bấm chấp nhận.
     */
    @GetMapping("/matching")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<SweepResult>> matching(
            @RequestParam(required = false) String specialty,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "24") int windowHours,
            @RequestParam(defaultValue = "30") int minMinutes) {
        return ResponseEntity.ok(ApiResponse.success(
                expertMatchingService.sweep(specialty, limit, windowHours, minMinutes)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<PaginatedResponse<ConsultationRequestSummaryResponse>> listMine(
            @RequestParam(required = false) ConsultationRequestStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            Principal principal) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PaginatedResponse.of(service.listMine(
                SecurityUtils.requireCurrentUserId(principal), status, pageable)));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<PaginatedResponse<ConsultationRequestSummaryResponse>> listAssigned(
            @RequestParam(required = false) ConsultationRequestStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            Principal principal) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PaginatedResponse.of(service.listAssigned(
                SecurityUtils.requireCurrentUserId(principal), status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT')")
    public ResponseEntity<ApiResponse<ConsultationRequestResponse>> getById(
            @PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getById(id, SecurityUtils.requireCurrentUserId(principal))));
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ConsultationRequestResponse>> accept(
            @PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.accept(id, SecurityUtils.requireCurrentUserId(principal))));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ConsultationRequestResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectConsultationRequestRequest request,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(service.reject(
                id, SecurityUtils.requireCurrentUserId(principal), request.getReason())));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<ConsultationRequestResponse>> cancel(
            @PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.cancel(id, SecurityUtils.requireCurrentUserId(principal))));
    }

    @GetMapping("/pending-summary")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ConsultationRequestPendingSummaryResponse>> pendingSummary(
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.pendingSummary(SecurityUtils.requireCurrentUserId(principal))));
    }
}
