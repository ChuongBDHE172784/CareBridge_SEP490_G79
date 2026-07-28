package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.content.dto.request.ChecklistItemRequest;
import com.carebridge.backend.content.dto.request.CreateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.HideChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.UpdateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.dto.response.HideChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ChecklistTemplateVersionSnapshotResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Override
    @Transactional(readOnly = true)
    public Page<AdminChecklistTemplateDetailResponse> list(
            ChecklistTemplateStatus status, ContentStage stage, Pageable pageable) {
        Page<ChecklistTemplate> templates = checklistTemplateRepository
                .findAdminByOptionalStageAndStatus(stage, status, null, pageable);
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
        ChecklistTemplate template = ChecklistTemplate.builder()
                .name(request.name())
                .description(request.description())
                .stage(request.stage())
                .status(ChecklistTemplateStatus.DRAFT)
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

        // Same separation-of-duties guard as ContentItem.updateContent (BR-CNT-006): a Content Admin
        // may only work a draft or submit it for review — publication is a System Admin decision
        // made exclusively through ChecklistTemplateApprovalService.decide().
        if (template.getStatus() != ChecklistTemplateStatus.DRAFT
                && template.getStatus() != ChecklistTemplateStatus.PENDING_REVIEW) {
            throw ContentException.checklistTemplateInvalidStatusTransition();
        }
        if (request.status() != ChecklistTemplateStatus.DRAFT
                && request.status() != ChecklistTemplateStatus.PENDING_REVIEW) {
            throw ContentException.checklistTemplateInvalidStatusTransition();
        }

        template.setName(request.name());
        template.setDescription(request.description());
        template.setStage(request.stage());
        template.setStatus(request.status());
        int currentVersion = template.getVersionNo() == null ? 1 : template.getVersionNo();
        template.setVersionNo(currentVersion + 1);
        if (request.status() == ChecklistTemplateStatus.PENDING_REVIEW) {
            clearReviewFeedback(template);
        }
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        // null leaves entries untouched; a non-null list is the complete active set. Existing
        // rows are reconciled in place so imported personal checklist foreign keys remain valid.
        List<ChecklistItem> currentItems;
        if (request.items() != null) {
            currentItems = reconcileItems(request.items(), saved);
        } else {
            currentItems = checklistItemRepository.findByTemplate_IdOrderByOrder(id);
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

        // ADR-CHK-002: idempotency guard — already ARCHIVED is rejected, not silently re-applied
        if (template.getStatus() == ChecklistTemplateStatus.ARCHIVED) {
            throw ContentException.checklistTemplateAlreadyArchived();
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw ContentException.checklistTemplateArchiveReasonRequired();
        }

        ChecklistTemplateStatus previousStatus = template.getStatus();
        // ADR-CHK-002: soft-delete only. Canonical template entries and imported personal
        // preparation_checklist_items remain stable when a template is archived.
        template.setStatus(ChecklistTemplateStatus.ARCHIVED);
        clearReviewFeedback(template);
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        Instant archivedAt = Instant.now();
        auditService.log(AuditAction.CHECKLIST_TEMPLATE_ARCHIVED, adminUserId,
                "ChecklistTemplate", saved.getId().toString(), "reason=" + request.reason() + " previousStatus=" + previousStatus);

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
                        id, "ChecklistTemplate", Set.of(AuditAction.CHECKLIST_TEMPLATE_CREATED, AuditAction.CHECKLIST_TEMPLATE_UPDATED)).stream()
                .map(this::toVersionResponse)
                .flatMap(java.util.Optional::stream)
                .toList();
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
            return java.util.Optional.of(new ChecklistTemplateVersionSnapshotResponse(snapshot.versionNo(), snapshot.name(),
                    snapshot.stage(), snapshot.status(), snapshot.itemCount(), auditLog.getActorUserId(), auditLog.getCreatedAt()));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private AdminChecklistTemplateDetailResponse toResponseWithItems(ChecklistTemplate template) {
        List<ChecklistItem> items = checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId());
        return contentMapper.toAdminChecklistTemplateDetailResponse(template, items);
    }

    private List<ChecklistItem> toEntities(List<ChecklistItemRequest> items, ChecklistTemplate template) {
        List<ChecklistItem> entities = new ArrayList<>();
        for (ChecklistItemRequest item : items) {
            entities.add(ChecklistItem.builder()
                    .template(template)
                    .itemText(item.itemText())
                    .order(item.order())
                    .isRequired(item.isRequired())
                    .isActive(true)
                    .build());
        }
        return entities;
    }

    private List<ChecklistItem> reconcileItems(
            List<ChecklistItemRequest> requestedItems, ChecklistTemplate template) {
        List<ChecklistItem> existingItems =
                checklistItemRepository.findAllByTemplateIdOrderByOrder(template.getId());
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
            item.setOrder(requested.order());
            item.setIsRequired(requested.isRequired());
            item.setIsActive(true);
            activeItems.add(item);
        }
        if (!itemsToSave.isEmpty()) {
            checklistItemRepository.saveAll(itemsToSave);
        }
        return activeItems;
    }
}
