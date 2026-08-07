package com.carebridge.backend.triage.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.triage.dto.request.CreateRedFlagRuleRequest;
import com.carebridge.backend.triage.dto.request.RedFlagRuleFilter;
import com.carebridge.backend.triage.dto.request.UpdateRedFlagRuleRequest;
import com.carebridge.backend.triage.dto.response.RedFlagRulePageResponse;
import com.carebridge.backend.triage.dto.response.RedFlagRuleResponse;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import com.carebridge.backend.triage.service.RedFlagRuleService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/red-flag-rules")
// Red-flag rules drive emergency escalation, so authoring them is reserved for SYSTEM_ADMIN —
// the same audience the /admin/safety-rules portal route is gated to. Moderators act on the
// results of these rules; they never edit the rules themselves.
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class RedFlagRuleController {

    private final RedFlagRuleService redFlagRuleService;

    @PostMapping
    public ResponseEntity<ApiResponse<RedFlagRuleResponse>> create(
            @Valid @RequestBody CreateRedFlagRuleRequest request,
            Principal principal) {
        UUID actorUserId = SecurityUtils.requireCurrentUserId(principal);
        RedFlagRuleResponse response = redFlagRuleService.createRule(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Red-flag rule created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RedFlagRulePageResponse>> list(
            @RequestParam(required = false) RedFlagSeverity severity,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RedFlagRuleFilter filter = new RedFlagRuleFilter(severity, isActive, page, size);
        RedFlagRulePageResponse response = redFlagRuleService.listRules(filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<RedFlagRuleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRedFlagRuleRequest request,
            Principal principal) {
        UUID actorUserId = SecurityUtils.requireCurrentUserId(principal);
        RedFlagRuleResponse response = redFlagRuleService.updateRule(id, request, actorUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Red-flag rule updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        UUID actorUserId = SecurityUtils.requireCurrentUserId(principal);
        redFlagRuleService.deleteRule(id, actorUserId);
        return ResponseEntity.noContent().build();
    }
}
