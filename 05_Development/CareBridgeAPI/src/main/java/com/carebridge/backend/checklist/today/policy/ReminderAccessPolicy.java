package com.carebridge.backend.checklist.today.policy;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.reminder.entity.Reminder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Shared authorization boundary for reminder projection and unified task actions. */
@Component
@RequiredArgsConstructor
public class ReminderAccessPolicy {
    private final CareGroupRepository groupRepository;
    private final CareGroupAuthorizationPolicy familyAuthorization;
    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyRepository;

    public boolean canView(Reminder reminder, UUID actorUserId) {
        if (isOwner(reminder, actorUserId)) {
            return true;
        }
        return uniqueActiveContextGroup(reminder)
                .filter(group -> familyAuthorization.hasPermission(
                        group.getId(), actorUserId, PermissionFlag.CHECKLIST_VIEW))
                .isPresent();
    }

    public boolean canComplete(Reminder reminder, UUID actorUserId) {
        if (isOwner(reminder, actorUserId)) {
            return true;
        }
        return uniqueActiveContextGroup(reminder)
                .filter(group -> familyAuthorization.hasPermission(
                        group.getId(), actorUserId, PermissionFlag.CHECKLIST_VIEW))
                .filter(group -> familyAuthorization.hasPermission(
                        group.getId(), actorUserId, PermissionFlag.CHECKLIST_COMPLETE))
                .isPresent();
    }

    /** Returns metadata only when the scoped reminder has one unambiguous active group. */
    public Optional<CareGroup> presentationGroup(Reminder reminder, UUID actorUserId) {
        if (isOwner(reminder, actorUserId)) {
            return uniqueActiveContextGroup(reminder);
        }
        return uniqueActiveContextGroup(reminder)
                .filter(group -> familyAuthorization.hasPermission(
                        group.getId(), actorUserId, PermissionFlag.CHECKLIST_VIEW));
    }

    private Optional<CareGroup> uniqueActiveContextGroup(Reminder reminder) {
        if (reminder == null || reminder.getOwnerUserId() == null) {
            return Optional.empty();
        }
        if (!contextBelongsToReminderOwner(reminder)) {
            return Optional.empty();
        }
        List<CareGroup> candidates;
        if (reminder.getBabyId() != null && reminder.getJourneyId() == null) {
            candidates = groupRepository.findByLinkedBabyProfileId(reminder.getBabyId());
        } else if (reminder.getJourneyId() != null && reminder.getBabyId() == null) {
            candidates = groupRepository.findByLinkedJourneyId(reminder.getJourneyId());
        } else {
            return Optional.empty();
        }
        List<CareGroup> matches = candidates.stream()
                .filter(group -> group.getStatus() == CareGroupStatus.ACTIVE)
                .filter(group -> reminder.getOwnerUserId().equals(group.getOwnerUserId()))
                .filter(group -> matchesContext(reminder, group))
                .toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    private boolean contextBelongsToReminderOwner(Reminder reminder) {
        if (reminder.getBabyId() != null && reminder.getJourneyId() == null) {
            return babyRepository.findByIdAndOwnerUserId(
                    reminder.getBabyId(), reminder.getOwnerUserId()).isPresent();
        }
        if (reminder.getJourneyId() != null && reminder.getBabyId() == null) {
            return journeyRepository.existsByIdAndOwnerUserId(
                    reminder.getJourneyId(), reminder.getOwnerUserId());
        }
        return false;
    }

    private static boolean matchesContext(Reminder reminder, CareGroup group) {
        return reminder.getBabyId() != null
                ? reminder.getBabyId().equals(group.getLinkedBabyProfileId())
                : reminder.getJourneyId().equals(group.getLinkedJourneyId());
    }

    private static boolean isOwner(Reminder reminder, UUID actorUserId) {
        return reminder != null && actorUserId != null
                && actorUserId.equals(reminder.getOwnerUserId());
    }
}
