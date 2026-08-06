package com.carebridge.backend.checklist.controller;

import com.carebridge.backend.checklist.dto.*;
import com.carebridge.backend.checklist.service.UserCreatedChecklistTaskService;
import com.carebridge.backend.checklist.service.ChecklistV2CompatibilityMutationService;
import com.carebridge.backend.checklist.service.OptionalChecklistTemplateImportService;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionResult;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-checklist-items")
@RequiredArgsConstructor
public class UserChecklistItemController {

    private final UserCreatedChecklistTaskService userCreatedTaskService;
    private final ChecklistV2CompatibilityMutationService v2MutationService;
    private final OptionalChecklistTemplateImportService optionalTemplateImportService;

    // UC50: Add custom checklist item
    @PostMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> addItem(
            @Valid @RequestBody AddChecklistItemRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = userCreatedTaskService.create(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Checklist item added"));
    }

    // UC50: Import items from template
    @PostMapping("/import")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<ChecklistItemResponse>>> importFromTemplate(
            @Valid @RequestBody ImportFromTemplateRequest request,
            Principal principal) {
        SecurityUtils.requireCurrentUserId(principal);
        throw retiredMutation();
    }

    // UC50: List checklist items
    @GetMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<ChecklistItemResponse>>> listItems(
            @RequestParam(required = false) UUID journeyId,
            @RequestParam(required = false) UUID babyId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        // Checklist v2 is the only source now. The legacy merge that used to fill in
        // rows from preparation_checklist_items was removed once V20260806176000
        // proved every legacy row has a v2 counterpart; keeping it would have hidden
        // any future orphan instead of surfacing it.
        return ResponseEntity.ok(ApiResponse.success(
                userCreatedTaskService.listAuthorized(callerId, journeyId, babyId)));
    }

    // UC50: Toggle complete/incomplete
    @PatchMapping("/{itemId}/toggle")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> toggleComplete(
            @PathVariable UUID itemId,
            Principal principal) {
        SecurityUtils.requireCurrentUserId(principal);
        throw retiredMutation();
    }

    // UC50: Update item text / category / order (custom items only)
    @PutMapping("/{itemId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateChecklistItemRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        v2MutationService.rejectUpdate(itemId, callerId);
        throw retiredMutation();
    }

    // UC50: Delete checklist item
    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID itemId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        v2MutationService.delete(itemId, callerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/from-template")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ChecklistDistributionResult>> selfAssignFromTemplate(
            @Valid @RequestBody SelfAssignChecklistTemplateRequest request,
            Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        ChecklistDistributionResult result = optionalTemplateImportService.selfAssign(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Checklist added to Today tasks"));
    }

    private static BusinessException retiredMutation() {
        return new BusinessException(HttpStatus.GONE, "CHECKLIST_LEGACY_ROUTE_RETIRED",
                "Use the unified Today task APIs");
    }
}
