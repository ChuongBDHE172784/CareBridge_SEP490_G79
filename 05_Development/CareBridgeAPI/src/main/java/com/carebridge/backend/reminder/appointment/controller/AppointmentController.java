package com.carebridge.backend.reminder.appointment.controller;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.reminder.dto.CreateReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderResponse;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.dto.UpdateReminderRequest;
import com.carebridge.backend.reminder.appointment.dto.AppointmentResponse;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.service.IReminderService;
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

/** Explicit non-recurring appointment resource. Legacy /reminders remains readable. */
@RestController
@RequestMapping("/api/v1/appointments")
@PreAuthorize("hasRole('MOTHER')")
public class AppointmentController {
    private final IReminderService reminderService;

    public AppointmentController(IReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(
            @Valid @RequestBody CreateReminderRequest request, Principal principal) {
        requireAppointment(request);
        UUID ownerId = SecurityUtils.requireCurrentUserId(principal);
        request.setRecurrenceType(RecurrenceType.NONE);
        CreateReminderResponse response = reminderService.createReminder(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toAppointment(response), "Appointment created successfully"));
    }

    @GetMapping
    public ApiResponse<List<AppointmentResponse>> list(Principal principal) {
        UUID ownerId = SecurityUtils.requireCurrentUserId(principal);
        return ApiResponse.success(reminderService.getAllReminders(ownerId).stream()
                .filter(this::isAppointment).map(this::toAppointment).toList());
    }

    @GetMapping("/{appointmentId}")
    public ApiResponse<AppointmentResponse> get(@PathVariable UUID appointmentId, Principal principal) {
        ReminderDetailResponse response = reminderService.getReminderDetail(
                appointmentId, SecurityUtils.requireCurrentUserId(principal));
        requireAppointment(response);
        return ApiResponse.success(toAppointment(response));
    }

    @PatchMapping("/{appointmentId}")
    public ApiResponse<AppointmentResponse> update(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody UpdateReminderRequest request,
            Principal principal) {
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "APPOINTMENT_REQUEST_REQUIRED",
                    "Appointment update is required");
        }
        if (request.getRecurrenceType() != null && request.getRecurrenceType() != RecurrenceType.NONE) {
            throw recurrenceRejected();
        }
        if (request.getRecurrenceEndDate() != null
                || Boolean.TRUE.equals(request.getRecurrenceEndDateSet())) {
            throw recurrenceRejected();
        }
        UUID ownerId = SecurityUtils.requireCurrentUserId(principal);
        ReminderDetailResponse existing = reminderService.getReminderDetail(appointmentId, ownerId);
        requireAppointment(existing);
        // The explicit appointment resource never carries recurrence, including
        // when a legacy recurring appointment is edited through this route.
        request.setRecurrenceType(RecurrenceType.NONE);
        request.setRecurrenceEndDate(null);
        request.setRecurrenceEndDateSet(true);
        return ApiResponse.success(toAppointment(
                reminderService.updateReminder(appointmentId, request, ownerId)));
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID appointmentId, Principal principal) {
        UUID ownerId = SecurityUtils.requireCurrentUserId(principal);
        requireAppointment(reminderService.getReminderDetail(appointmentId, ownerId));
        reminderService.deleteReminder(appointmentId, ownerId);
        return ResponseEntity.noContent().build();
    }

    private void requireAppointment(CreateReminderRequest request) {
        if (request == null || request.getReminderType() != ReminderType.APPOINTMENT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "APPOINTMENT_TYPE_REQUIRED",
                    "Appointments must use reminderType APPOINTMENT");
        }
        if (request.getRecurrenceType() != null && request.getRecurrenceType() != RecurrenceType.NONE) {
            throw recurrenceRejected();
        }
    }

    private void requireAppointment(ReminderDetailResponse response) {
        if (response == null || !ReminderType.APPOINTMENT.name().equals(response.getReminderType())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found");
        }
    }

    private boolean isAppointment(ReminderDetailResponse response) {
        return ReminderType.APPOINTMENT.name().equals(response.getReminderType());
    }

    private AppointmentResponse toAppointment(CreateReminderResponse response) {
        return new AppointmentResponse(response.getId(), response.getReminderType(), response.getTitle(),
                response.getScheduledAt(), response.getStatus(), response.getCreatedAt(), null,
                response.getNotificationOffsetsMinutes(), response.getTimeZone());
    }

    private AppointmentResponse toAppointment(ReminderDetailResponse response) {
        return new AppointmentResponse(response.getId(), response.getReminderType(), response.getTitle(),
                response.getScheduledAt(), response.getStatus(), response.getCreatedAt(), response.getUpdatedAt(),
                response.getNotificationOffsetsMinutes(), response.getTimeZone());
    }

    private static BusinessException recurrenceRejected() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "APPOINTMENT_RECURRENCE_NOT_SUPPORTED",
                "Appointments are one-date events and cannot recur");
    }
}
