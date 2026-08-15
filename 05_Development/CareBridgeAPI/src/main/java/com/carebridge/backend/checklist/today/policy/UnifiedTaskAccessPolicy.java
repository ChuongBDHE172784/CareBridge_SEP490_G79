package com.carebridge.backend.checklist.today.policy;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UnifiedTaskAccessPolicy {
    private final CareGroupRepository groupRepository;
    private final CareGroupAuthorizationPolicy careGroupAuthorizationPolicy;
    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyProfileRepository;
    private final CareGroupMemberRepository memberRepository;

    public UnifiedTaskAccessPolicy(CareGroupRepository groupRepository,
                                   CareGroupAuthorizationPolicy careGroupAuthorizationPolicy,
                                   MotherJourneyRepository journeyRepository,
                                   BabyProfileRepository babyProfileRepository) {
        this(groupRepository, careGroupAuthorizationPolicy, journeyRepository,
                babyProfileRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public UnifiedTaskAccessPolicy(CareGroupRepository groupRepository,
                                   CareGroupAuthorizationPolicy careGroupAuthorizationPolicy,
                                   MotherJourneyRepository journeyRepository,
                                   BabyProfileRepository babyProfileRepository,
                                   CareGroupMemberRepository memberRepository) {
        this.groupRepository = groupRepository;
        this.careGroupAuthorizationPolicy = careGroupAuthorizationPolicy;
        this.journeyRepository = journeyRepository;
        this.babyProfileRepository = babyProfileRepository;
        this.memberRepository = memberRepository;
    }

    public boolean canView(ChecklistInstance instance, UUID actorUserId) {
        if (!isCanonicalRecipient(instance, actorUserId)) {
            return false;
        }
        if (instance.getRecipientRole() == ChecklistRecipientRole.MOTHER) {
            return instance.getCareGroupId() == null
                    && actorUserId.equals(instance.getContextOwnerUserId())
                    && hasOwnedPersonalContext(instance);
        }
        if (instance.getCareGroupId() == null || !hasCurrentGroupContext(instance)) {
            return false;
        }
        return careGroupAuthorizationPolicy.hasPermission(
                instance.getCareGroupId(), actorUserId, PermissionFlag.CHECKLIST_VIEW)
                && hasCurrentAccessEpoch(instance, actorUserId);
    }

    /** Explicit FAMILY scope. Mother checklist rows remain personal (careGroupId null),
     * so this path validates the selected group against the row's linked owner/context
     * instead of pretending the family member is the recipient. */
    public boolean canView(ChecklistInstance instance, UUID actorUserId, UUID careGroupId) {
        if (isPersonalFamilyInstance(instance, actorUserId, careGroupId)) {
            return careGroupAuthorizationPolicy.hasPermission(
                    careGroupId, actorUserId, PermissionFlag.CHECKLIST_VIEW)
                    && hasCurrentGroupContext(instance, careGroupId)
                    && hasCurrentAccessEpoch(instance, actorUserId);
        }
        if (!isCanonicalMotherInstance(instance) && !isSharedMotherUserCreatedInstance(instance)) {
            return false;
        }
        if (careGroupId == null || actorUserId == null
                || !careGroupAuthorizationPolicy.hasPermission(
                        careGroupId, actorUserId, PermissionFlag.CHECKLIST_VIEW)) {
            return false;
        }
        return groupRepository.findById(careGroupId)
                .filter(group -> group.getStatus() == CareGroupStatus.ACTIVE)
                .filter(group -> instance.getContextOwnerUserId().equals(group.getOwnerUserId()))
                .filter(group -> hasMatchingLinkedContext(instance, group))
                .filter(group -> hasActiveCanonicalContext(instance, group))
                .filter(group -> hasCurrentAccessEpochForGroup(careGroupId, actorUserId))
                .isPresent();
    }

    public boolean canComplete(ChecklistInstance instance, UUID actorUserId) {
        if (!canView(instance, actorUserId)) {
            return false;
        }
        return instance.getRecipientRole() == ChecklistRecipientRole.MOTHER
                || careGroupAuthorizationPolicy.hasPermission(
                instance.getCareGroupId(), actorUserId, PermissionFlag.CHECKLIST_COMPLETE);
    }

    public boolean canComplete(ChecklistInstance instance, UUID actorUserId, UUID careGroupId) {
        if (!canView(instance, actorUserId, careGroupId)) {
            return false;
        }
        // A Mother-owned canonical row is never a Family occurrence.  The
        // explicit care-group read route must not turn its legacy projection
        // into a writable row; Family work is materialized separately with a
        // retained member id and access epoch.
        if (isCanonicalMotherInstance(instance)) {
            return false;
        }
        // A FAMILY member may complete/reopen their own USER_CREATED task. The
        // separate CHECKLIST_COMPLETE grant controls mutations of the mother's
        // shared checklist, not the member's private task.
        if (isPersonalFamilyInstance(instance, actorUserId, careGroupId)) {
            return true;
        }
        return careGroupAuthorizationPolicy.hasPermission(
                careGroupId, actorUserId, PermissionFlag.CHECKLIST_COMPLETE);
    }

    private static boolean isPersonalFamilyInstance(
            ChecklistInstance instance, UUID actorUserId, UUID careGroupId) {
        return instance != null
                && actorUserId != null
                && careGroupId != null
                && instance.getRecipientRole() == ChecklistRecipientRole.FAMILY
                && instance.getOrigin() == com.carebridge.backend.checklist.model.ChecklistOrigin.USER_CREATED
                && actorUserId.equals(instance.getRecipientUserId())
                && careGroupId.equals(instance.getCareGroupId())
                && instance.getCareContextType() != null
                && instance.getCareContextId() != null
                && instance.getContextOwnerUserId() != null;
    }

    private static boolean isCanonicalMotherInstance(ChecklistInstance instance) {
        return instance != null
                && instance.getRecipientRole() == ChecklistRecipientRole.MOTHER
                && instance.getRecipientUserId() != null
                && instance.getRecipientUserId().equals(instance.getContextOwnerUserId())
                && instance.getCareGroupId() == null
                && instance.getOrigin() == com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE
                && instance.getCareContextType() != null
                && instance.getCareContextId() != null;
    }

    private static boolean isSharedMotherUserCreatedInstance(ChecklistInstance instance) {
        return instance != null
                && instance.getRecipientRole() == ChecklistRecipientRole.MOTHER
                && instance.getRecipientUserId() != null
                && instance.getRecipientUserId().equals(instance.getContextOwnerUserId())
                && instance.getOrigin() == com.carebridge.backend.checklist.model.ChecklistOrigin.USER_CREATED
                && instance.getCareContextType() != null
                && instance.getCareContextId() != null;
    }

    private static boolean isCanonicalRecipient(ChecklistInstance instance, UUID actorUserId) {
        return instance != null && actorUserId != null
                && actorUserId.equals(instance.getRecipientUserId())
                && instance.getCareContextType() != null
                && instance.getCareContextId() != null
                && instance.getContextOwnerUserId() != null;
    }

    private boolean hasCurrentAccessEpoch(ChecklistInstance instance, UUID actorUserId) {
        if (instance.getRecipientRole() != ChecklistRecipientRole.FAMILY) {
            return true;
        }
        // Lightweight policy tests use the compatibility four-argument
        // constructor and do not model membership rows. Production wiring
        // always supplies the repository, so a Family row is fail-closed there.
        if (memberRepository == null) {
            return true;
        }
        if (instance.getCareGroupMemberId() == null || instance.getChecklistAccessEpoch() == null) {
            return false;
        }
        return memberRepository.findById(instance.getCareGroupMemberId())
                .filter(member -> actorUserId.equals(member.getUserId()))
                .filter(member -> instance.getCareGroupId().equals(member.getCareGroupId()))
                .filter(member -> member.getInviteStatus()
                        == com.carebridge.backend.family.entity.InviteStatus.ACCEPTED)
                .filter(member -> member.getChecklistAccessQuarantineReasonCode() == null)
                .filter(member -> Objects.equals(
                        member.getChecklistAccessEpoch(), instance.getChecklistAccessEpoch()))
                .isPresent();
    }

    /**
     * Snapshot the current accepted membership epoch for an explicit Family
     * scope. A null epoch is fail-closed in production wiring; compatibility
     * policy tests use the four-argument constructor and intentionally bypass
     * membership metadata.
     */
    public Long currentAccessEpoch(UUID careGroupId, UUID actorUserId) {
        if (memberRepository == null || careGroupId == null || actorUserId == null) {
            return null;
        }
        return memberRepository.findByCareGroupIdAndUserId(careGroupId, actorUserId)
                .filter(member -> member.getInviteStatus()
                        == com.carebridge.backend.family.entity.InviteStatus.ACCEPTED)
                .filter(member -> member.getChecklistAccessQuarantineReasonCode() == null)
                .map(member -> member.getChecklistAccessEpoch())
                .orElse(null);
    }

    private boolean hasCurrentAccessEpochForGroup(UUID careGroupId, UUID actorUserId) {
        return memberRepository == null || currentAccessEpoch(careGroupId, actorUserId) != null;
    }

    private boolean hasOwnedPersonalContext(ChecklistInstance instance) {
        if (instance.getCareContextType() == com.carebridge.backend.checklist.model.ChecklistCareContextType.JOURNEY) {
            return journeyRepository.existsByIdAndOwnerUserIdAndStatus(
                    instance.getCareContextId(), instance.getContextOwnerUserId(), JourneyStatus.ACTIVE);
        }
        return babyProfileRepository
                .findByIdAndOwnerUserId(instance.getCareContextId(), instance.getContextOwnerUserId())
                .filter(baby -> baby.getStatus() == BabyProfileStatus.ACTIVE)
                .isPresent();
    }

    private boolean hasCurrentGroupContext(ChecklistInstance instance) {
        return hasCurrentGroupContext(instance, instance.getCareGroupId());
    }

    private boolean hasCurrentGroupContext(ChecklistInstance instance, UUID careGroupId) {
        return careGroupId != null && groupRepository.findById(careGroupId)
                .filter(group -> group.getStatus() == CareGroupStatus.ACTIVE)
                .filter(group -> instance.getContextOwnerUserId().equals(group.getOwnerUserId()))
                .filter(group -> hasMatchingLinkedContext(instance, group))
                .filter(group -> hasActiveCanonicalContext(instance, group))
                .map(group -> true)
                .orElse(false);
    }

    private boolean hasActiveCanonicalContext(ChecklistInstance instance, CareGroup group) {
        return switch (instance.getCareContextType()) {
            case JOURNEY -> journeyRepository.existsByIdAndOwnerUserIdAndStatus(
                            instance.getCareContextId(), group.getOwnerUserId(), JourneyStatus.ACTIVE);
            case BABY -> babyProfileRepository.findByIdAndOwnerUserId(
                                    instance.getCareContextId(), group.getOwnerUserId())
                            .filter(baby -> baby.getStatus() == BabyProfileStatus.ACTIVE)
                            .isPresent();
        };
    }

    private static boolean hasMatchingLinkedContext(ChecklistInstance instance, CareGroup group) {
        return switch (instance.getCareContextType()) {
            case JOURNEY -> instance.getCareContextId().equals(group.getLinkedJourneyId());
            case BABY -> instance.getCareContextId().equals(group.getLinkedBabyProfileId());
        };
    }

}
