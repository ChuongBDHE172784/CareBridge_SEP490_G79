package com.carebridge.backend.reminder.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.RequiredAuditEvent;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.today.provider.ReminderOccurrenceIdFactory;
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
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.notification.service.CareGroupAppointmentNotificationService;
import com.carebridge.backend.reminder.notification.service.AppointmentNotificationScheduleService;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.IReminderService;
import com.carebridge.backend.reminder.service.ReminderActionAuditContext;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ReminderServiceImpl implements IReminderService {

    private final ReminderRepository reminderRepository;
    private final INotificationService notificationService;
    private final AuditService auditService;
    private final BabyProfileRepository babyProfileRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final EntityManager entityManager;
    private final AppointmentNotificationScheduleService appointmentNotificationScheduleService;
    private final CareGroupAppointmentNotificationService careGroupAppointmentNotificationService;

    /** Compatibility constructor for focused unit tests that do not exercise locking or appointment schedules. */
    public ReminderServiceImpl(
            ReminderRepository reminderRepository,
            INotificationService notificationService,
            AuditService auditService,
            BabyProfileRepository babyProfileRepository,
            VaccinationRecordRepository vaccinationRecordRepository) {
        this(reminderRepository, notificationService, auditService, babyProfileRepository,
                vaccinationRecordRepository, null, null, null);
    }

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

        if (saved.getReminderType() == ReminderType.APPOINTMENT
                && appointmentNotificationScheduleService != null) {
            appointmentNotificationScheduleService.createSnapshot(
                    saved, request.getNotificationOffsetsMinutes(), request.getTimeZone());
        } else {
            String fcmJobId = notificationService.scheduleFcmPush(
                    callerId, request.getTitle(), "Reminder: " + request.getTitle(), request.getScheduledAt());
            cancelScheduledJobOnRollback(fcmJobId);
            saved.setFcmJobId(fcmJobId);
            reminderRepository.save(saved);
        }

        auditService.log(AuditAction.REMINDER_CREATED, callerId,
                "Reminder", saved.getId().toString(), "created");
        notifyAppointmentCreated(saved);

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
        cancelScheduledJobOnRollback(fcmJobId);
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
        cancelScheduledJobOnRollback(fcmJobId);
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
        Reminder reminder = findReminderByIdForUpdate(reminderId);
        requireOwnership(reminder, callerId);
        requireActionableState(reminder, "REM-007",
                "Reminder in terminal state " + reminder.getStatus() + " cannot be modified");

        boolean appointmentChanged = reminder.getReminderType() == ReminderType.APPOINTMENT
                && (request.getTitle() != null
                || request.getScheduledAt() != null
                || request.getRecurrenceType() != null
                || request.getRecurrenceEndDate() != null
                || Boolean.TRUE.equals(request.getRecurrenceEndDateSet())
                || request.getTimeZone() != null
                || Boolean.TRUE.equals(request.getNotificationOffsetsMinutesSet()));

        if (request.getTitle() != null) {
            reminder.setTitle(request.getTitle());
        }

        boolean scheduleChanged = false;
        if (request.getScheduledAt() != null) {
            validateScheduledAt(request.getScheduledAt());
            if ((reminder.getReminderType() != ReminderType.APPOINTMENT
                    || appointmentNotificationScheduleService == null)
                    && reminder.getFcmJobId() != null) {
                cancelFcmJobAfterCommit(reminder.getFcmJobId());
            }
            reminder.setScheduledAt(request.getScheduledAt());
            scheduleChanged = true;
            if (reminder.getReminderType() != ReminderType.APPOINTMENT
                    || appointmentNotificationScheduleService == null) {
                String newJobId = notificationService.scheduleFcmPush(
                        callerId, reminder.getTitle(), "Reminder: " + reminder.getTitle(), request.getScheduledAt());
                cancelScheduledJobOnRollback(newJobId);
                reminder.setFcmJobId(newJobId);
            }
        }

        if (request.getRecurrenceType() != null) {
            reminder.setRecurrenceType(request.getRecurrenceType());
            scheduleChanged = true;
        }

        if (request.getRecurrenceEndDate() != null || Boolean.TRUE.equals(request.getRecurrenceEndDateSet())) {
            reminder.setRecurrenceEndDate(request.getRecurrenceEndDate());
            scheduleChanged = true;
        }

        Reminder saved = reminderRepository.save(reminder);
        if (appointmentNotificationScheduleService != null
                && saved.getReminderType() == ReminderType.APPOINTMENT
                && (scheduleChanged
                    || request.getTimeZone() != null
                    || Boolean.TRUE.equals(request.getNotificationOffsetsMinutesSet()))) {
            appointmentNotificationScheduleService.reschedule(
                    saved,
                    request.getNotificationOffsetsMinutes(),
                    Boolean.TRUE.equals(request.getNotificationOffsetsMinutesSet()),
                    request.getTimeZone());
        }
        if (appointmentChanged) {
            notifyAppointmentUpdated(saved);
        }
        return toDetailResponse(saved);
    }

    // ─── UC48: Snooze Reminder ────────────────────────────────────────────────

    @Override
    public ReminderDetailResponse snoozeReminder(UUID reminderId, SnoozeReminderRequest request, UUID callerId) {
        Reminder reminder = findReminderByIdForUpdate(reminderId);
        requireOwnership(reminder, callerId);
        requireActionableState(reminder, "REM-007",
                "Reminder in terminal state " + reminder.getStatus() + " cannot be modified");

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

        cancelTransientSnoozeJob(reminder);

        reminder.setStatus(ReminderStatus.SNOOZED);
        reminder.setSnoozedUntil(snoozedUntil);

        // Snoozing an appointment creates one transient follow-up notification only. The appointment
        // time and its durable milestone jobs remain unchanged so later configured milestones still run.
        String newJobId = notificationService.scheduleFcmPush(
                callerId, reminder.getTitle(), "Snoozed reminder: " + reminder.getTitle(), snoozedUntil);
        cancelScheduledJobOnRollback(newJobId);
        reminder.setFcmJobId(newJobId);

        Reminder saved = reminderRepository.save(reminder);
        return toDetailResponse(saved);
    }

    // ─── UC48: Complete Reminder ──────────────────────────────────────────────

    @Override
    public ReminderDetailResponse completeReminder(UUID reminderId, UUID callerId) {
        return completeReminderInternal(reminderId, callerId, null);
    }

    @Override
    public ReminderDetailResponse completeReminder(
            UUID reminderId, UUID callerId, ReminderActionAuditContext auditContext) {
        return completeReminderInternal(reminderId, callerId, auditContext);
    }

    private ReminderDetailResponse completeReminderInternal(
            UUID reminderId, UUID callerId, ReminderActionAuditContext auditContext) {
        Reminder reminder = findReminderByIdForUpdate(reminderId);
        requireOwnership(reminder, callerId);
        requireActionableState(reminder, "REM-007",
                "Reminder is already in a terminal state and cannot be completed");

        if (reminder.getReminderType() == ReminderType.APPOINTMENT
                && appointmentNotificationScheduleService != null) {
            cancelAppointmentActionJobs(reminder, auditContext);
            cancelTransientSnoozeJob(reminder);
        } else if (reminder.getFcmJobId() != null) {
            cancelFcmJobAfterCommit(reminder.getFcmJobId());
        }

        ReminderStatus previousStatus = reminder.getStatus();
        reminder.setStatus(ReminderStatus.COMPLETED);
        reminder.setSnoozedUntil(null);
        Reminder saved = reminderRepository.save(reminder);

        auditRequiredReminderAction(
                AuditAction.REMINDER_COMPLETED,
                reminder,
                effectiveAuditContext(reminder, callerId, auditContext),
                previousStatus,
                ReminderStatus.COMPLETED);

        return toDetailResponse(saved);
    }

    // ─── UC48: Skip Reminder ──────────────────────────────────────────────────

    @Override
    public ReminderDetailResponse skipReminder(UUID reminderId, UUID callerId) {
        return skipReminderInternal(reminderId, callerId, null);
    }

    @Override
    public ReminderDetailResponse skipReminder(
            UUID reminderId, UUID callerId, ReminderActionAuditContext auditContext) {
        return skipReminderInternal(reminderId, callerId, auditContext);
    }

    private ReminderDetailResponse skipReminderInternal(
            UUID reminderId, UUID callerId, ReminderActionAuditContext auditContext) {
        Reminder reminder = findReminderByIdForUpdate(reminderId);
        requireOwnership(reminder, callerId);
        requireActionableState(reminder, "REM-011",
                "Reminder is not in a skippable state");

        if (reminder.getReminderType() == ReminderType.APPOINTMENT
                && appointmentNotificationScheduleService != null) {
            cancelAppointmentActionJobs(reminder, auditContext);
            cancelTransientSnoozeJob(reminder);
        } else if (reminder.getFcmJobId() != null) {
            cancelFcmJobAfterCommit(reminder.getFcmJobId());
        }

        ReminderStatus previousStatus = reminder.getStatus();
        reminder.setStatus(ReminderStatus.SKIPPED);
        reminder.setSnoozedUntil(null);

        Reminder saved = reminderRepository.save(reminder);

        auditRequiredReminderAction(
                AuditAction.REMINDER_SKIPPED,
                reminder,
                effectiveAuditContext(reminder, callerId, auditContext),
                previousStatus,
                ReminderStatus.SKIPPED);

        return toDetailResponse(saved);
    }

    private void cancelAppointmentActionJobs(
            Reminder reminder, ReminderActionAuditContext auditContext) {
        if (isRecurringReminder(reminder) && auditContext != null && auditContext.occurrenceId() != null) {
            appointmentNotificationScheduleService.cancelOccurrence(
                    reminder.getId(), auditContext.occurrenceId());
            return;
        }
        appointmentNotificationScheduleService.cancelRemaining(reminder.getId());
    }

    private void cancelTransientSnoozeJob(Reminder reminder) {
        if (reminder.getFcmJobId() == null) return;
        cancelFcmJobAfterCommit(reminder.getFcmJobId());
        reminder.setFcmJobId(null);
    }

    private void auditRequiredReminderAction(
            AuditAction action,
            Reminder reminder,
            ReminderActionAuditContext auditContext,
            ReminderStatus previousStatus,
            ReminderStatus status) {
        ChecklistCareContextType contextType = reminder.getBabyId() != null
                ? ChecklistCareContextType.BABY
                : reminder.getJourneyId() != null ? ChecklistCareContextType.JOURNEY : null;
        UUID contextId = reminder.getBabyId() != null ? reminder.getBabyId() : reminder.getJourneyId();
        UUID actorUserId = auditContext.actorUserId() == null
                ? reminder.getOwnerUserId() : auditContext.actorUserId();
        Map<String, Object> beforePayload = new LinkedHashMap<>();
        beforePayload.put("status", previousStatus.name());
        Map<String, Object> afterPayload = new LinkedHashMap<>();
        afterPayload.put("status", status.name());
        if (auditContext.careGroupId() != null) {
            beforePayload.put("careGroupId", auditContext.careGroupId());
            afterPayload.put("careGroupId", auditContext.careGroupId());
        }
        auditService.logRequired(new RequiredAuditEvent(
                action,
                actorUserId,
                "USER",
                null,
                reminder.getOwnerUserId(),
                "ReminderOccurrence",
                auditContext.occurrenceId(),
                contextType,
                contextId,
                null,
                null,
                beforePayload,
                afterPayload,
                auditContext.reasonCode(),
                auditContext.correlationId()));
    }

    private static ReminderActionAuditContext effectiveAuditContext(
            Reminder reminder,
            UUID callerId,
            ReminderActionAuditContext auditContext) {
        if (auditContext != null) {
            return auditContext;
        }
        return new ReminderActionAuditContext(
                ReminderOccurrenceIdFactory.create(
                        reminder.getId(), reminder.getScheduledAt(), reminder.getOccurrenceGeneration()),
                "LEGACY_ACTION",
                UUID.randomUUID(),
                callerId,
                null);
    }

    @Override
    public void deleteReminder(UUID reminderId, UUID callerId) {
        Reminder reminder = findReminderForDeleteForUpdate(reminderId);
        requireDeleteOwnership(reminder, callerId);

        if (reminder.getStatus() == ReminderStatus.CANCELLED) {
            return;
        }

        if (reminder.getReminderType() == ReminderType.APPOINTMENT
                && appointmentNotificationScheduleService != null) {
            appointmentNotificationScheduleService.cancelRemaining(reminder.getId());
            cancelTransientSnoozeJob(reminder);
        } else if (reminder.getFcmJobId() != null) {
            cancelFcmJobAfterCommit(reminder.getFcmJobId());
            reminder.setFcmJobId(null);
        }

        reminder.setStatus(ReminderStatus.CANCELLED);
        reminder.setSnoozedUntil(null);
        reminderRepository.save(reminder);

        auditService.log(AuditAction.REMINDER_CANCELLED, callerId,
                "Reminder", reminderId.toString(), "cancelled");
        notifyAppointmentCancelled(reminder);
    }

    @Override
    public ReminderDetailResponse enableReminder(UUID reminderId, UUID callerId) {
        Reminder reminder = findReminderByIdForUpdate(reminderId);
        requireOwnership(reminder, callerId);

        if (reminder.getStatus() != ReminderStatus.CANCELLED) {
            return toDetailResponse(reminder);
        }

        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setSnoozedUntil(null);
        reminder.setFcmJobId(null);
        reminder.setOccurrenceGeneration(Math.addExact(
                reminder.getOccurrenceGeneration(), 1L));

        if ((reminder.getReminderType() != ReminderType.APPOINTMENT
                || appointmentNotificationScheduleService == null)
                && reminder.getScheduledAt().isAfter(Instant.now())) {
            String newJobId = notificationService.scheduleFcmPush(
                    callerId, reminder.getTitle(), "Reminder: " + reminder.getTitle(), reminder.getScheduledAt());
            cancelScheduledJobOnRollback(newJobId);
            reminder.setFcmJobId(newJobId);
        }

        Reminder saved = reminderRepository.save(reminder);
        if (saved.getReminderType() == ReminderType.APPOINTMENT
                && appointmentNotificationScheduleService != null) {
            appointmentNotificationScheduleService.reschedule(saved, null, false, null);
        }
        auditService.log(AuditAction.REMINDER_CREATED, callerId,
                "Reminder", reminderId.toString(), "enabled");
        notifyAppointmentUpdated(saved);
        return toDetailResponse(saved);
    }

    @Override
    public void hardDeleteReminder(UUID reminderId, UUID callerId) {
        Reminder reminder = findReminderForDeleteForUpdate(reminderId);
        requireDeleteOwnership(reminder, callerId);

        if (reminder.getReminderType() == ReminderType.APPOINTMENT
                && appointmentNotificationScheduleService != null) {
            appointmentNotificationScheduleService.cancelRemaining(reminder.getId());
            cancelTransientSnoozeJob(reminder);
        } else if (reminder.getFcmJobId() != null) {
            cancelFcmJobAfterCommit(reminder.getFcmJobId());
        }

        auditService.log(AuditAction.REMINDER_CANCELLED, callerId,
                "Reminder", reminderId.toString(), "permanently deleted");
        notifyAppointmentCancelled(reminder);
        reminderRepository.delete(reminder);
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

    /** Serialize every definition mutation against the canonical care_tasks row. */
    private Reminder findReminderByIdForUpdate(UUID reminderId) {
        if (entityManager == null) {
            return findReminderById(reminderId);
        }
        reminderRepository.findStatusByIdForUpdate(reminderId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REM-006",
                        "Reminder not found: " + reminderId));
        entityManager.clear();
        return findReminderById(reminderId);
    }

    private Reminder findReminderForDelete(UUID reminderId) {
        return reminderRepository.findById(reminderId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REM-015",
                        "Reminder not found"));
    }

    private Reminder findReminderForDeleteForUpdate(UUID reminderId) {
        if (entityManager == null) {
            return findReminderForDelete(reminderId);
        }
        reminderRepository.findStatusByIdForUpdate(reminderId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REM-015",
                        "Reminder not found"));
        entityManager.clear();
        return findReminderForDelete(reminderId);
    }

    private void cancelFcmJobAfterCommit(String fcmJobId) {
        if (fcmJobId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationService.cancelFcmJob(fcmJobId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.cancelFcmJob(fcmJobId);
            }
        });
    }

    private void cancelScheduledJobOnRollback(String fcmJobId) {
        if (fcmJobId == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    notificationService.cancelFcmJob(fcmJobId);
                }
            }
        });
    }

    private void requireOwnership(Reminder reminder, UUID callerId) {
        if (!reminder.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "REM-004",
                    "Access denied to reminder");
        }
    }

    private void requireDeleteOwnership(Reminder reminder, UUID callerId) {
        if (!reminder.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "REM-016",
                    "Access denied to reminder");
        }
    }

    private void requireBabyOwnership(UUID babyId, UUID callerId) {
        babyProfileRepository.findByIdAndOwnerUserId(babyId, callerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "REM-003",
                        "Baby profile not found or not owned by caller"));
    }

    private void requireActionableState(Reminder reminder, String code, String message) {
        ReminderStatus status = reminder.getStatus();
        if (isRecurringReminder(reminder) && status != ReminderStatus.CANCELLED) {
            return;
        }
        if (status != ReminderStatus.PENDING && status != ReminderStatus.SNOOZED) {
            throw new BusinessException(HttpStatus.CONFLICT, code, message);
        }
    }

    private boolean isRecurringReminder(Reminder reminder) {
        RecurrenceType recurrenceType = reminder.getRecurrenceType();
        return recurrenceType != null && recurrenceType != RecurrenceType.NONE;
    }

    private void notifyAppointmentCreated(Reminder reminder) {
        if (careGroupAppointmentNotificationService == null
                || reminder.getReminderType() != ReminderType.APPOINTMENT) return;
        dispatchAppointmentNotification(() -> careGroupAppointmentNotificationService.notifyCreated(reminder));
    }

    private void notifyAppointmentUpdated(Reminder reminder) {
        if (careGroupAppointmentNotificationService == null
                || reminder.getReminderType() != ReminderType.APPOINTMENT) return;
        dispatchAppointmentNotification(() -> careGroupAppointmentNotificationService.notifyUpdated(reminder));
    }

    private void notifyAppointmentCancelled(Reminder reminder) {
        if (careGroupAppointmentNotificationService == null
                || reminder.getReminderType() != ReminderType.APPOINTMENT) return;
        dispatchAppointmentNotification(() -> careGroupAppointmentNotificationService.notifyCancelled(reminder));
    }

    private void dispatchAppointmentNotification(Runnable notification) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                notification.run();
            } catch (RuntimeException ignored) {
                // Notification fan-out must not roll back the mother's appointment write.
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    notification.run();
                } catch (RuntimeException ignored) {
                    // Notification fan-out must not roll back the mother's appointment write.
                }
            }
        });
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
                .notificationOffsetsMinutes(notificationOffsets(saved))
                .timeZone(notificationTimeZone(saved))
                .build();
    }

    private ReminderDetailResponse toDetailResponse(Reminder reminder) {
        return ReminderDetailResponse.builder()
                .id(reminder.getId())
                .reminderType(reminder.getReminderType() != null ? reminder.getReminderType().name() : null)
                .title(reminder.getTitle())
                .scheduledAt(reminder.getScheduledAt())
                .recurrenceType(reminder.getRecurrenceType() != null ? reminder.getRecurrenceType().name() : null)
                .recurrenceEndDate(reminder.getRecurrenceEndDate())
                .status(reminder.getStatus().name())
                .snoozedUntil(reminder.getSnoozedUntil())
                .createdAt(reminder.getCreatedAt())
                .updatedAt(reminder.getUpdatedAt())
                .notificationOffsetsMinutes(notificationOffsets(reminder))
                .timeZone(notificationTimeZone(reminder))
                .build();
    }

    private List<Integer> notificationOffsets(Reminder reminder) {
        return reminder.getReminderType() == ReminderType.APPOINTMENT
                && appointmentNotificationScheduleService != null
                ? appointmentNotificationScheduleService.currentOffsets(reminder.getId())
                : List.of();
    }

    private String notificationTimeZone(Reminder reminder) {
        return reminder.getReminderType() == ReminderType.APPOINTMENT
                && appointmentNotificationScheduleService != null
                ? appointmentNotificationScheduleService.currentTimeZone(reminder.getId())
                : null;
    }
}
