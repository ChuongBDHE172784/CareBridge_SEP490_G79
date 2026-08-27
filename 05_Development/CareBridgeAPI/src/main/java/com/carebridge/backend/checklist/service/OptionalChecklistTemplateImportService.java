package com.carebridge.backend.checklist.service;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionCommand;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionItem;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionRecipient;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionResult;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionService;
import com.carebridge.backend.checklist.distribution.ChecklistLifecycleDates;
import com.carebridge.backend.checklist.distribution.ChecklistLifecycleEligibility;
import com.carebridge.backend.checklist.dto.SelfAssignChecklistTemplateRequest;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Canonical V2 self-assignment for approved optional templates. */
@Service
@RequiredArgsConstructor
public class OptionalChecklistTemplateImportService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ChecklistTemplateRepository templateRepository;
    private final ChecklistItemRepository itemRepository;
    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyRepository;
    private final ChecklistDistributionService distributionService;

    @Transactional
    public ChecklistDistributionResult selfAssign(
            SelfAssignChecklistTemplateRequest request,
            UUID actorUserId) {
        if (request == null || actorUserId == null
                || (request.journeyId() != null && request.babyId() != null)) {
            throw invalid("CHECKLIST_CONTEXT_REQUIRED", "At most one checklist care context is allowed");
        }

        ChecklistTemplate template = templateRepository.findById(request.templateId())
                .filter(value -> value.getStatus() == ChecklistTemplateStatus.APPROVED)
                .filter(value -> value.getTemplateType() == ChecklistTemplateType.OPTIONAL)
                .filter(value -> !Boolean.TRUE.equals(value.getMigrationReviewRequired()))
                .orElseThrow(OptionalChecklistTemplateImportService::templateUnavailable);
        UUID versionId = Objects.requireNonNull(template.getTemplateVersionId(), "Template version is required");
        boolean motherAllowed = template.getRecipientScope() == ChecklistRecipientScope.MOTHER
                || template.getRecipientScope() == ChecklistRecipientScope.BOTH;
        if (!motherAllowed) {
            throw templateUnavailable();
        }

        ResolvedContext context = resolveContext(request, actorUserId, template);
        ChecklistLifecycleEligibility eligibility = inlineEligibility(template);
        if (template.getStage() != null && eligibility == null) {
            throw templateUnavailable();
        }
        List<ChecklistDistributionItem> items = itemRepository
                .findByTemplate_IdOrderByOrder(template.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                .map(this::toDistributionItem)
                .toList();
        if (items.isEmpty()) {
            throw invalid("CHECKLIST_TEMPLATE_EMPTY", "Optional checklist template has no active items");
        }

        ChecklistDistributionCommand command = new ChecklistDistributionCommand(
                template.getTemplateLineageId() == null ? template.getId() : template.getTemplateLineageId(),
                versionId,
                null,
                actorUserId,
                context.type(),
                context.id(),
                actorUserId,
                template.getStage(),
                eligibility,
                context.dates(),
                LocalDate.now(DEFAULT_ZONE),
                DEFAULT_ZONE,
                List.of(new ChecklistDistributionRecipient(
                        actorUserId, ChecklistRecipientRole.MOTHER, true, true, true)),
                items,
                UUID.randomUUID(),
                null,
                null,
                template.getChecklistContractVersion());
        ChecklistDistributionResult result = distributionService.distribute(command);
        if (result.conflicts() > 0 || result.failures() > 0 || result.deniedRecipients() > 0
                || result.createdTasks() + result.existingTasks() == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "CHECKLIST_SELF_ASSIGN_FAILED",
                    "Optional checklist could not be added to Today tasks");
        }
        return result;
    }

    private static ChecklistLifecycleEligibility inlineEligibility(ChecklistTemplate template) {
        if (template.getStage() == null) {
            return null;
        }
        if (template.getEligibilityAnchorType() == null
                || template.getEligibilityRangeUnit() == null
                || template.getEligibilityStartInclusive() == null
                || template.getEligibilityEndInclusive() == null) {
            return null;
        }
        return new InlineEligibility(
                template.getStage().name(),
                template.getEligibilityAnchorType(),
                template.getEligibilityRangeUnit(),
                template.getEligibilityStartInclusive(),
                template.getEligibilityEndInclusive(),
                true);
    }

    private ResolvedContext resolveContext(
            SelfAssignChecklistTemplateRequest request,
            UUID actorUserId,
            ChecklistTemplate template) {
        ContentStage templateStage = template.getStage();
        if (template.getEligibilityAnchorType() == ChecklistAnchorType.BIRTH_DATE) {
            if (request.journeyId() != null) {
                throw templateUnavailable();
            }
            UUID babyId = request.babyId() == null
                    ? babyRepository.findActiveBabyId(actorUserId)
                            .orElseThrow(OptionalChecklistTemplateImportService::contextUnavailable)
                    : request.babyId();
            BabyProfile baby = babyRepository.findOwnedActiveByIdForUpdate(babyId, actorUserId)
                    .orElseThrow(OptionalChecklistTemplateImportService::contextUnavailable);
            return new ResolvedContext(ChecklistCareContextType.BABY, baby.getId(),
                    new ChecklistLifecycleDates(null, null, null, baby.getBirthDate()));
        }

        if (request.babyId() != null) {
            throw templateUnavailable();
        }

        MotherJourney journey = request.journeyId() == null
                ? journeyRepository.findCanonical(actorUserId)
                        .orElseThrow(OptionalChecklistTemplateImportService::contextUnavailable)
                : journeyRepository.findByIdAndOwnerUserIdAndStatus(
                                request.journeyId(), actorUserId, JourneyStatus.ACTIVE)
                        .orElseThrow(OptionalChecklistTemplateImportService::contextUnavailable);
        if (templateStage == null || !templateStage.name().equals(journey.getJourneyType().name())) {
            throw templateUnavailable();
        }
        return new ResolvedContext(ChecklistCareContextType.JOURNEY, journey.getId(),
                new ChecklistLifecycleDates(journey.getLastMenstrualDate(), journey.getEstimatedDueDate(),
                        journey.getDeliveryDate(), null));
    }

    private ChecklistDistributionItem toDistributionItem(ChecklistItem item) {
        return new ChecklistDistributionItem(
                item.getId(), item.getItemText(), item.getOrder() == null ? 0 : item.getOrder(),
                Boolean.TRUE.equals(item.getIsRequired()), item.getTargetSubject(), item.getDueAnchorType(),
                item.getDueOffsetStart(), item.getDueOffsetUnit(), item.getDescription(),
                item.getSupportFunction());
    }

    private static BusinessException invalid(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    private static BusinessException templateUnavailable() {
        return new BusinessException(HttpStatus.NOT_FOUND, "CHECKLIST_TEMPLATE_UNAVAILABLE",
                "Optional checklist template is unavailable");
    }

    private static BusinessException contextUnavailable() {
        return new BusinessException(HttpStatus.NOT_FOUND, "CHECKLIST_CONTEXT_UNAVAILABLE",
                "Checklist care context is unavailable");
    }

    private record ResolvedContext(
            ChecklistCareContextType type,
            UUID id,
            ChecklistLifecycleDates dates) {
    }

    private record InlineEligibility(
            String stage,
            ChecklistAnchorType anchorType,
            ChecklistRangeUnit rangeUnit,
            Integer startInclusive,
            Integer endInclusive,
            Boolean active) implements ChecklistLifecycleEligibility {
        @Override public String getStage() { return stage; }
        @Override public ChecklistAnchorType getAnchorType() { return anchorType; }
        @Override public ChecklistRangeUnit getRangeUnit() { return rangeUnit; }
        @Override public Integer getStartInclusive() { return startInclusive; }
        @Override public Integer getEndInclusive() { return endInclusive; }
        @Override public Boolean getActive() { return active; }
    }
}
