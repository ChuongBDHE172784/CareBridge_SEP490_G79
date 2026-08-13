package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.today.dto.TodayTaskCandidate;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskCadence;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.policy.ReminderAccessPolicy;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReminderTodayTaskProvider implements TodayTaskProvider {
    private final ReminderRepository reminderRepository;
    private final CareGroupRepository careGroupRepository;
    private final ReminderAccessPolicy accessPolicy;

    @Autowired
    public ReminderTodayTaskProvider(
            ReminderRepository reminderRepository,
            CareGroupRepository careGroupRepository,
            ReminderAccessPolicy accessPolicy) {
        this.reminderRepository = reminderRepository;
        this.careGroupRepository = careGroupRepository;
        this.accessPolicy = accessPolicy;
    }

    /** Legacy unit-test adapter: owner-only behavior without family sharing. */
    public ReminderTodayTaskProvider(
            ReminderRepository reminderRepository,
            CareGroupRepository careGroupRepository) {
        this(reminderRepository, careGroupRepository, null);
    }

    @Override
    public TaskKind taskKind() {
        return TaskKind.REMINDER;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodayTaskCandidate> findAuthorizedTasks(UUID actorUserId) {
        Map<UUID, Reminder> unique = new LinkedHashMap<>();
        Set<UUID> visibleOwnerIds = new LinkedHashSet<>();
        visibleOwnerIds.add(actorUserId);
        if (accessPolicy != null) {
            visibleOwnerIds.addAll(
                    careGroupRepository.findActiveOwnerUserIdsForChecklistViewer(actorUserId));
        }
        visibleOwnerIds.stream()
                .flatMap(ownerId -> reminderRepository
                        .findByOwnerUserIdAndStatusNot(ownerId, ReminderStatus.CANCELLED).stream())
                .filter(reminder -> canView(reminder, actorUserId))
                .forEach(reminder -> unique.putIfAbsent(reminder.getId(), reminder));
        return unique.values().stream()
                .filter(reminder -> canView(reminder, actorUserId))
                .map(reminder -> toCandidate(reminder, actorUserId))
                .toList();
    }

    private TodayTaskCandidate toCandidate(Reminder reminder, UUID actorUserId) {
        UUID contextId = reminder.getBabyId() != null ? reminder.getBabyId() : reminder.getJourneyId();
        ChecklistCareContextType contextType = reminder.getBabyId() != null
                ? ChecklistCareContextType.BABY
                : reminder.getJourneyId() != null ? ChecklistCareContextType.JOURNEY : null;
        ChecklistTargetSubject target = reminder.getBabyId() != null
                ? ChecklistTargetSubject.BABY
                : reminder.getJourneyId() != null ? ChecklistTargetSubject.MOTHER : null;
        ChecklistOrigin origin = reminder.getSourceReferenceId() != null
                || reminder.getSourceReferenceType() != null
                ? ChecklistOrigin.SYSTEM_TEMPLATE : ChecklistOrigin.USER_CREATED;
        Set<TaskAction> actions = EnumSet.noneOf(TaskAction.class);
        if ((reminder.getStatus() == ReminderStatus.PENDING
                || reminder.getStatus() == ReminderStatus.SNOOZED)
                && canComplete(reminder, actorUserId)) {
            actions.add(TaskAction.COMPLETE);
            actions.add(TaskAction.SKIP);
        }
        Instant dueAt = reminder.getStatus() == ReminderStatus.SNOOZED
                && reminder.getSnoozedUntil() != null
                ? reminder.getSnoozedUntil() : reminder.getScheduledAt();
        UUID occurrenceId = ReminderOccurrenceIdFactory.create(
                reminder.getId(), reminder.getScheduledAt(), reminder.getOccurrenceGeneration());
        return new TodayTaskCandidate(TaskKind.REMINDER, occurrenceId, null, null,
                resolveCareGroup(reminder, actorUserId), contextType, contextId, reminder.getTitle(), target,
                origin, reminder.getStatus().name(), actions, dueAt, null, reminder.getReminderType(),
                null, null, cadence(reminder.getRecurrenceType()));
    }

    private static TaskCadence cadence(RecurrenceType recurrenceType) {
        if (recurrenceType == RecurrenceType.DAILY) {
            return TaskCadence.DAILY;
        }
        if (recurrenceType == RecurrenceType.WEEKLY) {
            return TaskCadence.WEEKLY;
        }
        return recurrenceType == null || recurrenceType == RecurrenceType.NONE
                ? TaskCadence.ONCE : TaskCadence.UNKNOWN;
    }

    private UUID resolveCareGroup(Reminder reminder, UUID actorUserId) {
        if (accessPolicy != null) {
            return accessPolicy.presentationGroup(reminder, actorUserId)
                    .map(CareGroup::getId).orElse(null);
        }
        List<CareGroup> groups = reminder.getBabyId() != null
                ? careGroupRepository.findByLinkedBabyProfileId(reminder.getBabyId())
                : reminder.getJourneyId() != null
                        ? careGroupRepository.findByLinkedJourneyId(reminder.getJourneyId())
                        : List.of();
        return groups.stream()
                .filter(group -> reminder.getOwnerUserId().equals(group.getOwnerUserId()))
                .map(CareGroup::getId)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private boolean canView(Reminder reminder, UUID actorUserId) {
        return accessPolicy == null
                ? actorUserId.equals(reminder.getOwnerUserId())
                : accessPolicy.canView(reminder, actorUserId);
    }

    private boolean canComplete(Reminder reminder, UUID actorUserId) {
        return accessPolicy == null
                ? actorUserId.equals(reminder.getOwnerUserId())
                : accessPolicy.canComplete(reminder, actorUserId);
    }
}
