package com.carebridge.backend.checklist.history.controller;

import com.carebridge.backend.checklist.history.dto.ChecklistHistoryPageResponse;
import com.carebridge.backend.checklist.history.service.ChecklistHistoryService;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checklists")
@RequiredArgsConstructor
public class ChecklistHistoryController {

    private final ChecklistHistoryService historyService;

    @GetMapping("/history")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ChecklistHistoryPageResponse>> listHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ChecklistTargetSubject targetSubject,
            Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                historyService.listHistory(actorId, targetSubject, page, size)));
    }
}
