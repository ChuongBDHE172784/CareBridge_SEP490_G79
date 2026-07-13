package com.carebridge.backend.reminder.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.dto.CreateMedicationReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderResponse;
import com.carebridge.backend.reminder.dto.CreateVaccinationReminderRequest;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.dto.SnoozeReminderRequest;
import com.carebridge.backend.reminder.dto.UpdateReminderRequest;
import com.carebridge.backend.reminder.dto.VaccinationSuggestionDto;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.IReminderService;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ReminderServiceImpl implements IReminderService {

    private final ReminderRepository reminderRepository;
    private final INotificationService notificationService;
    private final AuditService auditService;
    private final BabyProfileRepository babyProfileRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;

    // ─── UC45: Create Appointment Reminder ────────────────────────────────────

    @Override
    public CreateReminderResponse createReminder(CreateReminderRequest request, UUID callerId) {
        validateScheduledAt(request.getScheduledAt());

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

        String fcmJobId = notificationService.scheduleFcmPush(
                callerId, request.getTitle(), "Reminder: " + request.getTitle(), request.getScheduledAt());
        saved.setFcmJobId(fcmJobId);
        reminderRepository.save(saved);

        auditService.log(AuditAction.REMINDER_CREATED, callerId,
                "Reminder", saved.getId().toString(), "created");

        return toCreateResponse(saved);
    }

    // ─── UC212: View Reminder Detail ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReminderDetailResponse getReminderDetail(UUID reminderId, UUID callerId) {
        Reminder reminder = findReminderById(reminderId);
        requireOwnership(reminder, callerId);
        return toDetailResponse(reminder);
    }

    // ─── View All Reminders ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ReminderDetailResponse> getAllReminders(UUID callerId) {
        return reminderRepository.findByOwnerUserIdOrderByScheduledAtDesc(callerId)
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    // ─── UC46: Create Medication Reminder ─────────────────────────────────────

    @Override
    public CreateReminderResponse createMedicationReminder(CreateMedicationReminderRequest request, UUID callerId) {
        validateScheduledAt(request.getScheduledAt());

        Reminder reminder = Reminder.builder()
                .ownerUserId(callerId)
                .journeyId(request.getJourneyId())
                .reminderType(ReminderType.MEDICATION)
                .title(request.getTitle())
                .scheduledAt(request.getScheduledAt())
                .recurrenceType(request.getRecurrenceType())
                .recurrenceEndDate(request.getRecurrenceEndDate())
                .status(ReminderStatus.PENDING)
                .build();

        Reminder saved = reminderRepository.save(reminder);

        String fcmJobId = notificationService.scheduleFcmPush(
                callerId, request.getTitle(), "Medication reminder: " + request.getTitle(), request.getScheduledAt());
        saved.setFcmJobId(fcmJobId);
        reminderRepository.save(saved);

        auditService.log(AuditAction.REMINDER_CREATED, callerId,
                "Reminder", saved.getId().toString(), "medication reminder created");

        return toCreateResponse(saved);
    }

    // ─── UC47: Create Vaccination Reminder ────────────────────────────────────

    @Override
    public CreateReminderResponse createVaccinationReminder(CreateVaccinationReminderRequest request, UUID callerId) {
        requireBabyOwnership(request.getBabyId(), callerId);
        validateScheduledAt(request.getScheduledAt());

        Reminder reminder = Reminder.builder()
                .ownerUserId(callerId)
                .journeyId(request.getJourneyId())
                .babyId(request.getBabyId())
                .reminderType(ReminderType.VACCINATION)
                .title(request.getTitle())
                .scheduledAt(request.getScheduledAt())
                .recurrenceType(request.getRecurrenceType())
                .recurrenceEndDate(request.getRecurrenceEndDate())
                .status(ReminderStatus.PENDING)
                .build();

        Reminder saved = reminderRepository.save(reminder);

        String fcmJobId = notificationService.scheduleFcmPush(
                callerId, request.getTitle(), "Vaccination reminder: " + request.getTitle(), request.getScheduledAt());
        saved.setFcmJobId(fcmJobId);
        reminderRepository.save(saved);

        auditService.log(AuditAction.REMINDER_CREATED, callerId,
                "Reminder", saved.getId().toString(), "vaccination reminder created");

        return toCreateResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VaccinationSuggestionDto> getVaccinationSuggestions(UUID babyId, UUID callerId) {
        requireBabyOwnership(babyId, callerId);

        return vaccinationRecordRepository
                .findByBabyIdAndStatus(babyId, VaccinationRecordStatus.SCHEDULED)
                .stream()
                .map(r -> VaccinationSuggestionDto.builder()
                        .babyId(babyId)
                        .vaccineName(r.getVaccineName())
                        .doseNumber(r.getDoseNumber())
                        .scheduledDate(r.getScheduledDate())
                        .build())
                .toList();
    }

    // ─── UC48: Update Reminder ────────────────────────────────────────────────

    @Override
    public ReminderDetailResponse updateReminder(UUID reminderId, UpdateReminderRequest request, UUID callerId) {
        Reminder reminder = findReminderById(reminderId);
        requireOwnership(reminder, callerId);
        requireMutableState(reminder);

        if (request.getTitle() != null) {
            reminder.setTitle(request.getTitle());
        }

        if (request.getScheduledAt() != null) {
            validateScheduledAt(request.getScheduledAt());
            // Cancel old FCM job and reschedule
            if (reminder.getFcmJobId() != null) {
                notificationService.cancelFcmJob(reminder.getFcmJobId());
            }
            reminder.setScheduledAt(request.getScheduledAt());
            String newJobId = notificationService.scheduleFcmPush(
                    callerId, reminder.getTitle(), "Reminder: " + reminder.getTitle(), request.getScheduledAt());
            reminder.setFcmJobId(newJobId);
        }

        if (request.getRecurrenceType() != null) {
            reminder.setRecurrenceType(request.getRecurrenceType());
        }

        if (request.getRecurrenceEndDate() != null) {
            reminder.setRecurrenceEndDate(request.getRecurrenceEndDate());
        }

        Reminder saved = reminderRepository.save(reminder);
        return toDetailResponse(saved);
    }

    // ─── UC48: Snooze Reminder ────────────────────────────────────────────────

    @Override
    public ReminderDetailResponse snoozeReminder(UUID reminderId, SnoozeReminderRequest request, UUID callerId) {
        Reminder reminder = findReminderById(reminderId);
        requireOwnership(reminder, callerId);
        requireMutableState(reminder);

        Instant snoozedUntil = request.getSnoozedUntil();
        Instant now = Instant.now();

        if (!snoozedUntil.isAfter(now)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "REM-005",
                    "snoozedUntil must be in the future");
        }

        if (snoozedUntil.isAfter(now.plus(24, ChronoUnit.HOURS))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "REM-008",
                    "snoozedUntil cannot be more than 24 hours in the future");
        }

        if (reminder.getFcmJobId() != null) {
            notificationService.cancelFcmJob(reminder.getFcmJobId());
        }

        reminder.setStatus(ReminderStatus.SNOOZED);
        reminder.setSnoozedUntil(snoozedUntil);

        String newJobId = notificationService.scheduleFcmPush(
                callerId, reminder.getTitle(), "Snoozed reminder: " + reminder.getTitle(), snoozedUntil);
        reminder.setFcmJobId(newJobId);

        Reminder saved = reminderRepository.save(reminder);
        return toDetailResponse(saved);
    }

    // ─── UC48: Complete Reminder ──────────────────────────────────────────────

    @Override
    public ReminderDetailResponse completeReminder(UUID reminderId, UUID callerId) {
        Reminder reminder = findReminderById(reminderId);
        requireOwnership(reminder, callerId);
        requirePendingState(reminder, "REM-007",
                "Reminder is already in a terminal state and cannot be completed");

        if (reminder.getFcmJobId() != null) {
            notificationService.cancelFcmJob(reminder.getFcmJobId());
        }

        reminder.setStatus(ReminderStatus.COMPLETED);
        Reminder saved = reminderRepository.save(reminder);

        auditService.log(AuditAction.REMINDER_COMPLETED, callerId,
                "Reminder", reminderId.toString(), "completed");

        return toDetailResponse(saved);
    }

    // ─── UC48: Skip Reminder ──────────────────────────────────────────────────

    @Override
    public ReminderDetailResponse skipReminder(UUID reminderId, UUID callerId) {
        Reminder reminder = findReminderById(reminderId);
        requireOwnership(reminder, callerId);
        requirePendingState(reminder, "REM-011",
                "Reminder is not in a skippable state");

        if (reminder.getFcmJobId() != null) {
            notificationService.cancelFcmJob(reminder.getFcmJobId());
        }

        reminder.setStatus(ReminderStatus.SKIPPED);

        Reminder saved = reminderRepository.save(reminder);

        auditService.log(AuditAction.REMINDER_SKIPPED, callerId,
                "Reminder", reminderId.toString(), "skipped");

        return toDetailResponse(saved);
    }

    @Override
    public void deleteReminder(UUID reminderId, UUID callerId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REM-015",
                        "Reminder not found"));

        if (!reminder.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "REM-016",
                    "Access denied to reminder");
        }

        if (reminder.getStatus() == ReminderStatus.CANCELLED) {
            return;
        }

        if (reminder.getStatus() == ReminderStatus.COMPLETED || reminder.getStatus() == ReminderStatus.SKIPPED) {
            throw new BusinessException(HttpStatus.CONFLICT, "REM-017",
                    "Reminder is in a terminal state and cannot be deleted");
        }

        if (reminder.getFcmJobId() != null) {
            notificationService.cancelFcmJob(reminder.getFcmJobId());
        }

        reminder.setStatus(ReminderStatus.CANCELLED);
        reminderRepository.save(reminder);

        auditService.log(AuditAction.REMINDER_CANCELLED, callerId,
                "Reminder", reminderId.toString(), "cancelled");
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void validateScheduledAt(Instant scheduledAt) {
        if (scheduledAt.isBefore(Instant.now().plus(5, ChronoUnit.MINUTES))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "REM-001",
                    "scheduledAt must be at least 5 minutes in the future");
        }
    }

    private Reminder findReminderById(UUID reminderId) {
        return reminderRepository.findById(reminderId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REM-006",
                        "Reminder not found: " + reminderId));
    }

    private void requireOwnership(Reminder reminder, UUID callerId) {
        if (!reminder.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "REM-004",
                    "Access denied to reminder");
        }
    }

    private void requireBabyOwnership(UUID babyId, UUID callerId) {
        babyProfileRepository.findByIdAndOwnerUserId(babyId, callerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "REM-003",
                        "Baby profile not found or not owned by caller"));
    }

    /** ADR-REM-STATE-001: cancelled reminders are immutable terminal states. */
    private void requireMutableState(Reminder reminder) {
        ReminderStatus status = reminder.getStatus();
        if (status == ReminderStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "REM-007",
                    "Reminder in terminal state " + status + " cannot be modified");
        }
    }

    private void requirePendingState(Reminder reminder, String code, String message) {
        if (reminder.getStatus() != ReminderStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, code, message);
        }
    }

    private CreateReminderResponse toCreateResponse(Reminder saved) {
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

    private ReminderDetailResponse toDetailResponse(Reminder reminder) {
        return ReminderDetailResponse.builder()
                .id(reminder.getId())
                .reminderType(reminder.getReminderType() != null ? reminder.getReminderType().name() : null)
                .title(reminder.getTitle())
                .scheduledAt(reminder.getScheduledAt())
                .recurrenceType(reminder.getRecurrenceType() != null ? reminder.getRecurrenceType().name() : null)
                .status(reminder.getStatus().name())
                .snoozedUntil(reminder.getSnoozedUntil())
                .createdAt(reminder.getCreatedAt())
                .updatedAt(reminder.getUpdatedAt())
                .build();
    }
}
