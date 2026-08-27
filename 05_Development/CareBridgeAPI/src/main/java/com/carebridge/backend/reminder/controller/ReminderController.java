package com.carebridge.backend.reminder.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.reminder.dto.CreateMedicationReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderResponse;
import com.carebridge.backend.reminder.dto.CreateVaccinationReminderRequest;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.dto.SnoozeReminderRequest;
import com.carebridge.backend.reminder.dto.TodayTaskItem;
import com.carebridge.backend.reminder.dto.UpdateReminderRequest;
import com.carebridge.backend.reminder.dto.VaccinationSuggestionDto;
import com.carebridge.backend.reminder.service.IReminderService;
import com.carebridge.backend.reminder.service.ITodayTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

    private final IReminderService reminderService;
    private final ITodayTaskService todayTaskService;
    private final UnifiedTaskActionFacade taskActionFacade;

    // UC45: Create appointment reminder
    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<CreateReminderResponse>> createReminder(
            @Valid @RequestBody CreateReminderRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = reminderService.createReminder(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Reminder created successfully"));
    }

    // View all reminders
    @GetMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<ReminderDetailResponse>>> getAllReminders(Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = reminderService.getAllReminders(callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC212: View reminder detail
    @GetMapping("/{reminderId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ReminderDetailResponse>> getReminderDetail(
            @PathVariable UUID reminderId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = reminderService.getReminderDetail(reminderId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC46: Create medication reminder
    @PostMapping("/medication")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<CreateReminderResponse>> createMedicationReminder(
            @Valid @RequestBody CreateMedicationReminderRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = reminderService.createMedicationReminder(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Medication reminder created successfully"));
    }

    // UC47: Create vaccination reminder
    @PostMapping("/vaccination")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<CreateReminderResponse>> createVaccinationReminder(
            @Valid @RequestBody CreateVaccinationReminderRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = reminderService.createVaccinationReminder(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Vaccination reminder created successfully"));
    }

    // UC47: Get vaccination reminder suggestions
    @GetMapping("/vaccination/suggestions")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<VaccinationSuggestionDto>>> getVaccinationSuggestions(
            @RequestParam UUID babyId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var suggestions = reminderService.getVaccinationSuggestions(babyId, callerId);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    // UC48: Update reminder fields
    @PatchMapping("/{reminderId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ReminderDetailResponse>> updateReminder(
            @PathVariable UUID reminderId,
            @Valid @RequestBody UpdateReminderRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = reminderService.updateReminder(reminderId, request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminder updated successfully"));
    }

    // UC48: Snooze reminder
    @PatchMapping("/{reminderId}/snooze")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ReminderDetailResponse>> snoozeReminder(
            @PathVariable UUID reminderId,
            @Valid @RequestBody SnoozeReminderRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = reminderService.snoozeReminder(reminderId, request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminder snoozed successfully"));
    }

    // UC48: Complete reminder
    @PatchMapping("/{reminderId}/complete")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ReminderDetailResponse>> completeReminder(
            @PathVariable UUID reminderId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        taskActionFacade.apply(callerId, TaskKind.REMINDER, reminderId,
                new TaskActionRequest(TaskAction.COMPLETE,
                        legacyRequestId(callerId, reminderId, TaskAction.COMPLETE), null));
        var response = reminderService.getReminderDetail(reminderId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminder completed successfully"));
    }

    // UC48: Skip reminder
    @RequestMapping(value = "/{reminderId}/skip", method = {RequestMethod.POST, RequestMethod.PATCH})
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ReminderDetailResponse>> skipReminder(
            @PathVariable UUID reminderId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        taskActionFacade.apply(callerId, TaskKind.REMINDER, reminderId,
                new TaskActionRequest(TaskAction.SKIP,
                        legacyRequestId(callerId, reminderId, TaskAction.SKIP), "USER_CHOICE"));
        var response = reminderService.getReminderDetail(reminderId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminder skipped successfully"));
    }

    @DeleteMapping("/{reminderId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<Void> deleteReminder(
            @PathVariable UUID reminderId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        reminderService.deleteReminder(reminderId, callerId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reminderId}/enable")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ReminderDetailResponse>> enableReminder(
            @PathVariable UUID reminderId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = reminderService.enableReminder(reminderId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminder enabled successfully"));
    }

    @DeleteMapping("/{reminderId}/permanent")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<Void> hardDeleteReminder(
            @PathVariable UUID reminderId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        reminderService.hardDeleteReminder(reminderId, callerId);
        return ResponseEntity.noContent().build();
    }

    // UC49: View today tasks
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<List<TodayTaskItem>>> getTodayTasks(
            @RequestHeader(value = "X-User-Timezone", required = false) String timezoneHeader,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        ZoneId timezone = resolveTimezone(timezoneHeader);
        var tasks = todayTaskService.getTodayTasks(callerId, timezone);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    private ZoneId resolveTimezone(String header) {
        if (header == null || header.isBlank()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(header);
        } catch (Exception e) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    private static UUID legacyRequestId(UUID actorId, UUID taskId, TaskAction action) {
        String identity = "legacy-reminder-action-v2|" + actorId + "|" + taskId + "|" + action;
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }
}
