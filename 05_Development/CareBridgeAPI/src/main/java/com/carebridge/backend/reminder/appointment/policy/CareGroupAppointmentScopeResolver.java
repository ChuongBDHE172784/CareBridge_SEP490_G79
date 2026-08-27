package com.carebridge.backend.reminder.appointment.policy;

import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolves the owner and active lifecycle context behind one selected group.
 * Appointment sharing is read-only and uses CALENDAR, which is default-deny
 * for an accepted FAMILY member when the permission key is absent.
 */
@Component
public class CareGroupAppointmentScopeResolver {

    private final CareGroupRepository groupRepository;
    private final CareGroupAuthorizationPolicy authorizationPolicy;
    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyRepository;

    public CareGroupAppointmentScopeResolver(
            CareGroupRepository groupRepository,
            CareGroupAuthorizationPolicy authorizationPolicy,
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository) {
        this.groupRepository = groupRepository;
        this.authorizationPolicy = authorizationPolicy;
        this.journeyRepository = journeyRepository;
        this.babyRepository = babyRepository;
    }

    public AppointmentScope resolveView(UUID actorUserId, UUID careGroupId) {
        if (actorUserId == null || careGroupId == null) return null;
        CareGroup group = groupRepository.findByIdAndStatus(careGroupId, CareGroupStatus.ACTIVE)
                .orElse(null);
        if (group == null) return null;

        boolean owner = Objects.equals(actorUserId, group.getOwnerUserId())
                || authorizationPolicy.isOwner(careGroupId, actorUserId);
        if (!owner && !authorizationPolicy.hasPermission(
                careGroupId, actorUserId, PermissionFlag.CALENDAR)) {
            return null;
        }

        List<LinkedContext> contexts = new ArrayList<>(2);
        if (group.getLinkedJourneyId() != null
                && isActiveJourney(group.getLinkedJourneyId(), group.getOwnerUserId())) {
            contexts.add(new LinkedContext(
                    ChecklistCareContextType.JOURNEY, group.getLinkedJourneyId()));
        }
        if (group.getLinkedBabyProfileId() != null
                && isActiveBaby(group.getLinkedBabyProfileId(), group.getOwnerUserId())) {
            contexts.add(new LinkedContext(
                    ChecklistCareContextType.BABY, group.getLinkedBabyProfileId()));
        }
        if (contexts.isEmpty()) return null;
        return new AppointmentScope(
                group.getId(), group.getGroupName(), group.getOwnerUserId(), List.copyOf(contexts));
    }

    private boolean isActiveJourney(UUID journeyId, UUID ownerUserId) {
        return journeyRepository.existsByIdAndOwnerUserIdAndStatus(
                journeyId, ownerUserId, JourneyStatus.ACTIVE);
    }

    private boolean isActiveBaby(UUID babyId, UUID ownerUserId) {
        return babyRepository.findByIdAndOwnerUserId(babyId, ownerUserId)
                .filter(baby -> baby.getStatus() == BabyProfileStatus.ACTIVE)
                .isPresent();
    }

    public record AppointmentScope(
            UUID careGroupId,
            String careGroupLabel,
            UUID ownerUserId,
            List<LinkedContext> linkedContexts) {
        public UUID linkedJourneyId() {
            return linkedContexts.stream()
                    .filter(context -> context.type() == ChecklistCareContextType.JOURNEY)
                    .map(LinkedContext::id)
                    .findFirst()
                    .orElse(null);
        }

        public UUID linkedBabyProfileId() {
            return linkedContexts.stream()
                    .filter(context -> context.type() == ChecklistCareContextType.BABY)
                    .map(LinkedContext::id)
                    .findFirst()
                    .orElse(null);
        }

        public boolean includes(UUID journeyId, UUID babyId) {
            return (journeyId != null && linkedJourneyId() != null
                    && linkedJourneyId().equals(journeyId))
                    || (babyId != null && linkedBabyProfileId() != null
                    && linkedBabyProfileId().equals(babyId));
        }
    }

    public record LinkedContext(ChecklistCareContextType type, UUID id) {
    }
}
