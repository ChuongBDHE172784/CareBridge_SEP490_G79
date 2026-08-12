package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.entity.ChecklistActionCommand;
import com.carebridge.backend.checklist.repository.ChecklistActionCommandRepository;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.provider.AuthorizedTask;
import com.carebridge.backend.checklist.today.provider.TaskActionHandler;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UnifiedTaskActionFacadeTest {

    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID TASK = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID INSTANCE = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final Instant NOW = Instant.parse("2026-08-03T12:00:00Z");

    private TaskActionHandler handler;
    private ChecklistActionCommandRepository repository;
    private ObjectMapper objectMapper;
    private UnifiedTaskActionFacade facade;

    @BeforeEach
    void setUp() {
        handler = mock(TaskActionHandler.class);
        repository = mock(ChecklistActionCommandRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        when(handler.taskKind()).thenReturn(TaskKind.CHECKLIST);
        facade = new UnifiedTaskActionFacade(java.util.List.of(handler), repository, objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void chk028_authorizesBeforeLookingUpReplayAndHidesExistence() {
        when(handler.authorize(ACTOR, TASK)).thenThrow(taskNotFound());

        assertThatThrownBy(() -> facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));

        verify(repository, never()).findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(any(), any(), any(), any());
    }

    @Test
    void chk028_samePayloadReplaysOriginalAndChangedPayloadIsRejected() throws Exception {
        AuthorizedTask authorized = new AuthorizedTask(TaskKind.CHECKLIST, TASK, INSTANCE,
                "PENDING", Set.of(TaskAction.COMPLETE, TaskAction.SKIP));
        when(handler.authorize(ACTOR, TASK)).thenReturn(authorized);
        when(handler.authorizeForUpdate(ACTOR, authorized)).thenReturn(authorized);
        when(handler.apply(any(), any(), any(), any(), any(), any())).thenAnswer(invocation ->
                new TaskActionResponse(TaskKind.CHECKLIST, TASK, INSTANCE, invocation.getArgument(2),
                        "PENDING", invocation.getArgument(2) == TaskAction.COMPLETE ? "COMPLETED" : "SKIPPED",
                        NOW, false, invocation.getArgument(5)));
        when(repository.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                ACTOR, "CHECKLIST", TASK, REQUEST_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskActionResponse first = facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null));
        ArgumentCaptor<ChecklistActionCommand> commandCaptor = ArgumentCaptor.forClass(ChecklistActionCommand.class);
        verify(repository).save(commandCaptor.capture());
        ChecklistActionCommand stored = commandCaptor.getValue();
        when(repository.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                ACTOR, "CHECKLIST", TASK, REQUEST_ID)).thenReturn(Optional.of(stored));

        TaskActionResponse replay = facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null));

        assertThat(first.idempotentReplay()).isFalse();
        assertThat(replay.idempotentReplay()).isTrue();
        assertThat(replay.correlationId()).isEqualTo(first.correlationId());
        verify(handler, org.mockito.Mockito.times(1)).apply(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.SKIP, REQUEST_ID, "USER_CHOICE")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("IDEMPOTENCY_KEY_REUSE"));
    }

    @Test
    void chk028_replayRejectsMembershipEpochChangedAfterInitialAuthorization() throws Exception {
        AuthorizedTask firstEpoch = new AuthorizedTask(
                TaskKind.CHECKLIST, TASK, INSTANCE, "PENDING", Set.of(TaskAction.COMPLETE),
                UUID.randomUUID(), 7L);
        AuthorizedTask secondEpoch = new AuthorizedTask(
                TaskKind.CHECKLIST, TASK, INSTANCE, "PENDING", Set.of(TaskAction.COMPLETE),
                firstEpoch.authorizationCareGroupId(), 8L);
        AuthorizedTask revalidatedEpoch = new AuthorizedTask(
                TaskKind.CHECKLIST, TASK, INSTANCE, "PENDING", Set.of(TaskAction.COMPLETE),
                firstEpoch.authorizationCareGroupId(), 9L);
        AtomicInteger postApplyAuthorizationCalls = new AtomicInteger();
        AtomicReference<Boolean> firstApplyComplete = new AtomicReference<>(false);
        when(handler.authorize(ACTOR, TASK)).thenAnswer(invocation ->
                firstApplyComplete.get()
                        ? (postApplyAuthorizationCalls.incrementAndGet() == 1
                            ? secondEpoch : revalidatedEpoch)
                        : firstEpoch);
        when(handler.authorizeForUpdate(ACTOR, firstEpoch)).thenReturn(firstEpoch);
        when(handler.apply(any(), any(), any(), any(), any(), any())).thenReturn(
                new TaskActionResponse(TaskKind.CHECKLIST, TASK, INSTANCE, TaskAction.COMPLETE,
                        "PENDING", "COMPLETED", NOW, false, UUID.randomUUID()));
        AtomicReference<ChecklistActionCommand> storedCommand = new AtomicReference<>();
        when(repository.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                ACTOR, "CHECKLIST", TASK, REQUEST_ID)).thenAnswer(invocation ->
                        Optional.ofNullable(storedCommand.get()));
        when(repository.save(any())).thenAnswer(invocation -> {
            ChecklistActionCommand command = invocation.getArgument(0);
            storedCommand.set(command);
            return command;
        });

        facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null));
        assertThat(storedCommand.get()).isNotNull();
        firstApplyComplete.set(true);
        assertThatThrownBy(() -> facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
    }

    @Test
    void chk028_rejectsActionNotAdvertisedByTheProvider() {
        AuthorizedTask authorized = new AuthorizedTask(
                TaskKind.CHECKLIST, TASK, INSTANCE, "PENDING", Set.of(TaskAction.COMPLETE));
        when(handler.authorize(ACTOR, TASK)).thenReturn(authorized);
        when(handler.authorizeForUpdate(ACTOR, authorized)).thenReturn(authorized);
        when(repository.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                ACTOR, "CHECKLIST", TASK, REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.SKIP, REQUEST_ID, "USER_CHOICE")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("ACTION_NOT_ALLOWED"));
        verify(handler, never()).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    void chk028_skipRequiresControlledReason() {
        when(handler.authorize(ACTOR, TASK)).thenReturn(new AuthorizedTask(
                TaskKind.CHECKLIST, TASK, INSTANCE, "PENDING", Set.of(TaskAction.SKIP)));
        when(repository.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                ACTOR, "CHECKLIST", TASK, REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.SKIP, REQUEST_ID, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_ACTION_REASON_REQUIRED"));
    }

    @Test
    void chk028_skipRejectsReasonOutsideControlledAllowlist() {
        when(handler.authorize(ACTOR, TASK)).thenReturn(new AuthorizedTask(
                TaskKind.CHECKLIST, TASK, INSTANCE, "PENDING", Set.of(TaskAction.SKIP)));

        assertThatThrownBy(() -> facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.SKIP, REQUEST_ID, "FREE_TEXT")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_ACTION_REASON_INVALID"));

        verify(repository, never()).acquireTaskActionLock(any());
        verify(handler, never()).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    void chk028_completeRejectsNonNullReason() {
        when(handler.authorize(ACTOR, TASK)).thenReturn(new AuthorizedTask(
                TaskKind.CHECKLIST, TASK, INSTANCE, "PENDING", Set.of(TaskAction.COMPLETE)));

        assertThatThrownBy(() -> facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, "FREE TEXT")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_ACTION_REASON_INVALID"));
        verify(handler, never()).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    void chk028_reopenRejectsNonNullReason() {
        when(handler.authorize(ACTOR, TASK)).thenReturn(new AuthorizedTask(
                TaskKind.CHECKLIST, TASK, INSTANCE, "COMPLETED", Set.of(TaskAction.REOPEN)));

        assertThatThrownBy(() -> facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.REOPEN, REQUEST_ID, "USER_CHOICE")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_ACTION_REASON_INVALID"));
        verify(repository, never()).acquireTaskActionLock(any());
        verify(handler, never()).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    void chk028_terminalStateStillBlocksMutationAfterCommandWasPurged() {
        AuthorizedTask authorized = new AuthorizedTask(
                TaskKind.CHECKLIST, TASK, INSTANCE, "COMPLETED", Set.of());
        when(handler.authorize(ACTOR, TASK)).thenReturn(authorized);
        when(handler.authorizeForUpdate(ACTOR, authorized)).thenReturn(authorized);
        when(repository.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                ACTOR, "CHECKLIST", TASK, REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.apply(ACTOR, TaskKind.CHECKLIST, TASK,
                new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_ALREADY_TERMINAL"));

        verify(handler, never()).apply(any(), any(), any(), any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void chk028_reminderReplaySurvivesRescheduleOrCancellationAfterOriginalApply() {
        TaskActionHandler reminderHandler = mock(TaskActionHandler.class);
        when(reminderHandler.taskKind()).thenReturn(TaskKind.REMINDER);
        UnifiedTaskActionFacade reminderFacade = new UnifiedTaskActionFacade(
                java.util.List.of(reminderHandler), repository, objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
        AuthorizedTask pendingOccurrence = new AuthorizedTask(
                TaskKind.REMINDER, TASK, INSTANCE, "PENDING", Set.of(TaskAction.COMPLETE));
        when(reminderHandler.authorize(ACTOR, TASK)).thenReturn(pendingOccurrence);
        when(reminderHandler.authorizeForUpdate(ACTOR, pendingOccurrence)).thenReturn(pendingOccurrence);
        when(reminderHandler.apply(any(), any(), any(), any(), any(), any())).thenAnswer(invocation ->
                new TaskActionResponse(TaskKind.REMINDER, TASK, INSTANCE, TaskAction.COMPLETE,
                        "PENDING", "COMPLETED", NOW, false, invocation.getArgument(5)));
        when(repository.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                ACTOR, "REMINDER", TASK, REQUEST_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TaskActionRequest request = new TaskActionRequest(TaskAction.COMPLETE, REQUEST_ID, null);

        TaskActionResponse original = reminderFacade.apply(
                ACTOR, TaskKind.REMINDER, TASK, request);
        ArgumentCaptor<ChecklistActionCommand> commandCaptor =
                ArgumentCaptor.forClass(ChecklistActionCommand.class);
        verify(repository).save(commandCaptor.capture());
        ChecklistActionCommand command = commandCaptor.getValue();
        when(repository.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                ACTOR, "REMINDER", TASK, REQUEST_ID)).thenReturn(Optional.of(command));
        when(reminderHandler.authorize(ACTOR, TASK)).thenThrow(taskNotFound());
        when(reminderHandler.authorizeReplay(ACTOR, TASK, INSTANCE)).thenReturn(new AuthorizedTask(
                TaskKind.REMINDER, TASK, INSTANCE, "CANCELLED", Set.of()));

        TaskActionResponse replay = reminderFacade.apply(
                ACTOR, TaskKind.REMINDER, TASK, request);

        assertThat(replay.idempotentReplay()).isTrue();
        assertThat(replay.correlationId()).isEqualTo(original.correlationId());
        verify(reminderHandler).authorizeReplay(ACTOR, TASK, INSTANCE);
        verify(reminderHandler, org.mockito.Mockito.times(1))
                .apply(any(), any(), any(), any(), any(), any());
    }

    private static BusinessException taskNotFound() {
        return new BusinessException(org.springframework.http.HttpStatus.NOT_FOUND,
                "TASK_NOT_FOUND", "Task not found");
    }
}
