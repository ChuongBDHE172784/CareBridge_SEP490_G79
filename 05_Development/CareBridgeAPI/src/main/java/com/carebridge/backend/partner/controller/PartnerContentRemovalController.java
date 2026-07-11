package com.carebridge.backend.partner.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.partner.dto.request.RemovalRequest;
import com.carebridge.backend.partner.dto.response.RemovalResponse;
import com.carebridge.backend.partner.entity.PartnerContentTargetType;
import com.carebridge.backend.partner.service.PartnerContentRemovalService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/partner-content")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class PartnerContentRemovalController {
    private final PartnerContentRemovalService service;

    @PostMapping("/{targetType}/{targetId}/remove")
    public ResponseEntity<ApiResponse<RemovalResponse>> remove(@PathVariable PartnerContentTargetType targetType,
            @PathVariable UUID targetId, @Valid @RequestBody RemovalRequest request, Principal principal) {
        UUID adminId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(service.remove(targetType, targetId, request, adminId),
                "Partner content removed successfully"));
    }
}
