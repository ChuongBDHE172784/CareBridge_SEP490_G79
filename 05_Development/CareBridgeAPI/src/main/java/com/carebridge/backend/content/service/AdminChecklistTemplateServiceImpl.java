package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.dto.request.ChecklistItemRequest;
import com.carebridge.backend.content.dto.request.CreateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.HideChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.UpdateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.HideChecklistTemplateResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    @Override
    @Transactional(readOnly = true)
    public Page<ChecklistTemplateResponse> list(ContentStatus status, ContentStage stage, Pageable pageable) {
        Page<ChecklistTemplate> templates = checklistTemplateRepository.findByAdminFilters(stage, status, pageable);
        return templates.map(this::toResponseWithItems);
    }

    @Override
    @Transactional(readOnly = true)
    public ChecklistTemplateResponse getById(UUID id) {
        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);
        return toResponseWithItems(template);
    }

    @Override
    @Transactional
    public ChecklistTemplateResponse create(CreateChecklistTemplateRequest request, UUID adminUserId) {
        ChecklistTemplate template = ChecklistTemplate.builder()
                .name(request.name())
                .description(request.description())
                .stage(request.stage())
                .status(ContentStatus.DRAFT)
                .build();
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        List<ChecklistItemRequest> requestedItems = request.items() == null ? List.of() : request.items();
        List<ChecklistItem> savedItems = requestedItems.isEmpty()
                ? List.of()
                : checklistItemRepository.saveAll(toEntities(requestedItems, saved));

        auditService.log(AuditAction.CHECKLIST_TEMPLATE_CREATED, adminUserId,
                "ChecklistTemplate", saved.getId().toString(), "created");

        return contentMapper.toChecklistTemplateResponse(saved, savedItems);
    }

    @Override
    @Transactional
    public ChecklistTemplateResponse update(UUID id, UpdateChecklistTemplateRequest request, UUID adminUserId) {
        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);

        // Same separation-of-duties guard as ContentItem.updateContent (BR-CNT-006): a Content Admin
        // may only work a draft or submit it for review — publication is a System Admin decision
        // made exclusively through ChecklistTemplateApprovalService.decide().
        if (template.getStatus() != ContentStatus.DRAFT && template.getStatus() != ContentStatus.PENDING_REVIEW) {
            throw ContentException.checklistTemplateInvalidStatusTransition();
        }
        if (request.status() != ContentStatus.DRAFT && request.status() != ContentStatus.PENDING_REVIEW) {
            throw ContentException.checklistTemplateInvalidStatusTransition();
        }

        template.setName(request.name());
        template.setDescription(request.description());
        template.setStage(request.stage());
        template.setStatus(request.status());
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        // null = client did not edit items, leave untouched; non-null (incl. []) = full replace.
        // ChecklistItem has no back-reference collection on ChecklistTemplate to reassign in place
        // (unlike ContentItem.sources, an @ElementCollection) — replace via repository delete+insert.
        List<ChecklistItem> currentItems;
        if (request.items() != null) {
            List<ChecklistItem> existing = checklistItemRepository.findByTemplate_IdOrderByOrder(id);
            if (!existing.isEmpty()) {
                checklistItemRepository.deleteAll(existing);
            }
            currentItems = request.items().isEmpty()
                    ? List.of()
                    : checklistItemRepository.saveAll(toEntities(request.items(), saved));
        } else {
            currentItems = checklistItemRepository.findByTemplate_IdOrderByOrder(id);
        }

        auditService.log(AuditAction.CHECKLIST_TEMPLATE_UPDATED, adminUserId,
                "ChecklistTemplate", saved.getId().toString(), "updated");

        return contentMapper.toChecklistTemplateResponse(saved, currentItems);
    }

    @Override
    @Transactional
    public HideChecklistTemplateResponse archive(UUID id, HideChecklistTemplateRequest request, UUID adminUserId) {
        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);

        // ADR-CHK-002: idempotency guard — already ARCHIVED is rejected, not silently re-applied
        if (template.getStatus() == ContentStatus.ARCHIVED) {
            throw ContentException.checklistTemplateAlreadyArchived();
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw ContentException.checklistTemplateArchiveReasonRequired();
        }

        ContentStatus previousStatus = template.getStatus();
        // ADR-CHK-002: soft-delete only — checklist_items are NOT touched, so UC-50's
        // Canonical preparation_checklist_items remain stable when a template is archived.
        template.setStatus(ContentStatus.ARCHIVED);
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        Instant archivedAt = Instant.now();
        auditService.log(AuditAction.CHECKLIST_TEMPLATE_ARCHIVED, adminUserId,
                "ChecklistTemplate", saved.getId().toString(), "reason=" + request.reason() + " previousStatus=" + previousStatus);

        return new HideChecklistTemplateResponse(
                saved.getId(), previousStatus, saved.getStatus(), request.reason(), adminUserId, archivedAt);
    }

    private ChecklistTemplateResponse toResponseWithItems(ChecklistTemplate template) {
        List<ChecklistItem> items = checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId());
        return contentMapper.toChecklistTemplateResponse(template, items);
    }

    private List<ChecklistItem> toEntities(List<ChecklistItemRequest> items, ChecklistTemplate template) {
        List<ChecklistItem> entities = new ArrayList<>();
        for (ChecklistItemRequest item : items) {
            entities.add(ChecklistItem.builder()
                    .template(template)
                    .itemText(item.itemText())
                    .order(item.order())
                    .isRequired(item.isRequired())
                    .build());
        }
        return entities;
    }
}
