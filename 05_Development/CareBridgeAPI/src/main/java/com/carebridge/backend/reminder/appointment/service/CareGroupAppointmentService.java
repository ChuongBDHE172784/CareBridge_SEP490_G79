package com.carebridge.backend.reminder.appointment.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.appointment.dto.SharedAppointmentResponse;
import com.carebridge.backend.reminder.appointment.policy.CareGroupAppointmentScopeResolver;
import com.carebridge.backend.reminder.appointment.policy.CareGroupAppointmentScopeResolver.AppointmentScope;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.notification.service.AppointmentNotificationScheduleService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only FAMILY appointment projection for one explicit care group. */
@Service
public class CareGroupAppointmentService {

    private final ReminderRepository reminderRepository;
    private final CareGroupAppointmentScopeResolver scopeResolver;
    private final AppointmentNotificationScheduleService scheduleService;

    public CareGroupAppointmentService(
            ReminderRepository reminderRepository,
            CareGroupAppointmentScopeResolver scopeResolver,
            AppointmentNotificationScheduleService scheduleService) {
        this.reminderRepository = reminderRepository;
        this.scopeResolver = scopeResolver;
        this.scheduleService = scheduleService;
    }

    @Transactional(readOnly = true)
    public List<SharedAppointmentResponse> list(UUID actorUserId, UUID careGroupId) {
        AppointmentScope scope = requireScope(actorUserId, careGroupId);
        List<Reminder> appointments = reminderRepository.findSharedAppointments(
                scope.ownerUserId(), scope.linkedJourneyId(), scope.linkedBabyProfileId());
        requireSameScope(actorUserId, careGroupId, scope);
        return appointments.stream()
                .map(reminder -> toResponse(reminder, scope.careGroupId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SharedAppointmentResponse get(UUID actorUserId, UUID careGroupId, UUID appointmentId) {
        AppointmentScope scope = requireScope(actorUserId, careGroupId);
        Reminder appointment = reminderRepository.findSharedAppointment(
                        appointmentId,
                        scope.ownerUserId(),
                        scope.linkedJourneyId(),
                        scope.linkedBabyProfileId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found"));
        requireSameScope(actorUserId, careGroupId, scope);
        return toResponse(appointment, scope.careGroupId());
    }

    private AppointmentScope requireScope(UUID actorUserId, UUID careGroupId) {
        AppointmentScope scope = scopeResolver.resolveView(actorUserId, careGroupId);
        if (scope == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND",
                    "Appointment scope not found");
        }
        return scope;
    }

    private void requireSameScope(
            UUID actorUserId, UUID careGroupId, AppointmentScope initial) {
        AppointmentScope current = scopeResolver.resolveView(actorUserId, careGroupId);
        if (current == null
                || !initial.ownerUserId().equals(current.ownerUserId())
                || !initial.linkedContexts().equals(current.linkedContexts())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND",
                    "Appointment scope not found");
        }
    }

    private SharedAppointmentResponse toResponse(Reminder reminder, UUID careGroupId) {
        return new SharedAppointmentResponse(
                reminder.getId(),
                careGroupId,
                reminder.getReminderType() == null ? null : reminder.getReminderType().name(),
                reminder.getTitle(),
                reminder.getScheduledAt(),
                reminder.getStatus() == null ? null : reminder.getStatus().name(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt(),
                scheduleService == null ? List.of() : scheduleService.currentOffsets(reminder.getId()),
                scheduleService == null ? null : scheduleService.currentTimeZone(reminder.getId()));
    }
}
