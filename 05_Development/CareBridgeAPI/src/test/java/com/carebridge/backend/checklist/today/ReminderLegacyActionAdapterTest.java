package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.entity.ChecklistActionCommand;
import com.carebridge.backend.checklist.repository.ChecklistActionCommandRepository;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.provider.ReminderOccurrenceIdFactory;
import com.carebridge.backend.checklist.today.provider.ReminderTaskActionHandler;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.reminder.controller.ReminderController;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.IReminderService;
import com.carebridge.backend.reminder.service.ITodayTaskService;
import com.carebridge.backend.reminder.service.ReminderActionAuditContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ReminderLegacyActionAdapterTest {

    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID TASK = UUID.fromString("00000000-0000-0000-0000-000000000401");

    @Mock IReminderService reminderService;
    @Mock ITodayTaskService legacyTodayService;
    @Mock UnifiedTaskActionFacade actionFacade;
    @Mock Principal principal;
    @InjectMocks ReminderController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(principal.getName()).thenReturn(ACTOR.toString());
        when(reminderService.getReminderDetail(TASK, ACTOR)).thenReturn(
                ReminderDetailResponse.builder().id(TASK).status("COMPLETED").build());
        when(reminderService.completeReminder(TASK, ACTOR)).thenReturn(
                ReminderDetailResponse.builder().id(TASK).status("COMPLETED").build());
        when(actionFacade.apply(eq(ACTOR), eq(TaskKind.REMINDER), eq(TASK), any())).thenReturn(
                new TaskActionResponse(TaskKind.REMINDER, TASK, null, TaskAction.COMPLETE,
                        "PENDING", "COMPLETED", Instant.parse("2026-08-03T12:00:00Z"), false,
                        UUID.fromString("00000000-0000-0000-0000-000000000701")));
    }

    @Test
    void chk025_legacyCompleteKeepsResponseSchemaButUsesUnifiedFacade() {
        var response = controller.completeReminder(TASK, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isInstanceOf(ReminderDetailResponse.class);
        verify(actionFacade).apply(eq(ACTOR), eq(TaskKind.REMINDER), eq(TASK), any());
        verify(reminderService, never()).completeReminder(TASK, ACTOR);
    }

    @Test
    void legacyDefinitionActionNormalizesBeforeIdempotencyAndKeepsLegacyResponseSchema() {
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000402");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        UUID occurrenceId = ReminderOccurrenceIdFactory.create(definitionId, scheduledAt);
        ReminderRepository reminders = org.mockito.Mockito.mock(ReminderRepository.class);
        ChecklistActionCommandRepository commands = org.mockito.Mockito.mock(ChecklistActionCommandRepository.class);
        IReminderService service = org.mockito.Mockito.mock(IReminderService.class);
        ITodayTaskService legacyToday = org.mockito.Mockito.mock(ITodayTaskService.class);
        Reminder reminder = Reminder.builder()
                .id(definitionId)
                .ownerUserId(ACTOR)
                .title("Appointment")
                .scheduledAt(scheduledAt)
                .status(ReminderStatus.PENDING)
                .build();
        when(reminders.findById(definitionId)).thenReturn(Optional.of(reminder));
        when(commands.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                eq(ACTOR), eq(TaskKind.REMINDER.name()), eq(occurrenceId), any()))
                .thenReturn(Optional.empty());
        when(service.getReminderDetail(definitionId, ACTOR)).thenReturn(
                ReminderDetailResponse.builder().id(definitionId).status("COMPLETED").build());
        var facade = new UnifiedTaskActionFacade(
                List.of(new ReminderTaskActionHandler(reminders, service)),
                commands,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-08-03T02:00:00Z"), ZoneOffset.UTC));
        var controller = new ReminderController(service, legacyToday, facade);

        var response = controller.completeReminder(definitionId, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isInstanceOf(ReminderDetailResponse.class);
        ArgumentCaptor<ChecklistActionCommand> command = ArgumentCaptor.forClass(ChecklistActionCommand.class);
        verify(commands).save(command.capture());
        assertThat(command.getValue().getTaskId()).isEqualTo(occurrenceId);
        assertThat(command.getValue().getReminderDefinitionId()).isEqualTo(definitionId);
        verify(commands).findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                eq(ACTOR), eq(TaskKind.REMINDER.name()), eq(occurrenceId), any());
        verify(service).completeReminder(
                eq(definitionId), eq(ACTOR), any(ReminderActionAuditContext.class));
    }
}
