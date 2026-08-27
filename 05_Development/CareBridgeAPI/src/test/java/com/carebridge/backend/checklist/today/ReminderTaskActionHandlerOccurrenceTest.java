package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.policy.ReminderAccessPolicy;
import com.carebridge.backend.checklist.entity.ReminderOccurrenceAlias;
import com.carebridge.backend.checklist.repository.ReminderOccurrenceAliasRepository;
import com.carebridge.backend.checklist.today.provider.ReminderTaskActionHandler;
import com.carebridge.backend.checklist.today.provider.ReminderOccurrenceIdFactory;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.IReminderService;
import com.carebridge.backend.reminder.service.ReminderActionAuditContext;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReminderTaskActionHandlerOccurrenceTest {
    @Test
    void staleGenerationAliasIsDeniedAfterReenableWhileCurrentGenerationRemainsActionable() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000410");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        UUID oldOccurrence = ReminderOccurrenceIdFactory.create(definitionId, scheduledAt, 0L);
        UUID currentOccurrence = ReminderOccurrenceIdFactory.create(definitionId, scheduledAt, 1L);
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner).scheduledAt(scheduledAt)
                .occurrenceGeneration(1L).status(ReminderStatus.PENDING)
                .title("Re-enabled appointment").build();
        ReminderRepository repository = mock(ReminderRepository.class);
        ReminderOccurrenceAliasRepository aliases = mock(ReminderOccurrenceAliasRepository.class);
        when(repository.findById(oldOccurrence)).thenReturn(Optional.empty());
        when(repository.findById(currentOccurrence)).thenReturn(Optional.empty());
        when(repository.findById(definitionId)).thenReturn(Optional.of(reminder));
        when(repository.findByOwnerUserIdOrderByScheduledAtDesc(owner)).thenReturn(List.of(reminder));
        when(aliases.findByOccurrenceId(oldOccurrence)).thenReturn(Optional.of(
                ReminderOccurrenceAlias.builder()
                        .occurrenceId(oldOccurrence).reminderDefinitionId(definitionId)
                        .ownerUserId(owner).scheduledAt(scheduledAt)
                        .occurrenceGeneration(0L).build()));
        when(aliases.findByOccurrenceId(currentOccurrence)).thenReturn(Optional.of(
                ReminderOccurrenceAlias.builder()
                        .occurrenceId(currentOccurrence).reminderDefinitionId(definitionId)
                        .ownerUserId(owner).scheduledAt(scheduledAt)
                        .occurrenceGeneration(1L).build()));
        var handler = new ReminderTaskActionHandler(
                repository, aliases, mock(IReminderService.class));

        assertThatThrownBy(() -> handler.authorize(owner, oldOccurrence))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
        assertThat(handler.authorize(owner, currentOccurrence).allowedActions())
                .containsExactlyInAnyOrder(TaskAction.COMPLETE, TaskAction.SKIP);
    }

    @Test
    void aliasOwnerMustMatchReminderDefinitionOwner() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID foreignOwner = UUID.fromString("00000000-0000-0000-0000-000000000199");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000411");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        UUID occurrenceId = ReminderOccurrenceIdFactory.create(definitionId, scheduledAt);
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner).scheduledAt(scheduledAt)
                .occurrenceGeneration(0L).status(ReminderStatus.PENDING)
                .title("Owner integrity").build();
        ReminderRepository repository = mock(ReminderRepository.class);
        ReminderOccurrenceAliasRepository aliases = mock(ReminderOccurrenceAliasRepository.class);
        when(repository.findById(occurrenceId)).thenReturn(Optional.empty());
        when(repository.findById(definitionId)).thenReturn(Optional.of(reminder));
        when(repository.findByOwnerUserIdOrderByScheduledAtDesc(owner)).thenReturn(List.of());
        when(aliases.findByOccurrenceId(occurrenceId)).thenReturn(Optional.of(
                ReminderOccurrenceAlias.builder()
                        .occurrenceId(occurrenceId).reminderDefinitionId(definitionId)
                        .ownerUserId(foreignOwner).scheduledAt(scheduledAt)
                        .occurrenceGeneration(0L).build()));

        assertThatThrownBy(() -> new ReminderTaskActionHandler(
                repository, aliases, mock(IReminderService.class))
                .authorize(owner, occurrenceId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
    }


    @Test
    void permittedFamilyCannotAddressReminderByDefinitionUuidButDurableAliasRemainsPositive() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID family = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID journey = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000301");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000408");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        UUID occurrenceId = ReminderOccurrenceIdFactory.create(definitionId, scheduledAt);
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner).journeyId(journey).scheduledAt(scheduledAt)
                .status(ReminderStatus.PENDING).title("Shared appointment").build();
        CareGroup group = CareGroup.builder()
                .id(groupId).ownerUserId(owner).linkedJourneyId(journey)
                .status(CareGroupStatus.ACTIVE).build();
        ReminderRepository repository = mock(ReminderRepository.class);
        ReminderOccurrenceAliasRepository aliases = mock(ReminderOccurrenceAliasRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy authorization = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        ReminderAccessPolicy access = new ReminderAccessPolicy(
                groups, authorization, journeys, mock(BabyProfileRepository.class));
        when(repository.findById(definitionId)).thenReturn(Optional.of(reminder));
        when(repository.findById(occurrenceId)).thenReturn(Optional.empty());
        when(aliases.findByOccurrenceId(occurrenceId)).thenReturn(Optional.of(
                ReminderOccurrenceAlias.builder()
                        .occurrenceId(occurrenceId).reminderDefinitionId(definitionId)
                        .ownerUserId(owner).scheduledAt(scheduledAt).build()));
        when(groups.findByLinkedJourneyId(journey)).thenReturn(List.of(group));
        when(journeys.existsByIdAndOwnerUserId(journey, owner)).thenReturn(true);
        when(authorization.hasPermission(groupId, family, PermissionFlag.CHECKLIST_VIEW))
                .thenReturn(true);
        when(authorization.hasPermission(groupId, family, PermissionFlag.CHECKLIST_COMPLETE))
                .thenReturn(true);
        var handler = new ReminderTaskActionHandler(
                repository, aliases, mock(IReminderService.class), access);

        assertThatThrownBy(() -> handler.authorize(family, definitionId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
        assertThat(handler.authorize(family, occurrenceId).instanceId()).isEqualTo(definitionId);
    }

    @Test
    void familyAliasActionIsDeniedWhenJourneyBelongsToAnotherOwner() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID family = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID journey = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000301");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000409");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        UUID occurrenceId = ReminderOccurrenceIdFactory.create(definitionId, scheduledAt);
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner).journeyId(journey).scheduledAt(scheduledAt)
                .status(ReminderStatus.PENDING).title("Foreign journey").build();
        CareGroup group = CareGroup.builder()
                .id(groupId).ownerUserId(owner).linkedJourneyId(journey)
                .status(CareGroupStatus.ACTIVE).build();
        ReminderRepository repository = mock(ReminderRepository.class);
        ReminderOccurrenceAliasRepository aliases = mock(ReminderOccurrenceAliasRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy authorization = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        ReminderAccessPolicy access = new ReminderAccessPolicy(
                groups, authorization, journeys, mock(BabyProfileRepository.class));
        when(repository.findById(occurrenceId)).thenReturn(Optional.empty());
        when(repository.findById(definitionId)).thenReturn(Optional.of(reminder));
        when(aliases.findByOccurrenceId(occurrenceId)).thenReturn(Optional.of(
                ReminderOccurrenceAlias.builder()
                        .occurrenceId(occurrenceId).reminderDefinitionId(definitionId)
                        .ownerUserId(owner).scheduledAt(scheduledAt).build()));
        when(groups.findByLinkedJourneyId(journey)).thenReturn(List.of(group));
        when(journeys.existsByIdAndOwnerUserId(journey, owner)).thenReturn(false);
        when(authorization.hasPermission(groupId, family, PermissionFlag.CHECKLIST_VIEW))
                .thenReturn(true);
        when(authorization.hasPermission(groupId, family, PermissionFlag.CHECKLIST_COMPLETE))
                .thenReturn(true);

        assertThatThrownBy(() -> new ReminderTaskActionHandler(
                repository, aliases, mock(IReminderService.class), access)
                .authorize(family, occurrenceId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
    }

    @Test
    void stableOccurrenceIdResolvesToDefinitionForAuthorizedAction() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000401");
        Instant dueAt = Instant.parse("2026-08-03T01:00:00Z");
        UUID occurrenceId = UUID.nameUUIDFromBytes(
                ("reminder-occurrence-v1|" + definitionId + "|" + dueAt)
                        .getBytes(StandardCharsets.UTF_8));
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner).scheduledAt(dueAt)
                .status(ReminderStatus.PENDING).title("Appointment").build();
        ReminderRepository repository = mock(ReminderRepository.class);
        IReminderService service = mock(IReminderService.class);
        when(repository.findById(occurrenceId)).thenReturn(Optional.empty());
        when(repository.findById(definitionId)).thenReturn(Optional.of(reminder));
        when(repository.findByOwnerUserIdOrderByScheduledAtDesc(owner))
                .thenReturn(List.of(reminder));
        var handler = new ReminderTaskActionHandler(repository, service);

        var authorized = handler.authorize(owner, occurrenceId);
        var response = handler.apply(authorized, owner, TaskAction.COMPLETE, null,
                Instant.parse("2026-08-03T02:00:00Z"), UUID.randomUUID());

        assertThat(authorized.taskId()).isEqualTo(occurrenceId);
        assertThat(authorized.instanceId()).isEqualTo(definitionId);
        assertThat(response.taskId()).isEqualTo(occurrenceId);
        verify(service).completeReminder(
                org.mockito.ArgumentMatchers.eq(definitionId),
                org.mockito.ArgumentMatchers.eq(owner),
                org.mockito.ArgumentMatchers.any(ReminderActionAuditContext.class));
    }

    @Test
    void permittedFamilyResolvesOwnerAliasAndServiceAuditsActualFamilyActor() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID family = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID journey = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000301");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000407");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        UUID occurrenceId = UUID.nameUUIDFromBytes(
                ("reminder-occurrence-v1|" + definitionId + "|" + scheduledAt)
                        .getBytes(StandardCharsets.UTF_8));
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner).journeyId(journey).scheduledAt(scheduledAt)
                .status(ReminderStatus.PENDING).title("Shared appointment").build();
        CareGroup group = CareGroup.builder()
                .id(groupId).ownerUserId(owner).linkedJourneyId(journey)
                .status(CareGroupStatus.ACTIVE).build();
        ReminderRepository repository = mock(ReminderRepository.class);
        ReminderOccurrenceAliasRepository aliases = mock(ReminderOccurrenceAliasRepository.class);
        IReminderService service = mock(IReminderService.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy authorization = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        ReminderAccessPolicy access = new ReminderAccessPolicy(
                groups, authorization, journeys, mock(BabyProfileRepository.class));
        when(repository.findById(occurrenceId)).thenReturn(Optional.empty());
        when(aliases.findByOccurrenceId(occurrenceId)).thenReturn(Optional.of(
                ReminderOccurrenceAlias.builder()
                        .occurrenceId(occurrenceId).reminderDefinitionId(definitionId)
                        .ownerUserId(owner).scheduledAt(scheduledAt).build()));
        when(repository.findById(definitionId)).thenReturn(Optional.of(reminder));
        when(groups.findByLinkedJourneyId(journey)).thenReturn(List.of(group));
        when(journeys.existsByIdAndOwnerUserId(journey, owner)).thenReturn(true);
        when(authorization.hasPermission(groupId, family, PermissionFlag.CHECKLIST_VIEW))
                .thenReturn(true);
        when(authorization.hasPermission(groupId, family, PermissionFlag.CHECKLIST_COMPLETE))
                .thenReturn(true);
        var handler = new ReminderTaskActionHandler(repository, aliases, service, access);

        var authorized = handler.authorize(family, occurrenceId);
        handler.apply(authorized, family, TaskAction.COMPLETE, null,
                Instant.parse("2026-08-03T02:00:00Z"), UUID.randomUUID());

        ArgumentCaptor<ReminderActionAuditContext> audit =
                ArgumentCaptor.forClass(ReminderActionAuditContext.class);
        verify(service).completeReminder(
                org.mockito.ArgumentMatchers.eq(definitionId),
                org.mockito.ArgumentMatchers.eq(owner), audit.capture());
        assertThat(audit.getValue().actorUserId()).isEqualTo(family);
        assertThat(audit.getValue().careGroupId()).isEqualTo(groupId);
    }

    @Test
    void snoozedOccurrenceStillResolvesThroughItsOriginalScheduledAnchor() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000402");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        Instant snoozedUntil = Instant.parse("2026-08-03T03:00:00Z");
        UUID occurrenceId = UUID.nameUUIDFromBytes(
                ("reminder-occurrence-v1|" + definitionId + "|" + scheduledAt)
                        .getBytes(StandardCharsets.UTF_8));
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner).scheduledAt(scheduledAt)
                .snoozedUntil(snoozedUntil).status(ReminderStatus.SNOOZED)
                .title("Appointment").build();
        ReminderRepository repository = mock(ReminderRepository.class);
        IReminderService service = mock(IReminderService.class);
        when(repository.findById(occurrenceId)).thenReturn(Optional.empty());
        when(repository.findByOwnerUserIdOrderByScheduledAtDesc(owner))
                .thenReturn(List.of(reminder));

        var authorized = new ReminderTaskActionHandler(repository, service)
                .authorize(owner, occurrenceId);

        assertThat(authorized.taskId()).isEqualTo(occurrenceId);
        assertThat(authorized.instanceId()).isEqualTo(definitionId);
    }

    @Test
    void replayAuthorizationUsesDefinitionAfterOccurrenceWasRescheduledOrCancelled() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000403");
        UUID oldOccurrenceId = UUID.fromString("00000000-0000-0000-0000-000000000903");
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner)
                .scheduledAt(Instant.parse("2026-08-04T01:00:00Z"))
                .status(ReminderStatus.CANCELLED).title("Rescheduled appointment").build();
        ReminderRepository repository = mock(ReminderRepository.class);
        when(repository.findById(definitionId)).thenReturn(Optional.of(reminder));

        var authorized = new ReminderTaskActionHandler(repository, mock(IReminderService.class))
                .authorizeReplay(owner, oldOccurrenceId, definitionId);

        assertThat(authorized.taskId()).isEqualTo(oldOccurrenceId);
        assertThat(authorized.instanceId()).isEqualTo(definitionId);
        assertThat(authorized.status()).isEqualTo("CANCELLED");
        assertThat(authorized.allowedActions()).isEmpty();
    }

    @Test
    void cancelledOccurrenceRemainsAuthorizableForTerminalCasAfterCommandPurge() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000405");
        Instant scheduledAt = Instant.parse("2026-08-03T01:00:00Z");
        UUID occurrenceId = UUID.nameUUIDFromBytes(
                ("reminder-occurrence-v1|" + definitionId + "|" + scheduledAt)
                        .getBytes(StandardCharsets.UTF_8));
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner).scheduledAt(scheduledAt)
                .status(ReminderStatus.CANCELLED).title("Cancelled appointment").build();
        ReminderRepository repository = mock(ReminderRepository.class);
        when(repository.findById(occurrenceId)).thenReturn(Optional.empty());
        when(repository.findByOwnerUserIdOrderByScheduledAtDesc(owner)).thenReturn(List.of(reminder));

        var authorized = new ReminderTaskActionHandler(repository, mock(IReminderService.class))
                .authorize(owner, occurrenceId);

        assertThat(authorized.status()).isEqualTo("CANCELLED");
        assertThat(authorized.allowedActions()).isEmpty();
    }

    @Test
    void historicalOccurrenceResolvesAfterReminderWasRescheduled() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000406");
        Instant oldSchedule = Instant.parse("2026-08-03T01:00:00Z");
        UUID oldOccurrenceId = UUID.nameUUIDFromBytes(
                ("reminder-occurrence-v1|" + definitionId + "|" + oldSchedule)
                        .getBytes(StandardCharsets.UTF_8));
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner)
                .scheduledAt(Instant.parse("2026-08-05T01:00:00Z"))
                .status(ReminderStatus.PENDING).title("Rescheduled appointment").build();
        ReminderRepository repository = mock(ReminderRepository.class);
        ReminderOccurrenceAliasRepository aliases = mock(ReminderOccurrenceAliasRepository.class);
        when(repository.findById(oldOccurrenceId)).thenReturn(Optional.empty());
        when(aliases.findByOccurrenceId(oldOccurrenceId))
                .thenReturn(Optional.of(ReminderOccurrenceAlias.builder()
                        .occurrenceId(oldOccurrenceId)
                        .reminderDefinitionId(definitionId)
                        .ownerUserId(owner)
                        .scheduledAt(oldSchedule)
                        .build()));
        when(repository.findById(definitionId)).thenReturn(Optional.of(reminder));

        var authorized = new ReminderTaskActionHandler(repository, aliases, mock(IReminderService.class))
                .authorize(owner, oldOccurrenceId);

        assertThat(authorized.taskId()).isEqualTo(oldOccurrenceId);
        assertThat(authorized.instanceId()).isEqualTo(definitionId);
    }

    @Test
    void replayAuthorizationStillDeniesAFormerOccurrenceToAnotherOwner() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID definitionId = UUID.fromString("00000000-0000-0000-0000-000000000404");
        Reminder reminder = Reminder.builder()
                .id(definitionId).ownerUserId(owner)
                .scheduledAt(Instant.parse("2026-08-04T01:00:00Z"))
                .status(ReminderStatus.CANCELLED).title("Cancelled appointment").build();
        ReminderRepository repository = mock(ReminderRepository.class);
        when(repository.findById(definitionId)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> new ReminderTaskActionHandler(repository, mock(IReminderService.class))
                .authorizeReplay(other, UUID.randomUUID(), definitionId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
    }
}
