package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.policy.ChecklistTemplateVisibilityPolicy;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.family.dto.FamilyPermission;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Enumerates REQUEST candidates from inline template metadata and canonical domain ownership. */
@Component
public class JpaChecklistReconciliationSource implements ChecklistReconciliationSource {

    private final ChecklistTemplateRepository templateRepository;
    private final ChecklistItemRepository itemRepository;
    private final CareGroupRepository careGroupRepository;
    private final CareGroupMemberRepository memberRepository;
    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyRepository;
    private final ChecklistInstanceRepository instanceRepository;

    public JpaChecklistReconciliationSource(
            ChecklistTemplateRepository templateRepository,
            ChecklistItemRepository itemRepository,
            CareGroupRepository careGroupRepository,
            CareGroupMemberRepository memberRepository,
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository) {
        this(templateRepository, itemRepository, careGroupRepository, memberRepository,
                journeyRepository, babyRepository, null);
    }

    @Autowired
    public JpaChecklistReconciliationSource(
            ChecklistTemplateRepository templateRepository,
            ChecklistItemRepository itemRepository,
            CareGroupRepository careGroupRepository,
            CareGroupMemberRepository memberRepository,
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository,
            ChecklistInstanceRepository instanceRepository) {
        this.templateRepository = templateRepository;
        this.itemRepository = itemRepository;
        this.careGroupRepository = careGroupRepository;
        this.memberRepository = memberRepository;
        this.journeyRepository = journeyRepository;
        this.babyRepository = babyRepository;
        this.instanceRepository = instanceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistDistributionCommand> loadCandidatesForActor(
            UUID actorUserId,
            LocalDate effectiveDate,
            ZoneId timezone,
            UUID correlationId) {
        if (actorUserId == null || effectiveDate == null || timezone == null || correlationId == null) {
            return List.of();
        }

        List<AuthorizedFamilyGroup> authorizedGroups = authorizedFamilyGroups(actorUserId);
        List<ChecklistInstance> discoveredPersonalInstances = instanceRepository == null
                ? List.of()
                : instanceRepository.findByRecipientUserIdAndHistoricalAtIsNull(actorUserId).stream()
                        .filter(instance -> instance.getRecipientRole() == ChecklistRecipientRole.MOTHER)
                        .filter(instance -> instance.getOrigin() == ChecklistOrigin.SYSTEM_TEMPLATE)
                        .filter(instance -> instance.getHistoricalAt() == null)
                        .filter(instance -> instance.getStatus() != com.carebridge.backend.checklist.model.ChecklistInstanceStatus.CANCELLED)
                        .toList();
        Map<UUID, ChecklistTemplate> currentTemplatesByVersion = templatesByVersion(discoveredPersonalInstances);
        List<ChecklistInstance> currentPersonalInstances = discoveredPersonalInstances.stream()
                .filter(instance -> ChecklistTemplateVisibilityPolicy.isVisible(
                        instance, currentTemplatesByVersion.get(instance.getTemplateVersionId())))
                .toList();
        List<ChecklistTemplate> approvedTemplates = templateRepository
                .findAllDistributionEnabledByStatus(ChecklistTemplateStatus.APPROVED);
        boolean activeLegacyPreconception = approvedTemplates.stream()
                .anyMatch(this::isActiveMotherLegacyCandidate);
        List<ChecklistDistributionCommand> commands = new ArrayList<>();
        for (ChecklistTemplate template : approvedTemplates) {
            Set<ChecklistRecipientRole> roles = recipientRoles(template);
            if (roles.isEmpty()) {
                continue;
            }
            ChecklistLifecycleEligibility eligibility = inlineEligibility(template);
            List<ChecklistDistributionItem> items = itemRepository.findByTemplate_IdOrderByOrder(template.getId())
                    .stream()
                    .map(this::toDistributionItem)
                    .toList();

            if (roles.contains(ChecklistRecipientRole.MOTHER)) {
                for (ContextSeed context : personalContexts(actorUserId, template.getStage())) {
                    if (isSequenceTemplate(template) && activeLegacyPreconception) {
                        continue;
                    }
                    if (isSequenceTemplate(template)
                            && !isCurrentSequenceCandidate(
                                    template, context.id(), currentPersonalInstances, currentTemplatesByVersion)) {
                        continue;
                    }
                    commands.add(actorCommand(
                            template, null, context, actorUserId, ChecklistRecipientRole.MOTHER,
                            true, eligibility, items, effectiveDate, timezone, correlationId));
                }
            }
            if (roles.contains(ChecklistRecipientRole.FAMILY)) {
                for (AuthorizedFamilyGroup authorizedGroup : authorizedGroups) {
                    for (ContextSeed context : directContexts(authorizedGroup.group(), template.getStage())) {
                        commands.add(actorCommand(
                                template, authorizedGroup.group(), context, actorUserId,
                                ChecklistRecipientRole.FAMILY,
                                authorizedGroup.permission().isChecklistComplete(), eligibility, items,
                                effectiveDate, timezone, correlationId));
                    }
                }
            }
        }
        return commands.stream()
                .sorted(Comparator.comparing(EnsureEligibleChecklistAssignmentsService::signature))
                .toList();
    }

    private static Set<ChecklistRecipientRole> recipientRoles(ChecklistTemplate template) {
        if (template.getRecipientScope() == null) {
            return Set.of();
        }
        return switch (template.getRecipientScope()) {
            case MOTHER -> Set.of(ChecklistRecipientRole.MOTHER);
            case FAMILY -> Set.of(ChecklistRecipientRole.FAMILY);
            case BOTH -> Set.of(ChecklistRecipientRole.MOTHER, ChecklistRecipientRole.FAMILY);
        };
    }

    private static ChecklistLifecycleEligibility inlineEligibility(ChecklistTemplate template) {
        if (template.getStage() == null) {
            return null;
        }
        return new InlineLifecycleEligibility(
                template.getStage().name(),
                template.getEligibilityAnchorType(),
                template.getEligibilityRangeUnit(),
                template.getEligibilityStartInclusive(),
                template.getEligibilityEndInclusive(),
                true);
    }

    private List<AuthorizedFamilyGroup> authorizedFamilyGroups(UUID actorUserId) {
        Map<UUID, AuthorizedFamilyGroup> groups = new LinkedHashMap<>();
        memberRepository.findByUserIdAndInviteStatus(actorUserId, InviteStatus.ACCEPTED).forEach(member -> {
            FamilyPermission permission = FamilyPermission.fromJson(member.getPermissionJson());
            if (!permission.isChecklistView() || member.getMemberRole() == GroupMemberRole.OWNER) {
                return;
            }
            careGroupRepository.findByIdAndStatus(member.getCareGroupId(), CareGroupStatus.ACTIVE)
                    .filter(group -> !Objects.equals(group.getOwnerUserId(), actorUserId))
                    .ifPresent(group -> groups.putIfAbsent(
                            group.getId(), new AuthorizedFamilyGroup(group, permission)));
        });
        return List.copyOf(groups.values());
    }

    private List<ContextSeed> personalContexts(UUID actorUserId, ContentStage stage) {
        List<ContextSeed> contexts = new ArrayList<>();
        if (stage == null || stage == ContentStage.PRE_PREGNANCY
                || stage == ContentStage.PREGNANCY || stage == ContentStage.POSTPARTUM) {
            journeyRepository.findCanonical(actorUserId)
                    .filter(journey -> stage == null || stage.name().equals(journey.getJourneyType().name()))
                    .map(this::journeyContext)
                    .ifPresent(contexts::add);
        }
        if (stage == null || stage == ContentStage.POSTPARTUM) {
            babyRepository.findByOwnerUserIdAndStatusOrderByCreatedAtAsc(actorUserId, BabyProfileStatus.ACTIVE)
                    .stream()
                    .map(this::babyContext)
                    .forEach(contexts::add);
        }
        return List.copyOf(contexts);
    }

    private List<ContextSeed> directContexts(CareGroup group, ContentStage stage) {
        List<ContextSeed> contexts = new ArrayList<>();
        if (stage == null || stage == ContentStage.PRE_PREGNANCY
                || stage == ContentStage.PREGNANCY || stage == ContentStage.POSTPARTUM) {
            UUID journeyId = group.getLinkedJourneyId();
            if (journeyId != null) {
                journeyRepository.findById(journeyId)
                        .filter(journey -> journey.getStatus() == JourneyStatus.ACTIVE)
                        .filter(journey -> Objects.equals(journey.getOwnerUserId(), group.getOwnerUserId()))
                        .filter(journey -> stage == null || stage.name().equals(journey.getJourneyType().name()))
                        .map(this::journeyContext)
                        .ifPresent(contexts::add);
            }
        }
        if (stage == null || stage == ContentStage.POSTPARTUM) {
            UUID babyId = group.getLinkedBabyProfileId();
            if (babyId != null) {
                babyRepository.findById(babyId)
                        .filter(baby -> baby.getStatus() == BabyProfileStatus.ACTIVE)
                        .filter(baby -> Objects.equals(baby.getOwnerUserId(), group.getOwnerUserId()))
                        .map(this::babyContext)
                        .ifPresent(contexts::add);
            }
        }
        return List.copyOf(contexts);
    }

    private static ChecklistDistributionCommand actorCommand(
            ChecklistTemplate template,
            CareGroup group,
            ContextSeed context,
            UUID actorUserId,
            ChecklistRecipientRole role,
            boolean checklistComplete,
            ChecklistLifecycleEligibility eligibility,
            List<ChecklistDistributionItem> items,
            LocalDate effectiveDate,
            ZoneId timezone,
            UUID correlationId) {
        return new ChecklistDistributionCommand(
                template.getTemplateLineageId() == null ? template.getId() : template.getTemplateLineageId(),
                template.getTemplateVersionId(),
                group == null ? null : group.getId(),
                group == null ? actorUserId : group.getOwnerUserId(),
                context.type(),
                context.id(),
                context.ownerUserId(),
                template.getStage(),
                eligibility,
                context.dates(),
                effectiveDate,
                timezone,
                List.of(new ChecklistDistributionRecipient(
                        actorUserId, role, true, true, checklistComplete)),
                items,
                correlationId);
    }

    private ContextSeed journeyContext(MotherJourney journey) {
        return new ContextSeed(
                ChecklistCareContextType.JOURNEY,
                journey.getId(),
                journey.getOwnerUserId(),
                new ChecklistLifecycleDates(
                        journey.getLastMenstrualDate(), journey.getEstimatedDueDate(),
                        journey.getDeliveryDate(), null));
    }

    private ContextSeed babyContext(BabyProfile baby) {
        return new ContextSeed(
                ChecklistCareContextType.BABY,
                baby.getId(),
                baby.getOwnerUserId(),
                new ChecklistLifecycleDates(null, null, null, baby.getBirthDate()));
    }

    private ChecklistDistributionItem toDistributionItem(ChecklistItem item) {
        return new ChecklistDistributionItem(
                item.getId(),
                item.getItemText(),
                item.getOrder() == null ? 0 : item.getOrder(),
                Boolean.TRUE.equals(item.getIsRequired()),
                item.getTargetSubject(),
                item.getDueAnchorType(),
                item.getDueOffsetStart(),
                item.getDueOffsetUnit());
    }

    private boolean isSequenceTemplate(ChecklistTemplate template) {
        return template.getStage() == ContentStage.PRE_PREGNANCY
                && template.getRecipientScope() == com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER
                && template.getTemplateType() == com.carebridge.backend.content.entity.ChecklistTemplateType.MANDATORY
                && template.getSequencePosition() != null && template.getSequencePosition() > 0;
    }

    private boolean isActiveMotherLegacyCandidate(ChecklistTemplate template) {
        return template.getStage() == ContentStage.PRE_PREGNANCY
                && template.getRecipientScope() != null
                && (template.getRecipientScope()
                        == com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER
                    || template.getRecipientScope()
                        == com.carebridge.backend.checklist.model.ChecklistRecipientScope.BOTH)
                && template.getTemplateType() == com.carebridge.backend.content.entity.ChecklistTemplateType.MANDATORY
                && (template.getSequencePosition() == null || template.getSequencePosition() <= 0);
    }

    private boolean isCurrentSequenceCandidate(
            ChecklistTemplate template,
            UUID contextId,
            List<ChecklistInstance> currentInstances,
            Map<UUID, ChecklistTemplate> currentTemplatesByVersion) {
        List<ChecklistInstance> contextInstances = currentInstances.stream()
                .filter(instance -> instance.getCareContextType() == ChecklistCareContextType.JOURNEY)
                .filter(instance -> Objects.equals(instance.getCareContextId(), contextId))
                .toList();
        if (contextInstances.isEmpty()) {
            return template.getSequencePosition() == 1;
        }
        boolean legacy = contextInstances.stream().anyMatch(instance -> {
            ChecklistTemplate existing = currentTemplatesByVersion.get(instance.getTemplateVersionId());
            return existing == null || existing.getSequencePosition() == null || existing.getSequencePosition() <= 0;
        });
        if (legacy) {
            return false;
        }
        return contextInstances.stream().anyMatch(instance ->
                Objects.equals(instance.getTemplateVersionId(), template.getTemplateVersionId()));
    }

    private Map<UUID, ChecklistTemplate> templatesByVersion(List<ChecklistInstance> instances) {
        List<UUID> versionIds = instances.stream()
                .map(ChecklistInstance::getTemplateVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (versionIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ChecklistTemplate> templates = new LinkedHashMap<>();
        for (ChecklistTemplate template : templateRepository.findAllByTemplateVersionIdIn(versionIds)) {
            templates.putIfAbsent(template.getTemplateVersionId(), template);
        }
        return Map.copyOf(templates);
    }

    private record ContextSeed(
            ChecklistCareContextType type,
            UUID id,
            UUID ownerUserId,
            ChecklistLifecycleDates dates) {
    }

    private record AuthorizedFamilyGroup(CareGroup group, FamilyPermission permission) {
    }

    private record InlineLifecycleEligibility(
            String stage,
            ChecklistAnchorType anchorType,
            ChecklistRangeUnit rangeUnit,
            Integer startInclusive,
            Integer endInclusive,
            Boolean active) implements ChecklistLifecycleEligibility {

        @Override
        public String getStage() {
            return stage;
        }

        @Override
        public ChecklistAnchorType getAnchorType() {
            return anchorType;
        }

        @Override
        public ChecklistRangeUnit getRangeUnit() {
            return rangeUnit;
        }

        @Override
        public Integer getStartInclusive() {
            return startInclusive;
        }

        @Override
        public Integer getEndInclusive() {
            return endInclusive;
        }

        @Override
        public Boolean getActive() {
            return active;
        }
    }
}
