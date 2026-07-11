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

@RestController
@RequestMapping("/api/v1/partner/services")
@PreAuthorize("hasRole('PARTNER')")
@RequiredArgsConstructor
public class PartnerServiceController {
    private final PartnerServiceService partnerServiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubmitServiceListingResponse>> submitService(
            @Valid @RequestBody SubmitServiceListingRequest request, Principal principal) {
        var response = partnerServiceService.submitService(request, SecurityUtils.requireCurrentUserId(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Partner service submitted successfully"));
    }
}
