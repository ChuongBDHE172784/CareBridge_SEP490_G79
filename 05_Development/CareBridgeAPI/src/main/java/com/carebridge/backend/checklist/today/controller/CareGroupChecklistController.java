package com.carebridge.backend.checklist.today.controller;

import com.carebridge.backend.checklist.history.dto.ChecklistHistoryPageResponse;
import com.carebridge.backend.checklist.history.service.ChecklistHistoryService;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistActionResponse;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistResponse;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.service.CareGroupChecklistService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Explicit FAMILY checklist projections bound to one selected care group. */
@RestController
@RequestMapping("/api/v1/care-groups/{careGroupId}/checklists")
@RequiredArgsConstructor
public class CareGroupChecklistController {
    private final CareGroupChecklistService checklistService;
    private final ChecklistHistoryService historyService;

    @GetMapping("/current/tasks")
    @PreAuthorize("hasRole('FAMILY')")
    public CurrentChecklistResponse getCurrentTasks(
            @PathVariable UUID careGroupId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-User-Timezone", required = false) String timezone,
            Principal principal) {
        return checklistService.getCurrentTasks(
                SecurityUtils.requireCurrentUserId(principal), careGroupId, date, timezone);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('FAMILY')")
    public ResponseEntity<ApiResponse<ChecklistHistoryPageResponse>> listHistory(
            @PathVariable UUID careGroupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ChecklistTargetSubject targetSubject,
            Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                historyService.listSharedHistory(actorId, careGroupId, targetSubject, page, size)));
    }

    @PostMapping("/tasks/{taskId}/actions")
    @PreAuthorize("hasRole('FAMILY')")
    public CurrentChecklistActionResponse applyAction(
            @PathVariable UUID careGroupId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskActionRequest request,
            Principal principal) {
        return checklistService.applyAction(
                SecurityUtils.requireCurrentUserId(principal), careGroupId, taskId, request);
    }
}
