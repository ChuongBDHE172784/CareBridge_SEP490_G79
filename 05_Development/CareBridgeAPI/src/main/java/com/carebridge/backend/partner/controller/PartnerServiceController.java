package com.carebridge.backend.partner.controller;

import com.carebridge.backend.partner.service.PartnerServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.partner.dto.request.SubmitServiceListingRequest;
import com.carebridge.backend.partner.dto.response.SubmitServiceListingResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.partner.dto.response.PartnerServiceListItemResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/partner/services")
@PreAuthorize("hasRole('PARTNER')")
@RequiredArgsConstructor
public class PartnerServiceController {
    private final PartnerServiceService partnerServiceService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<PartnerServiceListItemResponse>> getOwnServices(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size, Principal principal) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PaginatedResponse.of(partnerServiceService.getOwnServices(
                SecurityUtils.requireCurrentUserId(principal), pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubmitServiceListingResponse>> submitService(
            @Valid @RequestBody SubmitServiceListingRequest request, Principal principal) {
        var response = partnerServiceService.submitService(request, SecurityUtils.requireCurrentUserId(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Partner service submitted successfully"));
    }
}
