package com.carebridge.backend.partner.controller;
import com.carebridge.backend.partner.service.SponsoredCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.carebridge.backend.common.response.ApiResponse;import com.carebridge.backend.common.util.SecurityUtils;import com.carebridge.backend.partner.dto.request.SubmitSponsoredContentRequest;import com.carebridge.backend.partner.dto.response.SubmitSponsoredContentResponse;import jakarta.validation.Valid;import java.security.Principal;import org.springframework.http.*;
@RestController @RequestMapping("/api/v1/partner/campaigns") @PreAuthorize("hasRole('PARTNER')") @RequiredArgsConstructor
public class SponsoredCampaignController { private final SponsoredCampaignService service;
 @PostMapping public ResponseEntity<ApiResponse<SubmitSponsoredContentResponse>> submit(@Valid @RequestBody SubmitSponsoredContentRequest request,Principal principal){var response=service.submitCampaign(request,SecurityUtils.requireCurrentUserId(principal));return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response,"Sponsored campaign submitted successfully"));}}
