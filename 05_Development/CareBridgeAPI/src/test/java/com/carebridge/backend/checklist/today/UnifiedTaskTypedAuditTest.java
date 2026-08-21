package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.mapper.AuditLogMapper;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.audit.service.impl.AuditServiceImpl;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.provider.CareTaskActionHandler;
import com.carebridge.backend.checklist.today.provider.ReminderTaskActionHandler;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.ReminderActionAuditContext;
import com.carebridge.backend.reminder.service.impl.ReminderServiceImpl;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UnifiedTaskTypedAuditTest {
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");

    @Test
    void careTaskCompletionPersistsActorRecipientContextReasonAndFacadeCorrelation() {
        UUID taskId = UUID.fromString("00000000-0000-0000-0000-000000000601");
        CareTaskRepository tasks = mock(CareTaskRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy authorization = mock(CareGroupAuthorizationPolicy.class);
        AuditLogRepository audits = mock(AuditLogRepository.class);
        CareTask task = CareTask.builder()
                .id(taskId).careGroupId(GROUP_ID).assignedTo(ACTOR).title("Care task")
                .targetSubject(ChecklistTargetSubject.MOTHER).journeyId(JOURNEY_ID)
                .status(CareTaskStatus.OPEN).build();
        CareGroup group = CareGroup.builder()
                .id(GROUP_ID).ownerUserId(ACTOR).groupName("Care group")
                .linkedJourneyId(JOURNEY_ID).status(CareGroupStatus.ACTIVE).build();
        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
        when(groups.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(tasks.save(task)).thenReturn(task);
        var handler = new CareTaskActionHandler(tasks, groups, authorization, auditService(audits));

        handler.apply(handler.authorize(ACTOR, taskId), ACTOR, TaskAction.COMPLETE, null,
                Instant.parse("2026-08-03T02:00:00Z"), CORRELATION_ID);

        AuditLog saved = captureAudit(audits);
        assertThat(saved.getActorUserId()).isEqualTo(ACTOR);
        assertThat(saved.getActorType()).isEqualTo("USER");
        assertThat(saved.getSubjectUserId()).isEqualTo(ACTOR);
        assertThat(saved.getEntityType()).isEqualTo("CareTask");
        assertThat(saved.getEntityId()).isEqualTo(taskId);
        assertThat(saved.getCareContextType()).isEqualTo(ChecklistCareContextType.JOURNEY);
        assertThat(saved.getCareContextId()).isEqualTo(JOURNEY_ID);
        assertThat(saved.getReasonCode()).isEqualTo("USER_ACTION");
        assertThat(saved.getCorrelationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void reminderCompletionPersistsOccurrenceRecipientContextReasonAndFacadeCorrelation() {
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000602");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        UUID occurrenceId = com.carebridge.backend.checklist.today.provider.ReminderOccurrenceIdFactory
                .create(definitionId, scheduledAt);
        ReminderRepository reminders = mock(ReminderRepository.class);
        AuditLogRepository audits = mock(AuditLogRepository.class);
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(ACTOR).journeyId(JOURNEY_ID)
                .scheduledAt(scheduledAt).status(ReminderStatus.PENDING).title("Reminder").build();
        when(reminders.findById(occurrenceId)).thenReturn(Optional.empty());
        when(reminders.findByOwnerUserIdOrderByScheduledAtDesc(ACTOR)).thenReturn(List.of(reminder));
        when(reminders.findById(definitionId)).thenReturn(Optional.of(reminder));
        when(reminders.save(reminder)).thenReturn(reminder);
        ReminderServiceImpl service = new ReminderServiceImpl(
                reminders,
                mock(INotificationService.class),
                auditService(audits),
                mock(BabyProfileRepository.class),
                mock(VaccinationRecordRepository.class));
        var handler = new ReminderTaskActionHandler(reminders, service);

        handler.apply(handler.authorize(ACTOR, occurrenceId), ACTOR, TaskAction.COMPLETE, null,
                Instant.parse("2026-08-03T02:00:00Z"), CORRELATION_ID);

        AuditLog saved = captureAudit(audits);
        assertThat(saved.getActorUserId()).isEqualTo(ACTOR);
        assertThat(saved.getActorType()).isEqualTo("USER");
        assertThat(saved.getSubjectUserId()).isEqualTo(ACTOR);
        assertThat(saved.getEntityType()).isEqualTo("ReminderOccurrence");
        assertThat(saved.getEntityId()).isEqualTo(occurrenceId);
        assertThat(saved.getCareContextType()).isEqualTo(ChecklistCareContextType.JOURNEY);
        assertThat(saved.getCareContextId()).isEqualTo(JOURNEY_ID);
        assertThat(saved.getReasonCode()).isEqualTo("USER_ACTION");
        assertThat(saved.getCorrelationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void sharedReminderAuditUsesFamilyActorAndMotherOwnerAsSubject() {
        UUID family = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000603");
        UUID occurrenceId = UUID.fromString("00000000-0000-0000-0000-000000000703");
        ReminderRepository reminders = mock(ReminderRepository.class);
        AuditLogRepository audits = mock(AuditLogRepository.class);
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(ACTOR).journeyId(JOURNEY_ID)
                .scheduledAt(Instant.parse("2026-08-03T01:00:00Z"))
                .status(ReminderStatus.PENDING).title("Shared reminder").build();
        when(reminders.findById(definitionId)).thenReturn(Optional.of(reminder));
        when(reminders.save(reminder)).thenReturn(reminder);
        ReminderServiceImpl service = new ReminderServiceImpl(
                reminders,
                mock(INotificationService.class),
                auditService(audits),
                mock(BabyProfileRepository.class),
                mock(VaccinationRecordRepository.class));

        service.completeReminder(definitionId, ACTOR, new ReminderActionAuditContext(
                occurrenceId, "USER_ACTION", CORRELATION_ID, family, GROUP_ID));

        AuditLog saved = captureAudit(audits);
        assertThat(saved.getActorUserId()).isEqualTo(family);
        assertThat(saved.getSubjectUserId()).isEqualTo(ACTOR);
        assertThat(saved.getEntityId()).isEqualTo(occurrenceId);
        assertThat(saved.getNewValueJson()).contains(GROUP_ID.toString());
    }

    private static AuditServiceImpl auditService(AuditLogRepository repository) {
        return new AuditServiceImpl(repository, mock(AuditLogMapper.class),
                new AuditEligibilityPolicy(), new ObjectMapper().findAndRegisterModules(), mock(UserRepository.class));
    }

    private static AuditLog captureAudit(AuditLogRepository repository) {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
