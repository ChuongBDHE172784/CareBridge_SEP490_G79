package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.checklist.audit.ChecklistAuditActorType;
import com.carebridge.backend.checklist.audit.ChecklistAuditEvent;
import com.carebridge.backend.checklist.audit.ChecklistAuditResourceType;
import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateDecisionResponse;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentDecision;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.notification.service.ContentReviewNotificationService;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// UC-243 §14 addendum — mirrors ContentApprovalServiceImpl, kept separate because ChecklistTemplate
// has neither versionNo nor publishedAt (see ChecklistTemplateApprovalService javadoc).
@Service
@RequiredArgsConstructor
public class ChecklistTemplateApprovalServiceImpl implements ChecklistTemplateApprovalService {

    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistAuditWriter auditService;
    private final ContentReviewNotificationService contentReviewNotificationService;
    private final ChecklistItemRepository checklistItemRepository;
    private final ChecklistInstanceRepository checklistInstanceRepository;

    @Override
    @Transactional
    public ChecklistTemplateDecisionResponse decide(UUID id, ContentDecisionRequest request, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        if (request == null || request.decision() == null) {
            throw new IllegalArgumentException("Checklist template decision is required");
        }
        ContentDecision decision = request.decision();

        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);

        boolean approving = decision == ContentDecision.APPROVE;
        boolean validApprovalStatus = template.getStatus() == ChecklistTemplateStatus.DRAFT
                || template.getStatus() == ChecklistTemplateStatus.PENDING_REVIEW;
        if ((approving && !validApprovalStatus)
                || (!approving && template.getStatus() != ChecklistTemplateStatus.PENDING_REVIEW)) {
            throw ContentException.checklistTemplateNotPendingReview();
        }

        if (approving && Boolean.TRUE.equals(template.getMigrationReviewRequired())) {
            throw ContentException.migrationReviewRequired();
        }
        if (approving && template.getMigrationReviewedAt() != null) {
            throw ContentException.migrationReviewRequired();
        }

        if (decision == ContentDecision.REJECT
                && (request.reason() == null || request.reason().isBlank())) {
            throw ContentException.checklistTemplateDecisionReasonRequired();
        }

        ChecklistTemplateStatus previousStatus = template.getStatus();
        ChecklistTemplateStatus newStatus = decision == ContentDecision.APPROVE
                ? ChecklistTemplateStatus.APPROVED
                : ChecklistTemplateStatus.DRAFT;
        if (approving) {
            validateAuthoringContract(template);
            prepareSequenceApproval(template);
        }
        template.setStatus(newStatus);

        Instant decidedAt = Instant.now();
        if (decision == ContentDecision.REJECT) {
            template.setRevisionReason(request.reason().trim());
            template.setRevisionRequestedAt(decidedAt);
            template.setRevisionRequestedBy(adminUserId);
            template.setRevisionRequestedVersion(template.getVersionNo());
        } else {
            clearReviewFeedback(template);
            template.setMigrationReviewRequired(false);
            template.setDistributionEnabled(isMandatory(template));
            template.setApprovedAt(decidedAt);
            template.setApprovedBy(adminUserId);
        }
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        UUID correlationId = UUID.randomUUID();
        writeDecisionAudit(saved, adminUserId,
                decision == ContentDecision.APPROVE ? "TEMPLATE_APPROVED" : "TEMPLATE_REJECTED",
                correlationId);

        if (decision == ContentDecision.REJECT) {
            contentReviewNotificationService.notifyReturned(
                    saved.getAuthorUserId(), saved.getId(), "CHECKLIST", saved.getName(),
                    request.reason().trim(), "/content/checklists/" + saved.getId() + "/edit");
        }

        return new ChecklistTemplateDecisionResponse(
                saved.getId(), previousStatus, saved.getStatus(), adminUserId, request.reason(), decidedAt);
    }

    @Override
    @Transactional
    public ChecklistTemplateDecisionResponse decideInLineage(
            UUID lineageId, UUID id, ContentDecisionRequest request, Principal principal) {
        return decide(resolveEntityIdInLineage(lineageId, id), request, principal);
    }

    @Override
    @Transactional
    public ChecklistTemplateDecisionResponse reviewImported(UUID id, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);
        if (!Boolean.TRUE.equals(template.getMigrationReviewRequired())) {
            throw ContentException.checklistTemplateInvalidStatusTransition();
        }
        if (template.getStatus() != ChecklistTemplateStatus.PENDING_REVIEW) {
            throw ContentException.checklistTemplateNotPendingReview();
        }
        validateAuthoringContract(template);

        ChecklistTemplateStatus previousStatus = template.getStatus();
        template.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
        template.setMigrationReviewRequired(false);
        template.setMigrationReviewedAt(Instant.now());
        template.setMigrationReviewedBy(adminUserId);
        template.setDistributionEnabled(false);
        template.setApprovedAt(null);
        template.setApprovedBy(null);
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        Instant reviewedAt = Instant.now();
        writeDecisionAudit(saved, adminUserId, "MIGRATION_REVIEW_COMPLETED", UUID.randomUUID());
        return new ChecklistTemplateDecisionResponse(
                saved.getId(), previousStatus, saved.getStatus(), adminUserId, null, reviewedAt);
    }

    @Override
    @Transactional
    public ChecklistTemplateDecisionResponse reviewImportedInLineage(
            UUID lineageId, UUID id, Principal principal) {
        return reviewImported(resolveEntityIdInLineage(lineageId, id), principal);
    }

    @Override
    @Transactional
    public ChecklistTemplateDecisionResponse activateImported(UUID id, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);
        if (Boolean.TRUE.equals(template.getMigrationReviewRequired())) {
            throw ContentException.migrationReviewRequired();
        }
        if (template.getMigrationReviewedAt() == null) {
            throw ContentException.migrationReviewRequired();
        }
        if (template.getStatus() != ChecklistTemplateStatus.PENDING_REVIEW) {
            throw ContentException.checklistTemplateNotPendingReview();
        }
        validateAuthoringContract(template);
        prepareSequenceApproval(template);

        ChecklistTemplateStatus previousStatus = template.getStatus();
        Instant activatedAt = Instant.now();
        template.setStatus(ChecklistTemplateStatus.APPROVED);
        template.setDistributionEnabled(isMandatory(template));
        template.setApprovedAt(activatedAt);
        template.setApprovedBy(adminUserId);
        clearReviewFeedback(template);
        ChecklistTemplate saved = checklistTemplateRepository.save(template);
        UUID correlationId = UUID.randomUUID();
        writeDecisionAudit(saved, adminUserId, "MIGRATION_ACTIVATED", correlationId);
        return new ChecklistTemplateDecisionResponse(
                saved.getId(), previousStatus, saved.getStatus(), adminUserId, null, activatedAt);
    }

    @Override
    @Transactional
    public ChecklistTemplateDecisionResponse activateImportedInLineage(
            UUID lineageId, UUID id, Principal principal) {
        return activateImported(resolveEntityIdInLineage(lineageId, id), principal);
    }

    private UUID resolveEntityIdInLineage(UUID expectedLineageId, UUID versionId) {
        ChecklistTemplate template = checklistTemplateRepository.findByTemplateVersionId(versionId)
                .orElseThrow(ContentException::checklistTemplateNotFound);
        UUID actualLineageId = template.getTemplateLineageId() == null
                ? template.getId()
                : template.getTemplateLineageId();
        if (!expectedLineageId.equals(actualLineageId)) {
            throw ContentException.checklistTemplateNotFound();
        }
        return template.getId();
    }

    private void validateAuthoringContract(ChecklistTemplate template) {
        UUID versionId = template.getTemplateVersionId();
        if (versionId == null) {
            throw ContentException.checklistTemplateInvalidStatusTransition();
        }
        if (checklistItemRepository == null) {
            throw new IllegalStateException("Checklist authoring validation boundary is unavailable");
        }
        ChecklistRecipientScope scope = template.getRecipientScope();
        if (scope == null) {
            throw ContentException.templateRoleRequired();
        }
        if (template.getId() == null || checklistItemRepository
                .findByTemplate_IdOrderByOrder(template.getId()).stream()
                .anyMatch(item -> item.getTargetSubject() == null)) {
            throw ContentException.itemTargetRequired();
        }

        if (scope == ChecklistRecipientScope.FAMILY) {
            if (template.getStage() != null
                    || template.getEligibilityAnchorType() != null
                    || template.getEligibilityRangeUnit() != null
                    || template.getEligibilityStartInclusive() != null
                    || template.getEligibilityEndInclusive() != null) {
                throw ContentException.familyStageNotAllowed();
            }
            return;
        }

        ContentStage stage = template.getStage();
        ChecklistAnchorType anchor = template.getEligibilityAnchorType();
        ChecklistRangeUnit unit = template.getEligibilityRangeUnit();
        Integer start = template.getEligibilityStartInclusive();
        Integer end = template.getEligibilityEndInclusive();
        if (stage == null || anchor == null || unit == null || start == null || end == null
                || start < 0 || end < start || !isAnchorCompatible(stage, anchor)) {
            throw ContentException.substageStageMismatch();
        }
        if (stage == ContentStage.PRE_PREGNANCY
                && (anchor != ChecklistAnchorType.NONE
                    || unit != ChecklistRangeUnit.DAY
                    || start != 0 || end != 0)) {
            throw ContentException.substageStageMismatch();
        }
        if (template.getSequencePosition() != null && template.getSequencePosition() < 0) {
            throw ContentException.substageStageMismatch();
        }
        if (template.getSequencePosition() != null && template.getSequencePosition() > 1000) {
            throw ContentException.validationFailed("displayOrder", "must be at most 1000");
        }
        if (template.getSequencePosition() != null && template.getSequencePosition() > 0
                && (stage != ContentStage.PRE_PREGNANCY
                    || scope != ChecklistRecipientScope.MOTHER
                    || template.getTemplateType() != ChecklistTemplateType.MANDATORY)) {
            throw ContentException.substageStageMismatch();
        }
        if (isPositiveSequenceTemplate(template)) {
            boolean hasRequired = checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId()).stream()
                    .anyMatch(item -> Boolean.TRUE.equals(item.getIsRequired()));
            if (!hasRequired) {
                throw ContentException.validationFailed(
                        "items", "a sequence checklist must contain at least one required item");
            }
        }
    }

    /**
     * Approving a new version at an occupied position is a replacement, not a second
     * active candidate. Archive/disable the previous candidate before the new row is
     * saved so the partial unique index can enforce one active version per position.
     * The complete active chain is then validated (position 1 and no gaps).
     */
    private void prepareSequenceApproval(ChecklistTemplate template) {
        UUID lineageId = template.getTemplateLineageId();
        if (lineageId != null && checklistInstanceRepository.existsByTemplateLineageId(lineageId)) {
            List<ChecklistTemplate> lineageVersions = checklistTemplateRepository
                    .findByTemplateLineageId(lineageId);
            boolean conflictingPosition = lineageVersions.stream()
                    .filter(existing -> existing.getId() == null
                            || !existing.getId().equals(template.getId()))
                    .filter(existing -> existing.getSequencePosition() != null)
                    .anyMatch(existing -> !existing.getSequencePosition()
                            .equals(template.getSequencePosition()));
            if (conflictingPosition) {
                throw ContentException.validationFailed(
                        "displayOrder", "sequence position is immutable after the lineage has started");
            }
        }
        List<ChecklistTemplate> approved = checklistTemplateRepository
                .findAllDistributionEnabledByStageAndStatus(
                        ContentStage.PRE_PREGNANCY, ChecklistTemplateStatus.APPROVED);
        if (!isPositiveSequenceTemplate(template)) {
            return;
        }
        boolean activeLegacyCandidate = approved.stream()
                .filter(ChecklistTemplateApprovalServiceImpl::isMotherPreconceptionCandidate)
                .anyMatch(existing -> existing.getSequencePosition() == null
                        || existing.getSequencePosition() <= 0);
        if (activeLegacyCandidate) {
            throw ContentException.validationFailed(
                    "displayOrder", "archive or disable active legacy PRE_PREGNANCY candidates first");
        }
        for (ChecklistTemplate existing : approved) {
            if (existing.getId() != null && !existing.getId().equals(template.getId())
                    && existing.getSequencePosition() != null
                    && existing.getSequencePosition().equals(template.getSequencePosition())) {
                UUID existingLineage = existing.getTemplateLineageId();
                if (lineageId == null || existingLineage == null || !lineageId.equals(existingLineage)) {
                    throw ContentException.validationFailed("displayOrder", "duplicate sequence position");
                }
                existing.setStatus(ChecklistTemplateStatus.ARCHIVED);
                existing.setDistributionEnabled(false);
                checklistTemplateRepository.save(existing);
            }
        }

        List<Integer> positions = new ArrayList<>();
        for (ChecklistTemplate existing : approved) {
            if (existing.getId() != null && existing.getId().equals(template.getId())) {
                continue;
            }
            if (isPositiveSequenceTemplate(existing)
                    && existing.getStatus() == ChecklistTemplateStatus.APPROVED
                    && Boolean.TRUE.equals(existing.getDistributionEnabled())) {
                positions.add(existing.getSequencePosition());
            }
        }
        positions.add(template.getSequencePosition());
        Set<Integer> unique = new HashSet<>(positions);
        if (unique.size() != positions.size()) {
            throw ContentException.validationFailed("displayOrder", "duplicate sequence position");
        }
        int max = unique.stream().max(Comparator.naturalOrder()).orElse(0);
        for (int expected = 1; expected <= max; expected++) {
            if (!unique.contains(expected)) {
                throw ContentException.validationFailed("displayOrder", "sequence positions must be contiguous from 1");
            }
        }
    }

    private static boolean isPositiveSequenceTemplate(ChecklistTemplate template) {
        return template != null
                && template.getStage() == ContentStage.PRE_PREGNANCY
                && template.getTemplateType() == ChecklistTemplateType.MANDATORY
                && template.getRecipientScope() == ChecklistRecipientScope.MOTHER
                && template.getSequencePosition() != null
                && template.getSequencePosition() > 0;
    }

    private static boolean isMotherPreconceptionCandidate(ChecklistTemplate template) {
        return template != null
                && template.getStage() == ContentStage.PRE_PREGNANCY
                && template.getTemplateType() == ChecklistTemplateType.MANDATORY
                && Boolean.TRUE.equals(template.getDistributionEnabled())
                && (template.getRecipientScope() == ChecklistRecipientScope.MOTHER
                    || template.getRecipientScope() == ChecklistRecipientScope.BOTH);
    }

    private boolean isAnchorCompatible(ContentStage stage, ChecklistAnchorType anchor) {
        return switch (stage) {
            case PREGNANCY -> anchor == ChecklistAnchorType.LMP || anchor == ChecklistAnchorType.EDD;
            case POSTPARTUM -> anchor == ChecklistAnchorType.DELIVERY_DATE
                    || anchor == ChecklistAnchorType.BIRTH_DATE;
            case PRE_PREGNANCY -> anchor == ChecklistAnchorType.NONE;
        };
    }

    private boolean isMandatory(ChecklistTemplate template) {
        return template.getTemplateType() != ChecklistTemplateType.OPTIONAL;
    }

    private void clearReviewFeedback(ChecklistTemplate template) {
        template.setRevisionReason(null);
        template.setRevisionRequestedAt(null);
        template.setRevisionRequestedBy(null);
        template.setRevisionRequestedVersion(null);
    }

    private void writeDecisionAudit(
            ChecklistTemplate template,
            UUID actorUserId,
            String reasonCode,
            UUID correlationId) {
        UUID versionId = template.getTemplateVersionId() == null
                ? template.getId()
                : template.getTemplateVersionId();
        if (auditService == null) {
            throw new IllegalStateException("Required checklist approval audit boundary is unavailable");
        }
        auditService.write(new ChecklistAuditEvent(
                AuditAction.CHECKLIST_TEMPLATE_DECIDED,
                actorUserId,
                ChecklistAuditActorType.USER,
                null,
                ChecklistAuditResourceType.CHECKLIST_TEMPLATE_VERSION,
                versionId,
                null,
                null,
                null,
                versionId,
                null,
                null,
                null,
                reasonCode,
                correlationId));
    }
}
