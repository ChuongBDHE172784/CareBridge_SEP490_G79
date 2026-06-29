package com.carebridge.backend.reminder.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.reminder.dto.CreateReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderResponse;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.service.IReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final IReminderService reminderService;

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
}
