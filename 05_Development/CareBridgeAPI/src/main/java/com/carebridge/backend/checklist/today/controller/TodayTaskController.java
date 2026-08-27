package com.carebridge.backend.checklist.today.controller;

import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.checklist.today.service.UnifiedTodayTaskService;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TodayTaskController {
    private final UnifiedTodayTaskService todayTaskService;
    private final UnifiedTaskActionFacade actionFacade;

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public TodayTasksResponse getTodayTasks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-User-Timezone", required = false) String timezone,
            Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        return todayTaskService.getTodayTasks(actorId, date, timezone);
    }

    @PostMapping("/{taskKind}/{taskId}/actions")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public TaskActionResponse applyAction(
            @PathVariable TaskKind taskKind,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskActionRequest request,
            Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        return actionFacade.apply(actorId, taskKind, taskId, request);
    }
}
