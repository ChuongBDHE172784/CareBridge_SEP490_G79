package com.carebridge.backend.reminder.appointment.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.reminder.appointment.dto.SharedAppointmentResponse;
import com.carebridge.backend.reminder.appointment.service.CareGroupAppointmentService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Explicit read-only appointment routes for FAMILY calendar views. */
@RestController
@RequestMapping("/api/v1/care-groups/{careGroupId}/appointments")
@PreAuthorize("isAuthenticated()")
public class CareGroupAppointmentController {

    private final CareGroupAppointmentService appointmentService;

    public CareGroupAppointmentController(CareGroupAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SharedAppointmentResponse>>> list(
            @PathVariable UUID careGroupId,
            Principal principal) {
        UUID actorUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.list(actorUserId, careGroupId)));
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<ApiResponse<SharedAppointmentResponse>> get(
            @PathVariable UUID careGroupId,
            @PathVariable UUID appointmentId,
            Principal principal) {
        UUID actorUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.get(actorUserId, careGroupId, appointmentId)));
    }
}
