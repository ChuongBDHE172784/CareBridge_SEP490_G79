package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import com.carebridge.backend.checklist.model.ChecklistScheduleEndMode;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.checklist.model.ChecklistWeekBoundaryRule;
import com.carebridge.backend.content.dto.request.ChecklistItemRequest;
import com.carebridge.backend.content.dto.request.ChecklistSubstageRequest;
import com.carebridge.backend.content.dto.request.CreateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.HideChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.UpdateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.dto.response.HideChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ChecklistTemplateVersionSnapshotResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// UC-243 (CB-CONTENT-IMP-011)
@Service
@RequiredArgsConstructor
public class AdminChecklistTemplateServiceImpl implements AdminChecklistTemplateService {

    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ContentMapper contentMapper;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final ContentWorkloadDispatcherService contentWorkloadDispatcherService;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminChecklistTemplateDetailResponse> list(
            ChecklistTemplateStatus status, ContentStage stage, String keyword, Pageable pageable) {
        Page<ChecklistTemplate> templates = checklistTemplateRepository
                .findAdminByOptionalStageAndStatus(stage, status,
                        keyword == null || keyword.isBlank() ? null : keyword.trim(), pageable);
        return templates.map(this::toResponseWithItems);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminChecklistTemplateDetailResponse getById(UUID id) {
        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);
        return toResponseWithItems(template);
    }

    @Override
    @Transactional
    public AdminChecklistTemplateDetailResponse create(
            CreateChecklistTemplateRequest request, UUID adminUserId) {
        Set<ChecklistRecipientRole> recipientRoles = requireRecipientRoles(request.recipientRoles());
        ContentStage normalizedStage = normalizeStage(recipientRoles, request.stage());
        InlineEligibility eligibility = resolveEligibility(recipientRoles, normalizedStage, request.substage());
        Short contractVersion = normalizeContractVersion(request.checklistContractVersion());
        validateItems(request.items(), contractVersion);
        validateCadence(request.scheduleType(), request.materializationPolicy(), request.items());
        UUID lineageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        ChecklistTemplate template = ChecklistTemplate.builder()
                .name(request.name())
                .description(request.description())
                .templateLineageId(lineageId)
                .templateVersionId(versionId)
                .stage(normalizedStage)
                .sequencePosition(normalizeSequencePosition(request.displayOrder()))
                .recipientScope(toRecipientScope(recipientRoles))
                .eligibilityAnchorType(eligibility.anchor())
                .eligibilityRangeUnit(eligibility.unit())
                .eligibilityStartInclusive(eligibility.startInclusive())
                .eligibilityEndInclusive(eligibility.endInclusive())
                .status(ChecklistTemplateStatus.DRAFT)
                .migrationReviewRequired(false)
                .distributionEnabled(false)
                .templateType(normalizeTemplateType(request.templateType()))
                .checklistContractVersion(contractVersion)
                .scheduleType(request.scheduleType())
                .materializationPolicy(request.materializationPolicy())
                .scheduleGroupKey(request.scheduleGroupKey())
                .scheduleContextType(canonicalScheduleContextType(normalizedStage, request.scheduleContextType()))
                .scheduleEndMode(request.scheduleEndMode())
                .weekBoundaryRule(request.weekBoundaryRule())
                .authorUserId(adminUserId)
                .build();
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        List<ChecklistItemRequest> requestedItems = request.items() == null ? List.of() : request.items();
        List<ChecklistItem> savedItems = requestedItems.isEmpty()
                ? List.of()
                : checklistItemRepository.saveAll(toEntities(requestedItems, saved));

        auditService.log(AuditAction.CHECKLIST_TEMPLATE_CREATED, adminUserId,
                "ChecklistTemplate", saved.getId().toString(), snapshotOf(saved, savedItems.size()));

        return contentMapper.toAdminChecklistTemplateDetailResponse(saved, savedItems);
    }

    @Override
    @Transactional
    public AdminChecklistTemplateDetailResponse update(
            UUID id, UpdateChecklistTemplateRequest request, UUID adminUserId) {
        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);

        if (template.getStatus() == ChecklistTemplateStatus.APPROVED
                || template.getStatus() == ChecklistTemplateStatus.ARCHIVED) {
            throw ContentException.versionImmutable();
        }

        // Same separation-of-duties guard as ContentItem.updateContent (BR-CNT-006): a
        // Content Admin
        // may only work a draft or submit it for review — publication is a System Admin
        // decision
        // made exclusively through ChecklistTemplateApprovalService.decide().
        if (template.getStatus() != ChecklistTemplateStatus.DRAFT
                && template.getStatus() != ChecklistTemplateStatus.PENDING_REVIEW) {
            throw ContentException.checklistTemplateInvalidStatusTransition();
        }
        if (request.status() != ChecklistTemplateStatus.DRAFT
                && request.status() != ChecklistTemplateStatus.PENDING_REVIEW) {
            throw ContentException.checklistTemplateInvalidStatusTransition();
        }
        boolean reviewedImportedVersion = template.getMigrationReviewedAt() != null;

        Set<ChecklistRecipientRole> recipientRoles = requireRecipientRoles(request.recipientRoles());
        ContentStage normalizedStage = normalizeStage(recipientRoles, request.stage());
        InlineEligibility eligibility = resolveEligibility(recipientRoles, normalizedStage, request.substage());
        Short contractVersion = request.checklistContractVersion() == null
                ? normalizeContractVersion(template.getChecklistContractVersion())
                : normalizeContractVersion(request.checklistContractVersion());
        ChecklistScheduleType effectiveScheduleType = request.scheduleType() == null
                && request.materializationPolicy() == null
                        ? template.getScheduleType()
                        : request.scheduleType();
        ChecklistMaterializationPolicy effectiveMaterializationPolicy = request.scheduleType() == null
                && request.materializationPolicy() == null
                        ? template.getMaterializationPolicy()
                        : request.materializationPolicy();
        String effectiveScheduleGroupKey = request.scheduleGroupKey() == null
                ? template.getScheduleGroupKey()
                : request.scheduleGroupKey();
        ChecklistCareContextType effectiveScheduleContextType = request.scheduleContextType() == null
                ? template.getScheduleContextType()
                : request.scheduleContextType();
        effectiveScheduleContextType = canonicalScheduleContextType(
                normalizedStage, effectiveScheduleContextType);
        ChecklistScheduleEndMode effectiveScheduleEndMode = request.scheduleEndMode() == null
                ? template.getScheduleEndMode()
                : request.scheduleEndMode();
        ChecklistWeekBoundaryRule effectiveWeekBoundaryRule = request.weekBoundaryRule() == null
                ? template.getWeekBoundaryRule()
                : request.weekBoundaryRule();
        validateItems(request.items(), contractVersion);
        validateCadence(effectiveScheduleType, effectiveMaterializationPolicy, request.items());
        Short previousContractVersion = normalizeContractVersion(template.getChecklistContractVersion());
        if (request.items() == null && !previousContractVersion.equals(contractVersion)) {
            // A root contract change cannot leave the previous leaf shape in
            // place. Require the caller to submit the complete replacement set
            // so target/requiredness is validated and rewritten atomically.
            throw ContentException.validationFailed(
                    "items", "must be provided when checklistContractVersion changes");
        }

        if (template.getTemplateLineageId() == null) {
            template.setTemplateLineageId(template.getId());
        }
        if (template.getTemplateVersionId() == null) {
            template.setTemplateVersionId(template.getId());
        }

        template.setName(request.name());
        template.setDescription(request.description());
        template.setTemplateType(request.templateType() == null
                ? normalizeTemplateType(template.getTemplateType())
                : request.templateType());
        template.setChecklistContractVersion(contractVersion);
        template.setScheduleType(effectiveScheduleType);
        template.setMaterializationPolicy(effectiveMaterializationPolicy);
        template.setScheduleGroupKey(effectiveScheduleGroupKey);
        template.setScheduleContextType(effectiveScheduleContextType);
        template.setScheduleEndMode(effectiveScheduleEndMode);
        template.setWeekBoundaryRule(effectiveWeekBoundaryRule);
        template.setStage(normalizedStage);
        if (request.displayOrder() != null
                && template.getStatus() != ChecklistTemplateStatus.APPROVED
                && template.getStatus() != ChecklistTemplateStatus.ARCHIVED) {
            template.setSequencePosition(normalizeSequencePosition(request.displayOrder()));
        }
        template.setSubstageId(null);
        template.setRecipientScope(toRecipientScope(recipientRoles));
        template.setEligibilityAnchorType(eligibility.anchor());
        template.setEligibilityRangeUnit(eligibility.unit());
        template.setEligibilityStartInclusive(eligibility.startInclusive());
        template.setEligibilityEndInclusive(eligibility.endInclusive());
        template.setStatus(request.status());
        if (reviewedImportedVersion) {
            template.setMigrationReviewRequired(true);
            template.setMigrationReviewedAt(null);
            template.setMigrationReviewedBy(null);
            template.setDistributionEnabled(false);
            template.setApprovedAt(null);
            template.setApprovedBy(null);
        }
        if (request.status() == ChecklistTemplateStatus.PENDING_REVIEW) {
            UUID previousReviewer = template.getRevisionRequestedBy();
            clearReviewFeedback(template);
            if (template.getAssignedExpertId() == null || previousReviewer != null) {
                UUID assignedExpertId = contentWorkloadDispatcherService.dispatchToOptimalExpert(
                        ContentType.CHECKLIST, normalizedStage, previousReviewer);
                template.setAssignedExpertId(assignedExpertId);
                template.setAssignedAt(Instant.now());
            }
        }
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        // null leaves entries untouched; a non-null list is the complete active set.
        // Existing
        // rows are reconciled in place so imported personal checklist foreign keys
        // remain valid.
        List<ChecklistItem> currentItems;
        if (request.items() != null) {
            currentItems = reconcileItems(request.items(), saved);
        } else {
            currentItems = checklistItemRepository.findByTemplate_IdOrderByOrder(id);
            normalizeExistingItems(currentItems, saved);
        }

        auditService.log(AuditAction.CHECKLIST_TEMPLATE_UPDATED, adminUserId,
                "ChecklistTemplate", saved.getId().toString(), snapshotOf(saved, currentItems.size()));

        return contentMapper.toAdminChecklistTemplateDetailResponse(saved, currentItems);
    }

    @Override
    @Transactional
    public HideChecklistTemplateResponse archive(UUID id, HideChecklistTemplateRequest request, UUID adminUserId) {
        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);

        // ADR-CHK-002: idempotency guard — already ARCHIVED is rejected, not silently
        // re-applied
        if (template.getStatus() == ChecklistTemplateStatus.ARCHIVED) {
            throw ContentException.checklistTemplateAlreadyArchived();
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw ContentException.checklistTemplateArchiveReasonRequired();
        }

        ChecklistTemplateStatus previousStatus = template.getStatus();
        // ADR-CHK-002: soft-delete only. Canonical template entries and imported
        // personal
        // preparation_checklist_items remain stable when a template is archived.
        template.setStatus(ChecklistTemplateStatus.ARCHIVED);
        template.setDistributionEnabled(false);
        clearReviewFeedback(template);
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        Instant archivedAt = Instant.now();
        auditService.log(AuditAction.CHECKLIST_TEMPLATE_ARCHIVED, adminUserId,
                "ChecklistTemplate", saved.getId().toString(),
                "reason=" + request.reason() + " previousStatus=" + previousStatus);

        return new HideChecklistTemplateResponse(
                saved.getId(), previousStatus, saved.getStatus(), request.reason(), adminUserId, archivedAt);
    }

    private void clearReviewFeedback(ChecklistTemplate template) {
        template.setRevisionReason(null);
        template.setRevisionRequestedAt(null);
        template.setRevisionRequestedBy(null);
        template.setRevisionRequestedVersion(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistTemplateVersionSnapshotResponse> getVersionHistory(UUID id) {
        if (!checklistTemplateRepository.existsById(id)) {
            throw ContentException.checklistTemplateNotFound();
        }
        return auditLogRepository.findByEntityIdAndEntityTypeAndActionInOrderByCreatedAtDesc(
                id, "ChecklistTemplate",
                Set.of(AuditAction.CHECKLIST_TEMPLATE_CREATED, AuditAction.CHECKLIST_TEMPLATE_UPDATED)).stream()
                .map(this::toVersionResponse)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    @Override
    @Transactional
    public AdminChecklistTemplateDetailResponse cloneVersion(UUID id, UUID adminUserId) {
        return cloneVersionInternal(null, id, adminUserId);
    }

    @Override
    @Transactional
    public AdminChecklistTemplateDetailResponse cloneVersionInLineage(
            UUID lineageId, UUID id, UUID adminUserId) {
        return cloneVersionInternal(lineageId, id, adminUserId);
    }

    private AdminChecklistTemplateDetailResponse cloneVersionInternal(
            UUID expectedLineageId, UUID id, UUID adminUserId) {
        ChecklistTemplate source = (expectedLineageId == null
                ? checklistTemplateRepository.findById(id)
                : checklistTemplateRepository.findByTemplateVersionId(id))
                .orElseThrow(ContentException::checklistTemplateNotFound);
        if (source.getStatus() != ChecklistTemplateStatus.APPROVED) {
            throw ContentException.checklistTemplateInvalidStatusTransition();
        }

        UUID lineageId = source.getTemplateLineageId() == null ? source.getId() : source.getTemplateLineageId();
        if (expectedLineageId != null && !expectedLineageId.equals(lineageId)) {
            throw ContentException.checklistTemplateNotFound();
        }
        checklistTemplateRepository.acquireLineageLock(lineageId);
        UUID versionId = UUID.randomUUID();
        int sourceVersionNo = source.getVersionNo() == null ? 1 : source.getVersionNo();
        int lineageMaxVersionNo = checklistTemplateRepository.findMaxVersionNoForLineage(lineageId);
        int nextVersionNo = Math.max(sourceVersionNo, lineageMaxVersionNo) + 1;
        ChecklistTemplate clone = ChecklistTemplate.builder()
                .name(source.getName())
                .description(source.getDescription())
                .templateLineageId(lineageId)
                .templateVersionId(versionId)
                .stage(source.getStage())
                .sequencePosition(source.getSequencePosition())
                .recipientScope(source.getRecipientScope())
                .substageId(source.getSubstageId())
                .eligibilityAnchorType(source.getEligibilityAnchorType())
                .eligibilityRangeUnit(source.getEligibilityRangeUnit())
                .eligibilityStartInclusive(source.getEligibilityStartInclusive())
                .eligibilityEndInclusive(source.getEligibilityEndInclusive())
                .scheduleType(source.getScheduleType())
                .materializationPolicy(source.getMaterializationPolicy())
                .scheduleGroupKey(source.getScheduleGroupKey())
                .scheduleContextType(source.getScheduleContextType())
                .scheduleEndMode(source.getScheduleEndMode())
                .weekBoundaryRule(source.getWeekBoundaryRule())
                .status(ChecklistTemplateStatus.DRAFT)
                .migrationReviewRequired(false)
                .distributionEnabled(false)
                .templateType(source.getTemplateType())
                .checklistContractVersion(normalizeContractVersion(source.getChecklistContractVersion()))
                // Preserve the source's canonical cadence/provenance contract. A
                // cloned Pregnancy V2 draft must remain activatable after review;
                // dropping this metadata would make the DB activation gate fail
                // permanently because authoring requests deliberately do not expose
                // internal provenance fields.
                .checklistMetadataJson(source.getChecklistMetadataJson())
                .checklistMetadataHash(source.getChecklistMetadataHash())
                .checklistQuarantineReasonCode(source.getChecklistQuarantineReasonCode())
                .versionNo(nextVersionNo)
                .authorUserId(adminUserId)
                .build();
        ChecklistTemplate saved = checklistTemplateRepository.save(clone);

        List<ChecklistItem> sourceItems = checklistItemRepository.findByTemplate_IdOrderByOrder(source.getId());
        List<ChecklistItem> clonedItems = sourceItems.stream()
                .map(item -> {
                    validateItem(item.getTargetSubject(), item.getIsRequired(),
                            normalizeContractVersion(source.getChecklistContractVersion()));
                    return ChecklistItem.builder()
                            .template(saved)
                            .itemText(item.getItemText())
                            .description(item.getDescription())
                            .supportFunction(item.getSupportFunction())
                            .order(item.getOrder())
                            .isRequired(item.getIsRequired())
                            .checklistContractVersion(normalizeContractVersion(source.getChecklistContractVersion()))
                            .targetSubject(item.getTargetSubject())
                            .configurationJson(item.getConfigurationJson())
                            .isActive(true)
                            .build();
                })
                .toList();
        if (!clonedItems.isEmpty()) {
            checklistItemRepository.saveAll(clonedItems);
        }

        auditService.log(AuditAction.CHECKLIST_TEMPLATE_CREATED, adminUserId,
                "ChecklistTemplate", saved.getId().toString(), snapshotOf(saved, clonedItems.size()));
        return contentMapper.toAdminChecklistTemplateDetailResponse(saved, clonedItems);
    }

    private ChecklistTemplateVersionSnapshotResponse snapshotOf(ChecklistTemplate template, int itemCount) {
        return new ChecklistTemplateVersionSnapshotResponse(template.getVersionNo(), template.getName(),
                template.getStage() == null ? null : template.getStage().name(), template.getStatus().name(),
                itemCount, null, Instant.now());
    }

    private java.util.Optional<ChecklistTemplateVersionSnapshotResponse> toVersionResponse(AuditLog auditLog) {
        try {
            ChecklistTemplateVersionSnapshotResponse snapshot = objectMapper.readValue(
                    auditLog.getNewValueJson(), ChecklistTemplateVersionSnapshotResponse.class);
            return java.util.Optional
                    .of(new ChecklistTemplateVersionSnapshotResponse(snapshot.versionNo(), snapshot.name(),
                            snapshot.stage(), snapshot.status(), snapshot.itemCount(), auditLog.getActorUserId(),
                            auditLog.getCreatedAt()));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private AdminChecklistTemplateDetailResponse toResponseWithItems(ChecklistTemplate template) {
        List<ChecklistItem> items = checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId());
        return contentMapper.toAdminChecklistTemplateDetailResponse(
                template, items);
    }

    private List<ChecklistItem> toEntities(List<ChecklistItemRequest> items, ChecklistTemplate template) {
        List<ChecklistItem> entities = new ArrayList<>();
        for (ChecklistItemRequest item : items) {
            entities.add(ChecklistItem.builder()
                    .template(template)
                    .itemText(item.itemText())
                    .description(item.description())
                    .supportFunction(item.supportFunction())
                    .order(item.order())
                    .isRequired(item.isRequired())
                    .checklistContractVersion(normalizeContractVersion(template.getChecklistContractVersion()))
                    .targetSubject(resolveTargetSubject(item, template))
                    .configurationJson(itemConfiguration(item))
                    .isActive(true)
                    .build());
        }
        return entities;
    }

    private List<ChecklistItem> reconcileItems(
            List<ChecklistItemRequest> requestedItems, ChecklistTemplate template) {
        List<ChecklistItem> existingItems = checklistItemRepository.findAllByTemplateIdOrderByOrder(template.getId());
        Map<UUID, ChecklistItem> existingById = new HashMap<>();
        for (ChecklistItem existing : existingItems) {
            existingById.put(existing.getId(), existing);
        }

        Set<UUID> requestedIds = new HashSet<>();
        for (ChecklistItemRequest requested : requestedItems) {
            if (requested.id() != null
                    && (!requestedIds.add(requested.id()) || !existingById.containsKey(requested.id()))) {
                throw ContentException.checklistTemplateItemReferenceInvalid();
            }
        }

        existingItems.forEach(item -> item.setIsActive(false));
        List<ChecklistItem> activeItems = new ArrayList<>();
        List<ChecklistItem> itemsToSave = new ArrayList<>(existingItems);
        for (ChecklistItemRequest requested : requestedItems) {
            ChecklistItem item;
            if (requested.id() == null) {
                item = ChecklistItem.builder().template(template).build();
                itemsToSave.add(item);
            } else {
                item = existingById.get(requested.id());
            }
            item.setItemText(requested.itemText());
            item.setDescription(requested.description());
            item.setSupportFunction(requested.supportFunction());
            item.setOrder(requested.order());
            item.setIsRequired(requested.isRequired());
            item.setChecklistContractVersion(normalizeContractVersion(template.getChecklistContractVersion()));
            item.setTargetSubject(resolveTargetSubject(requested, template));
            item.setConfigurationJson(itemConfiguration(requested));
            item.setIsActive(true);
            activeItems.add(item);
        }
        if (!itemsToSave.isEmpty()) {
            checklistItemRepository.saveAll(itemsToSave);
        }
        return activeItems;
    }

    private ChecklistTargetSubject resolveTargetSubject(
            ChecklistItemRequest item, ChecklistTemplate template) {
        Short contractVersion = normalizeContractVersion(template.getChecklistContractVersion());
        validateItem(item.targetSubject(), item.isRequired(), contractVersion);
        return contractVersion == 2 ? null : item.targetSubject();
    }

    private void normalizeExistingItems(List<ChecklistItem> items, ChecklistTemplate template) {
        Short contractVersion = normalizeContractVersion(template.getChecklistContractVersion());
        boolean changed = false;
        for (ChecklistItem item : items) {
            validateItem(item.getTargetSubject(), item.getIsRequired(), contractVersion);
            if (!contractVersion.equals(normalizeContractVersion(item.getChecklistContractVersion()))) {
                item.setChecklistContractVersion(contractVersion);
                changed = true;
            }
        }
        if (changed) {
            checklistItemRepository.saveAll(items);
        }
    }

    private Set<ChecklistRecipientRole> requireRecipientRoles(Set<ChecklistRecipientRole> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()
                || requestedRoles.stream().anyMatch(java.util.Objects::isNull)) {
            throw ContentException.templateRoleRequired();
        }
        return new LinkedHashSet<>(requestedRoles);
    }

    private ContentStage normalizeStage(Set<ChecklistRecipientRole> roles, ContentStage requestedStage) {
        boolean familyOnly = roles.size() == 1 && roles.contains(ChecklistRecipientRole.FAMILY);
        return familyOnly ? null : requestedStage;
    }

    private InlineEligibility resolveEligibility(
            Set<ChecklistRecipientRole> roles,
            ContentStage stage,
            ChecklistSubstageRequest requestedSubstage) {
        boolean familyOnly = roles.size() == 1 && roles.contains(ChecklistRecipientRole.FAMILY);
        if (familyOnly) {
            return InlineEligibility.none();
        }
        if (stage == ContentStage.PRE_PREGNANCY) {
            if (requestedSubstage != null
                    && requestedSubstage.anchor() != null
                    && requestedSubstage.anchor() != ChecklistAnchorType.NONE) {
                throw ContentException.substageStageMismatch();
            }
            return new InlineEligibility(ChecklistAnchorType.NONE, ChecklistRangeUnit.DAY, 0, 0);
        }
        if (stage == null || requestedSubstage == null
                || requestedSubstage.code() == null || requestedSubstage.code().isBlank()
                || requestedSubstage.anchor() == null || requestedSubstage.unit() == null
                || requestedSubstage.startInclusive() == null || requestedSubstage.endInclusive() == null
                || requestedSubstage.startInclusive() < 0
                || requestedSubstage.endInclusive() < requestedSubstage.startInclusive()
                || !isAnchorCompatible(stage, requestedSubstage.anchor())) {
            throw ContentException.substageStageMismatch();
        }
        return new InlineEligibility(
                requestedSubstage.anchor(),
                requestedSubstage.unit(),
                requestedSubstage.startInclusive(),
                requestedSubstage.endInclusive());
    }

    private void validateItems(List<ChecklistItemRequest> items, Short contractVersion) {
        if (items == null) {
            return;
        }
        for (ChecklistItemRequest item : items) {
            validateItem(item.targetSubject(), item.isRequired(), contractVersion);
            validateSourceUrl(item.sourceUrl());
        }
    }

    private void validateSourceUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return;
        }
        String normalized = sourceUrl.trim();
        if (normalized.length() > 2048) {
            throw ContentException.validationFailed("items.sourceUrl",
                    "Link nguồn không được vượt quá 2.048 ký tự");
        }
        try {
            URI uri = URI.create(normalized);
            boolean validScheme = "http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme());
            if (!validScheme || uri.getHost() == null || uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException("Unsupported source URL");
            }
        } catch (IllegalArgumentException exception) {
            throw ContentException.validationFailed("items.sourceUrl",
                    "Link nguồn phải là URL HTTP hoặc HTTPS hợp lệ");
        }
    }

    private void validateCadence(
            ChecklistScheduleType scheduleType,
            ChecklistMaterializationPolicy policy,
            List<ChecklistItemRequest> items) {
        boolean weekly = items != null && items.stream().anyMatch(item -> Boolean.TRUE.equals(item.repeatWeekly()));
        boolean daily = items != null && items.stream().anyMatch(item -> Boolean.TRUE.equals(item.repeatDaily()));
        boolean unmarked = items != null && items.stream().anyMatch(
                item -> !Boolean.TRUE.equals(item.repeatWeekly()) && !Boolean.TRUE.equals(item.repeatDaily()));
        if (weekly && daily) {
            throw ContentException.validationFailed("items",
                    "Từng tuần và Từng ngày không được trộn trong cùng checklist");
        }
        if ((weekly || daily) && unmarked) {
            throw ContentException.validationFailed("items", "Các mục có nội dung phải dùng cùng một nhịp lặp");
        }
        if (scheduleType == null && policy == null) {
            return;
        }
        if (scheduleType == null || policy == null) {
            throw ContentException.validationFailed("scheduleType",
                    "scheduleType và materializationPolicy phải đi cùng nhau");
        }
        boolean validPair = switch (scheduleType) {
            case LEGACY -> policy == ChecklistMaterializationPolicy.LEGACY_WINDOW;
            case SET -> policy == ChecklistMaterializationPolicy.SEQUENCE_STEP;
            case WEEKLY -> policy == ChecklistMaterializationPolicy.ONCE_PER_WINDOW
                    || policy == ChecklistMaterializationPolicy.EACH_WEEK;
            case DAILY -> policy == ChecklistMaterializationPolicy.EACH_DAY;
        };
        if (!validPair) {
            throw ContentException.validationFailed("materializationPolicy", "cadence pair không hợp lệ");
        }
        if (weekly && !(scheduleType == ChecklistScheduleType.WEEKLY
                && policy == ChecklistMaterializationPolicy.EACH_WEEK)) {
            throw ContentException.validationFailed("scheduleType", "Mục Từng tuần cần cadence WEEKLY/EACH_WEEK");
        }
        if (daily && !(scheduleType == ChecklistScheduleType.DAILY
                && policy == ChecklistMaterializationPolicy.EACH_DAY)) {
            throw ContentException.validationFailed("scheduleType", "Mục Từng ngày cần cadence DAILY/EACH_DAY");
        }
    }

    private String itemConfiguration(ChecklistItemRequest item) {
        String sourceUrl = item.sourceUrl() == null || item.sourceUrl().isBlank()
                ? null : item.sourceUrl().trim();
        if (!Boolean.TRUE.equals(item.repeatWeekly())
                && !Boolean.TRUE.equals(item.repeatDaily())
                && sourceUrl == null) {
            return "{}";
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.put("repeatWeekly", Boolean.TRUE.equals(item.repeatWeekly()));
        node.put("repeatDaily", Boolean.TRUE.equals(item.repeatDaily()));
        if (sourceUrl != null) {
            node.put("sourceUrl", sourceUrl);
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private void validateItem(
            ChecklistTargetSubject targetSubject,
            Boolean required,
            Short contractVersion) {
        if (contractVersion == 2) {
            if (targetSubject != null) {
                throw ContentException.itemTargetUnsupported();
            }
            if (required == null) {
                throw ContentException.itemRequirednessRequired();
            }
            return;
        }
        if (targetSubject == null) {
            throw ContentException.itemTargetRequired();
        }
        if (required == null) {
            throw ContentException.validationFailed("isRequired", "must be provided for contract version 1");
        }
    }

    private Short normalizeContractVersion(Short requestedVersion) {
        short resolved = requestedVersion == null ? 1 : requestedVersion;
        if (resolved != 1 && resolved != 2) {
            throw ContentException.validationFailed(
                    "checklistContractVersion", "must be 1 or 2");
        }
        return resolved;
    }

    private ChecklistTemplateType normalizeTemplateType(ChecklistTemplateType requestedType) {
        return requestedType == null ? ChecklistTemplateType.MANDATORY : requestedType;
    }

    private ChecklistRecipientScope toRecipientScope(Set<ChecklistRecipientRole> roles) {
        if (roles.contains(ChecklistRecipientRole.MOTHER)
                && roles.contains(ChecklistRecipientRole.FAMILY)) {
            return ChecklistRecipientScope.BOTH;
        }
        return roles.contains(ChecklistRecipientRole.FAMILY)
                ? ChecklistRecipientScope.FAMILY
                : ChecklistRecipientScope.MOTHER;
    }

    private boolean isAnchorCompatible(ContentStage stage, ChecklistAnchorType anchor) {
        return switch (stage) {
            case PREGNANCY -> anchor == ChecklistAnchorType.LMP || anchor == ChecklistAnchorType.EDD;
            case POSTPARTUM -> anchor == ChecklistAnchorType.DELIVERY_DATE;
            case BABY_CARE -> anchor == ChecklistAnchorType.BIRTH_DATE;
            case PRE_PREGNANCY -> anchor == ChecklistAnchorType.NONE;
        };
    }

    private ChecklistCareContextType canonicalScheduleContextType(
            ContentStage stage, ChecklistCareContextType requested) {
        if (stage == null || stage == ContentStage.PRE_PREGNANCY) {
            return requested;
        }
        ChecklistCareContextType expected = stage == ContentStage.BABY_CARE
                ? ChecklistCareContextType.BABY : ChecklistCareContextType.JOURNEY;
        if (requested != null && requested != expected) {
            throw ContentException.substageStageMismatch();
        }
        return expected;
    }

    private record InlineEligibility(
            ChecklistAnchorType anchor,
            ChecklistRangeUnit unit,
            Integer startInclusive,
            Integer endInclusive) {
        private static InlineEligibility none() {
            return new InlineEligibility(null, null, null, null);
        }
    }

    private static int normalizeSequencePosition(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw ContentException.validationFailed("displayOrder", "must be zero or a positive sequence position");
        }
        return value;
    }
}
