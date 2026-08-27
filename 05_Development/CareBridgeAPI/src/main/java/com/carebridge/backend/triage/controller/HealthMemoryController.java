package com.carebridge.backend.triage.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.response.HealthMemoryEntryResponse;
import com.carebridge.backend.triage.service.HealthMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/triage/health-memory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MOTHER')")
public class HealthMemoryController {
    private final HealthMemoryService healthMemoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HealthMemoryEntryResponse>>> list(
            @RequestParam TriageStage stage, @RequestParam UUID profileId, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(healthMemoryService.list(userId, stage, profileId).stream()
                .map(HealthMemoryEntryResponse::from).toList()));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(@PathVariable UUID entryId, Principal principal) {
        healthMemoryService.delete(SecurityUtils.requireCurrentUserId(principal), entryId);
        return ResponseEntity.noContent().build();
    }
}
