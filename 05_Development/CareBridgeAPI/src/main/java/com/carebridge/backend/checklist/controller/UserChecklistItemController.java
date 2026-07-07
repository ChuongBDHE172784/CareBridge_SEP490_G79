package com.carebridge.backend.checklist.controller;

import com.carebridge.backend.checklist.dto.*;
import com.carebridge.backend.checklist.service.IUserChecklistItemService;
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

    private final IUserChecklistItemService checklistService;

    // UC50: Add custom checklist item
    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> addItem(
            @Valid @RequestBody AddChecklistItemRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = checklistService.addItem(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Checklist item added"));
    }

    // UC50: Import items from template
    @PostMapping("/import")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<ChecklistItemResponse>>> importFromTemplate(
            @Valid @RequestBody ImportFromTemplateRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = checklistService.importFromTemplate(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Items imported from template"));
    }

    // UC50: List checklist items
    @GetMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<ChecklistItemResponse>>> listItems(
            @RequestParam(required = false) UUID journeyId,
            @RequestParam(required = false) UUID babyId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = checklistService.listItems(callerId, journeyId, babyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC50: Toggle complete/incomplete
    @PatchMapping("/{itemId}/toggle")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> toggleComplete(
            @PathVariable UUID itemId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = checklistService.toggleComplete(itemId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC50: Update item text / category / order (custom items only)
    @PutMapping("/{itemId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateChecklistItemRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = checklistService.updateItem(itemId, request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Checklist item updated"));
    }

    // UC50: Delete checklist item
    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable UUID itemId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        checklistService.deleteItem(itemId, callerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Checklist item deleted"));
    }
}
