package com.carebridge.backend.reminder.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.dto.CreateReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderResponse;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.IReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ReminderServiceImpl implements IReminderService {

    private final ReminderRepository reminderRepository;
    private final INotificationService notificationService;
    private final AuditService auditService;

    @Override
    public CreateReminderResponse createReminder(CreateReminderRequest request, UUID callerId) {
        // C1: scheduledAt >= now + 5 minutes
        Instant minScheduled = Instant.now().plus(5, ChronoUnit.MINUTES);
        if (request.getScheduledAt().isBefore(minScheduled)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "REM-001",
                    "scheduledAt must be at least 5 minutes in the future");
        }

        // C4: accountId from JWT
        Reminder reminder = Reminder.builder()
                .ownerUserId(callerId)
                .journeyId(request.getJourneyId())
                .babyId(request.getBabyId())
                .reminderType(request.getReminderType())
                .title(request.getTitle())
                .scheduledAt(request.getScheduledAt())
                .recurrenceType(request.getRecurrenceType())
                .recurrenceEndDate(request.getRecurrenceEndDate())
                .status(ReminderStatus.PENDING)
                .build();

        Reminder saved = reminderRepository.save(reminder);

        // C2: FCM scheduling synchronous
        String fcmJobId = notificationService.scheduleFcmPush(
                callerId, request.getTitle(), "Reminder: " + request.getTitle(), request.getScheduledAt());
        saved.setFcmJobId(fcmJobId);
        reminderRepository.save(saved);

        // C3: emit audit event
        auditService.log(AuditAction.REMINDER_CREATED, callerId,
                "Reminder", saved.getId().toString(), "created");

        return CreateReminderResponse.builder()
                .id(saved.getId())
                .reminderType(saved.getReminderType().name())
                .title(saved.getTitle())
                .scheduledAt(saved.getScheduledAt())
                .recurrenceType(saved.getRecurrenceType() != null ? saved.getRecurrenceType().name() : null)
                .status(saved.getStatus().name())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderDetailResponse getReminderDetail(UUID reminderId, UUID callerId) {
        // REM-006: not found → 404
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REM-006",
                        "Reminder not found: " + reminderId));

        // C1: ownership check — REM-004 / 403
        if (!reminder.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "REM-004",
                    "Access denied to reminder");
        }

        // ADR-REM-002: CANCELLED reminders are still viewable — return 200
        // C2: NO dosage/prescription fields in response (BR-SAFETY)
        return ReminderDetailResponse.builder()
                .id(reminder.getId())
                .reminderType(reminder.getReminderType().name())
                .title(reminder.getTitle())
                .scheduledAt(reminder.getScheduledAt())
                .recurrenceType(reminder.getRecurrenceType() != null ? reminder.getRecurrenceType().name() : null)
                .status(reminder.getStatus().name())
                .createdAt(reminder.getCreatedAt())
                .updatedAt(reminder.getUpdatedAt())
                .build();
    }
}
