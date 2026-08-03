package com.carebridge.backend.checklist.today.controller;

import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistActionResponse;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.service.CurrentChecklistService;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Explicit checklist resource. The legacy /tasks/today endpoint remains a
 * compatibility read and is intentionally not changed here. */
@RestController
@RequestMapping("/api/v1/checklists")
public class CurrentChecklistController {
    private final CurrentChecklistService checklistService;
    private final UnifiedTaskActionFacade actionFacade;

    public CurrentChecklistController(
            CurrentChecklistService checklistService, UnifiedTaskActionFacade actionFacade) {
        this.checklistService = checklistService;
        this.actionFacade = actionFacade;
    }

    @GetMapping("/current/tasks")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public CurrentChecklistResponse getCurrentTasks(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-User-Timezone", required = false) String timezone,
            Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        return checklistService.getCurrentTasks(actorId, date, timezone);
    }

    @PostMapping("/tasks/{taskId}/actions")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public CurrentChecklistActionResponse applyAction(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskActionRequest request,
            Principal principal) {
        if (request == null || (request.action() != TaskAction.COMPLETE
                && request.action() != TaskAction.REOPEN)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CHECKLIST_ACTION_INVALID",
                    "Only COMPLETE and REOPEN are supported for checklist tasks");
        }
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        TaskActionResponse response = actionFacade.apply(actorId,
                com.carebridge.backend.checklist.today.model.TaskKind.CHECKLIST,
                taskId, request);
        return new CurrentChecklistActionResponse(response.taskId(), response.instanceId(),
                response.action(), response.previousStatus(), response.status(),
                response.appliedAt(), response.idempotentReplay(), response.correlationId());
    }
}
