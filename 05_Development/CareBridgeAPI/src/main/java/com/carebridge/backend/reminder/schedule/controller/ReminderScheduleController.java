package com.carebridge.backend.reminder.schedule.controller;

import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.reminder.schedule.dto.CreateReminderScheduleRequest;
import com.carebridge.backend.reminder.schedule.dto.ReminderScheduleResponse;
import com.carebridge.backend.reminder.schedule.dto.UpdateReminderScheduleRequest;
import com.carebridge.backend.reminder.schedule.service.ReminderScheduleService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reminder-schedules")
@PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
public class ReminderScheduleController {
    private final ReminderScheduleService scheduleService;

    public ReminderScheduleController(ReminderScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReminderScheduleResponse>> create(
            @Valid @RequestBody CreateReminderScheduleRequest request, Principal principal) {
        UUID ownerId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(scheduleService.create(ownerId, request)));
    }

    @GetMapping
    public ApiResponse<List<ReminderScheduleResponse>> list(Principal principal) {
        return ApiResponse.success(scheduleService.list(SecurityUtils.requireCurrentUserId(principal)));
    }

    @GetMapping("/{scheduleId}")
    public ApiResponse<ReminderScheduleResponse> get(@PathVariable UUID scheduleId, Principal principal) {
        return ApiResponse.success(scheduleService.get(SecurityUtils.requireCurrentUserId(principal), scheduleId));
    }

    @PatchMapping("/{scheduleId}")
    public ApiResponse<ReminderScheduleResponse> update(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody UpdateReminderScheduleRequest request,
            Principal principal) {
        return ApiResponse.success(scheduleService.update(
                SecurityUtils.requireCurrentUserId(principal), scheduleId, request));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(@PathVariable UUID scheduleId, Principal principal) {
        scheduleService.delete(SecurityUtils.requireCurrentUserId(principal), scheduleId);
        return ResponseEntity.noContent().build();
    }
}
