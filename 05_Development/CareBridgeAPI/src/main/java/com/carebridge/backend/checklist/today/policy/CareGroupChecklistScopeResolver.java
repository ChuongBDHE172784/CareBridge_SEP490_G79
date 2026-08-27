package com.carebridge.backend.checklist.today.policy;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolves an explicit FAMILY checklist scope. The resolver deliberately does
 * not select a group on behalf of a caller: group id, active membership,
 * permissions, owner and linked lifecycle context must all match.
 */
@Component
public class CareGroupChecklistScopeResolver {
    private final CareGroupRepository groupRepository;
    private final CareGroupAuthorizationPolicy authorizationPolicy;
    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public CareGroupChecklistScopeResolver(
            CareGroupRepository groupRepository,
            CareGroupAuthorizationPolicy authorizationPolicy,
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository) {
        this.groupRepository = groupRepository;
        this.authorizationPolicy = authorizationPolicy;
        this.journeyRepository = journeyRepository;
        this.babyRepository = babyRepository;
    }

    /** Compatibility constructor for lightweight unit tests. */
    public CareGroupChecklistScopeResolver(
            CareGroupRepository groupRepository,
            CareGroupAuthorizationPolicy authorizationPolicy) {
        this(groupRepository, authorizationPolicy, null, null);
    }

    public CareGroupChecklistScope resolveView(UUID actorUserId, UUID careGroupId) {
        return resolve(actorUserId, careGroupId, PermissionFlag.CHECKLIST_VIEW, false);
    }

    public CareGroupChecklistScope resolveComplete(UUID actorUserId, UUID careGroupId) {
        return resolve(actorUserId, careGroupId, PermissionFlag.CHECKLIST_COMPLETE, false);
    }

    /** Resolve while holding the care-group row lock for the surrounding transaction. */
    public CareGroupChecklistScope resolveViewForUpdate(UUID actorUserId, UUID careGroupId) {
        return resolve(actorUserId, careGroupId, PermissionFlag.CHECKLIST_VIEW, true);
    }

    /** Resolve completion permission while holding the care-group row lock. */
    public CareGroupChecklistScope resolveCompleteForUpdate(UUID actorUserId, UUID careGroupId) {
        return resolve(actorUserId, careGroupId, PermissionFlag.CHECKLIST_COMPLETE, true);
    }

    private CareGroupChecklistScope resolve(
            UUID actorUserId,
            UUID careGroupId,
            PermissionFlag permission,
            boolean lockGroup) {
        if (actorUserId == null || careGroupId == null) {
            return null;
        }
        CareGroup group;
        if (lockGroup) {
            group = groupRepository.findByIdForUpdate(careGroupId)
                    .filter(candidate -> candidate.getId().equals(careGroupId))
                    .filter(candidate -> candidate.getStatus() == CareGroupStatus.ACTIVE)
                    .orElseGet(() -> groupRepository
                            .findByIdAndStatus(careGroupId, CareGroupStatus.ACTIVE)
                            .orElse(null));
        } else {
            group = groupRepository.findByIdAndStatus(careGroupId, CareGroupStatus.ACTIVE)
                    .orElse(null);
        }
        if (group == null || Objects.equals(actorUserId, group.getOwnerUserId())
                || !authorizationPolicy.hasPermission(careGroupId, actorUserId, permission)) {
            return null;
        }
        List<LinkedContext> contexts = new ArrayList<>(2);
        if (group.getLinkedJourneyId() != null
                && isActiveLinkedContext(group, ChecklistCareContextType.JOURNEY,
                        group.getLinkedJourneyId())) {
            contexts.add(new LinkedContext(
                    ChecklistCareContextType.JOURNEY, group.getLinkedJourneyId()));
        }
        if (group.getLinkedBabyProfileId() != null
                && isActiveLinkedContext(group, ChecklistCareContextType.BABY,
                        group.getLinkedBabyProfileId())) {
            contexts.add(new LinkedContext(
                    ChecklistCareContextType.BABY, group.getLinkedBabyProfileId()));
        }
        if (contexts.isEmpty()) {
            return null;
        }
        return new CareGroupChecklistScope(
                group.getId(), group.getGroupName(), group.getOwnerUserId(), List.copyOf(contexts));
    }

    private boolean isActiveLinkedContext(
            CareGroup group, ChecklistCareContextType type, UUID contextId) {
        if (contextId == null) {
            return true;
        }
        return switch (type) {
            case JOURNEY -> journeyRepository == null
                    || journeyRepository.existsByIdAndOwnerUserIdAndStatus(
                            contextId, group.getOwnerUserId(), JourneyStatus.ACTIVE);
            case BABY -> babyRepository == null
                    || babyRepository.findByIdAndOwnerUserId(contextId, group.getOwnerUserId())
                            .filter(baby -> baby.getStatus() == BabyProfileStatus.ACTIVE)
                            .isPresent();
        };
    }

    public record CareGroupChecklistScope(
            UUID careGroupId,
            String careGroupLabel,
            UUID ownerUserId,
            List<LinkedContext> linkedContexts) {
        public boolean includes(ChecklistCareContextType type, UUID contextId) {
            return linkedContexts.stream().anyMatch(context ->
                    context.type() == type && Objects.equals(context.id(), contextId));
        }

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
    }

    public record LinkedContext(ChecklistCareContextType type, UUID id) {
    }
}
