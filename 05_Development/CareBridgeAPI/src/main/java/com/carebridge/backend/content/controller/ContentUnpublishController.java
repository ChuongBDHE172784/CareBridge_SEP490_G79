package com.carebridge.backend.content.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.content.dto.request.UnpublishRequest;
import com.carebridge.backend.content.dto.response.UnpublishResponse;
import com.carebridge.backend.content.service.ContentUnpublishService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/content")
@PreAuthorize("hasRole('CONTENT_ADMIN')")
@RequiredArgsConstructor
public class ContentUnpublishController {
    private final ContentUnpublishService service;

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<UnpublishResponse>> unpublish(@PathVariable UUID id,
            @Valid @RequestBody UnpublishRequest request, Principal principal) {
        UUID adminId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(service.unpublish(id, request, adminId), "Content unpublished"));
    }
}
