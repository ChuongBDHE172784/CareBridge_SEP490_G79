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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    /** Stable reason codes consumed by the admin reviewer; HTTP CNT-001 remains unchanged. */
    public static final String ACTIVE_LEGACY_CONFLICT = "CHECKLIST_ACTIVE_LEGACY_CONFLICT";
    public static final String ACTIVE_SEQUENCE_CONFLICT = "CHECKLIST_ACTIVE_SEQUENCE_CONFLICT";
    public static final String REQUIRED_ITEM_MISSING = "CHECKLIST_REQUIRED_ITEM_MISSING";
    public static final String DUPLICATE_SEQUENCE_POSITION = "CHECKLIST_DUPLICATE_SEQUENCE_POSITION";
    public static final String SEQUENCE_POSITION_GAP = "CHECKLIST_SEQUENCE_POSITION_GAP";
    public static final String SEQUENCE_POSITION_IMMUTABLE = "CHECKLIST_SEQUENCE_POSITION_IMMUTABLE";

    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistAuditWriter auditService;
    private final ContentReviewNotificationService contentReviewNotificationService;
    private final ChecklistItemRepository checklistItemRepository;
    private final ChecklistInstanceRepository checklistInstanceRepository;
    private final ObjectMapper objectMapper;

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
            // A technically reviewed Pregnancy V2 import still requires the
            // explicit provenance/copy sign-off before any generic approve path
            // can attempt to persist APPROVED. Keep the stable provenance error
            // ahead of the database CHECK gate.
            requirePregnancyImportedProvenance(template);
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
            prepareSequenceApproval(template);
            validateAuthoringContract(template);
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
        if (template.getStatus() != ChecklistTemplateStatus.DRAFT
                && template.getStatus() != ChecklistTemplateStatus.PENDING_REVIEW) {
            throw ContentException.checklistTemplateNotPendingReview();
        }
        validateAuthoringContract(template);

        ChecklistTemplateStatus previousStatus = template.getStatus();
        template.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
        template.setMigrationReviewRequired(false);
        Instant reviewedAt = Instant.now();
        template.setMigrationReviewedAt(reviewedAt);
        template.setMigrationReviewedBy(adminUserId);
        template.setDistributionEnabled(false);
        template.setApprovedAt(null);
        template.setApprovedBy(null);
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

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
        requirePregnancyImportedProvenance(template);
        prepareSequenceApproval(template);
        validateAuthoringContract(template);

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
        Short contractVersion = normalizeContractVersion(template.getChecklistContractVersion());
        if (template.getId() == null) {
            throw ContentException.checklistTemplateInvalidStatusTransition();
        }
        for (var item : checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId())) {
            if (!java.util.Objects.equals(
                    normalizeContractVersion(item.getChecklistContractVersion()), contractVersion)) {
                throw ContentException.validationFailed(
                        "items", "leaf contract version must match checklistContractVersion");
            }
            if (contractVersion == 2) {
                if (item.getTargetSubject() != null) {
                    throw ContentException.itemTargetUnsupported();
                }
                if (item.getIsRequired() != null) {
                    throw ContentException.itemRequirednessUnsupported();
                }
            } else {
                if (item.getTargetSubject() == null) {
                    throw ContentException.itemTargetRequired();
                }
                if (item.getIsRequired() == null) {
                    throw ContentException.validationFailed(
                            "isRequired", "must be provided for contract version 1");
                }
            }
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
        if (contractVersion != 2 && isPositiveSequenceTemplate(template)) {
            boolean hasRequired = checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId()).stream()
                    .anyMatch(item -> Boolean.TRUE.equals(item.getIsRequired()));
            if (!hasRequired) {
                throw ContentException.checklistValidationFailed(
                        "items", "a sequence checklist must contain at least one required item",
                        REQUIRED_ITEM_MISSING);
            }
        }
    }

    private static Short normalizeContractVersion(Short requestedVersion) {
        short resolved = requestedVersion == null ? 1 : requestedVersion;
        if (resolved != 1 && resolved != 2) {
            throw ContentException.validationFailed(
                    "checklistContractVersion", "must be 1 or 2");
        }
        return resolved;
    }

    /**
     * Pregnancy V2 imports are recommendation copy, so technical migration
     * review is intentionally not treated as clinical/copy sign-off.  Keep the
     * activation boundary fail-closed when metadata is absent, malformed, or
     * still carries the seed's pending provenance status.
     */
    private void requirePregnancyImportedProvenance(ChecklistTemplate template) {
        if (template.getStage() != ContentStage.PREGNANCY
                || normalizeContractVersion(template.getChecklistContractVersion()) != 2) {
            return;
        }
        String metadataJson = template.getChecklistMetadataJson();
        if (metadataJson == null || metadataJson.isBlank()) {
            throw ContentException.checklistProvenanceSignOffRequired();
        }
        try {
            ObjectMapper mapper = objectMapper == null ? new ObjectMapper() : objectMapper;
            JsonNode metadata = mapper.readTree(metadataJson);
            if (metadata == null || !metadata.isObject()
                    || !isText(metadata.get("schema"), "CHECKLIST_METADATA_V1")
                    || !isText(metadata.get("provenanceStatus"), "SIGNED_OFF")) {
                throw ContentException.checklistProvenanceSignOffRequired();
            }
        } catch (ContentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ContentException.checklistProvenanceSignOffRequired();
        }
    }

    private static boolean isText(JsonNode node, String expected) {
        return node != null && node.isTextual() && expected.equals(node.asText());
    }

    /**
     * Approving a new version at an occupied position is a replacement, not a second
     * active candidate. All cohort and chain checks complete before any existing row is
     * archived/disabled, preserving the fail-closed approval boundary. The previous
     * candidate is then archived as part of the same transaction so the partial unique
     * index can enforce one active version per position.
     */
    private void prepareSequenceApproval(ChecklistTemplate template) {
        UUID lineageId = template.getTemplateLineageId();
        boolean positiveSequence = isPositiveSequenceTemplate(template);
        boolean legacyPreconception = isLegacyPreconceptionTemplate(template);

        // Legacy and positive candidates are one mutually-exclusive cohort. Lock
        // before reading active rows and retain the transaction-scoped lock through
        // the eventual save so concurrent approvals cannot both pass the guards.
        if (positiveSequence || legacyPreconception) {
            checklistTemplateRepository.acquirePreconceptionSequenceCohortLock();
        }

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
                throw ContentException.checklistValidationFailed(
                        "displayOrder", "sequence position is immutable after the lineage has started",
                        SEQUENCE_POSITION_IMMUTABLE);
            }
        }
        List<ChecklistTemplate> approved = checklistTemplateRepository
                .findAllDistributionEnabledByStageAndStatus(
                        ContentStage.PRE_PREGNANCY, ChecklistTemplateStatus.APPROVED);

        boolean activeLegacyCandidate = approved.stream()
                .filter(ChecklistTemplateApprovalServiceImpl::isMotherPreconceptionCandidate)
                .anyMatch(ChecklistTemplateApprovalServiceImpl::isLegacyPreconceptionTemplate);
        boolean activeSequenceCandidate = approved.stream()
                .anyMatch(ChecklistTemplateApprovalServiceImpl::isPositiveSequenceTemplate);

        // The two cohorts are mutually exclusive in both approval directions. Keep this
        // guard ahead of all status/distribution/audit/save operations.
        if (positiveSequence && activeLegacyCandidate) {
            throw ContentException.checklistValidationFailed(
                    "displayOrder", "archive or disable active legacy PRE_PREGNANCY candidates first",
                    ACTIVE_LEGACY_CONFLICT);
        }
        if (legacyPreconception && activeSequenceCandidate) {
            throw ContentException.checklistValidationFailed(
                    "displayOrder", "archive or disable active sequence PRE_PREGNANCY candidates first",
                    ACTIVE_SEQUENCE_CONFLICT);
        }
        if (!positiveSequence) {
            return;
        }

        List<ChecklistTemplate> replacements = new ArrayList<>();
        for (ChecklistTemplate existing : approved) {
            if (existing.getId() != null && !existing.getId().equals(template.getId())
                    && existing.getSequencePosition() != null
                    && existing.getSequencePosition().equals(template.getSequencePosition())) {
                UUID existingLineage = existing.getTemplateLineageId();
                if (lineageId == null || existingLineage == null || !lineageId.equals(existingLineage)) {
                    throw ContentException.checklistValidationFailed(
                            "displayOrder", "duplicate sequence position", DUPLICATE_SEQUENCE_POSITION);
                }
                replacements.add(existing);
            }
        }

        List<Integer> positions = new ArrayList<>();
        for (ChecklistTemplate existing : approved) {
            if ((existing.getId() != null && existing.getId().equals(template.getId()))
                    || replacements.contains(existing)) {
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
            throw ContentException.checklistValidationFailed(
                    "displayOrder", "duplicate sequence position", DUPLICATE_SEQUENCE_POSITION);
        }
        int max = unique.stream().max(Comparator.naturalOrder()).orElse(0);
        for (int expected = 1; expected <= max; expected++) {
            if (!unique.contains(expected)) {
                throw ContentException.checklistValidationFailed(
                        "displayOrder", "sequence positions must be contiguous from 1", SEQUENCE_POSITION_GAP);
            }
        }

        // Mutation is intentionally last: every validation above must pass first.
        for (ChecklistTemplate replacement : replacements) {
            replacement.setStatus(ChecklistTemplateStatus.ARCHIVED);
            replacement.setDistributionEnabled(false);
            checklistTemplateRepository.save(replacement);
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

    private static boolean isLegacyPreconceptionTemplate(ChecklistTemplate template) {
        return template != null
                && template.getStage() == ContentStage.PRE_PREGNANCY
                && template.getTemplateType() == ChecklistTemplateType.MANDATORY
                && (template.getRecipientScope() == ChecklistRecipientScope.MOTHER
                    || template.getRecipientScope() == ChecklistRecipientScope.BOTH)
                && (template.getSequencePosition() == null || template.getSequencePosition() <= 0);
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
