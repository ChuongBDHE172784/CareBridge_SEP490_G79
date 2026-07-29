package com.carebridge.backend.checklist.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.dto.*;
import com.carebridge.backend.checklist.entity.ChecklistCategory;
import com.carebridge.backend.checklist.entity.UserChecklistItem;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.service.IUserChecklistItemService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import com.carebridge.backend.content.policy.ResolvedLifecycleContext;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserChecklistItemServiceImpl implements IUserChecklistItemService {

    private final UserChecklistItemRepository checklistRepository;
    private final ChecklistItemRepository templateItemRepository;
    private final AuditService auditService;
    private final LifecycleContentStageResolver lifecycleContentStageResolver;
    private final BabyProfileRepository babyProfileRepository;

    @Override
    public ChecklistItemResponse addItem(AddChecklistItemRequest request, UUID userId) {
        var item = UserChecklistItem.builder()
                .ownerUserId(userId)
                .journeyId(request.journeyId())
                .babyId(request.babyId())
                .itemText(request.itemText())
                .category(request.category() != null ? request.category() : ChecklistCategory.GENERAL)
                .itemOrder(request.itemOrder() != null ? request.itemOrder() : 0)
                .build();

        var saved = checklistRepository.save(item);
        auditService.log(AuditAction.CHECKLIST_ITEM_ADDED, userId,
                "UserChecklistItem", saved.getId().toString(), "added");
        log.info("Checklist item added: itemId={}, userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Override
    public List<ChecklistItemResponse> importFromTemplate(ImportFromTemplateRequest request, UUID userId) {
        if (request.journeyId() != null && request.babyId() != null) {
            throw invalidImport();
        }

        UUID journeyId = null;
        UUID babyId = null;
        ContentStage contextStage;
        if (request.babyId() != null) {
            var baby = babyProfileRepository.findOwnedActiveByIdForUpdate(request.babyId(), userId)
                    .orElseThrow(this::unavailableTemplateItem);
            babyId = baby.getId();
            contextStage = ContentStage.POSTPARTUM;
        } else {
            ResolvedLifecycleContext context;
            try {
                context = lifecycleContentStageResolver.resolveForUpdate(userId);
            } catch (ContentException exception) {
                if (request.journeyId() != null && "CNT-013".equals(exception.getCode())) {
                    throw unavailableTemplateItem();
                }
                throw exception;
            }
            if (request.journeyId() != null && !request.journeyId().equals(context.journeyId())) {
                throw unavailableTemplateItem();
            }
            journeyId = context.journeyId();
            contextStage = context.stage();
        }

        LinkedHashSet<UUID> distinctTemplateItemIds = new LinkedHashSet<>(request.templateItemIds());
        List<UUID> sortedIds = new ArrayList<>(distinctTemplateItemIds);
        sortedIds.sort(Comparator.naturalOrder());
        List<ChecklistItem> available = templateItemRepository.findAllAvailableByIdInForUpdate(
                sortedIds, ChecklistTemplateStatus.APPROVED, contextStage);
        if (available.size() != distinctTemplateItemIds.size()) {
            throw unavailableTemplateItem();
        }
        Map<UUID, ChecklistItem> byId = available.stream()
                .collect(Collectors.toMap(ChecklistItem::getId, Function.identity()));
        final UUID resolvedJourneyId = journeyId;
        final UUID resolvedBabyId = babyId;

        return distinctTemplateItemIds.stream().map(templateItemId -> {
            ChecklistItem template = byId.get(templateItemId);
            if (template == null || template.getItemText() == null || template.getItemText().isBlank()) {
                throw unavailableTemplateItem();
            }
            int inserted;
            UserChecklistItem persisted;
            if (resolvedBabyId != null) {
                inserted = checklistRepository.insertBabyImportedIfAbsent(
                        UUID.randomUUID(), userId, resolvedBabyId, templateItemId,
                        template.getItemText(), template.getOrder() != null ? template.getOrder() : 0);
                persisted = checklistRepository.findBabyImportedByExactScope(
                                userId, resolvedBabyId, templateItemId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Imported baby checklist item could not be resolved"));
                if (persisted.getJourneyId() != null) {
                    persisted.setJourneyId(null);
                }
            } else {
                inserted = checklistRepository.insertJourneyImportedIfAbsent(
                        UUID.randomUUID(), userId, resolvedJourneyId, templateItemId,
                        template.getItemText(), template.getOrder() != null ? template.getOrder() : 0);
                persisted = checklistRepository.findJourneyImportedByExactScope(
                                userId, resolvedJourneyId, templateItemId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Imported journey checklist item could not be resolved"));
            }

            if (inserted == 1) {
                auditService.log(AuditAction.CHECKLIST_ITEM_ADDED, userId,
                        "UserChecklistItem", persisted.getId().toString(), "imported");
            }
            return toResponse(persisted, template);
        }).toList();
    }

    private BusinessException invalidImport() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "CHECKLIST-001",
                "Invalid checklist import request");
    }

    private BusinessException unavailableTemplateItem() {
        return new BusinessException(HttpStatus.NOT_FOUND, "CHECKLIST-007",
                "Template item not found or unavailable");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> listItems(UUID userId, UUID journeyId, UUID babyId) {
        List<UserChecklistItem> items = checklistRepository.findByOwnerFiltered(userId, journeyId, babyId);
        Map<UUID, ChecklistItem> templates = templateItemRepository.findAllWithTemplateByIdIn(items.stream()
                        .map(UserChecklistItem::getTemplateItemId).filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(ChecklistItem::getId, Function.identity()));
        return items.stream()
                .map(item -> toResponse(item, templates.get(item.getTemplateItemId())))
                .toList();
    }

    @Override
    public ChecklistItemResponse toggleComplete(UUID itemId, UUID userId) {
        var item = findOwnedOrThrow(itemId, userId);
        item.setCompleted(!item.isCompleted());
        item.setCompletedAt(item.isCompleted() ? Instant.now() : null);
        var saved = checklistRepository.save(item);
        auditService.log(AuditAction.CHECKLIST_ITEM_COMPLETED, userId,
                "UserChecklistItem", itemId.toString(), String.valueOf(saved.isCompleted()));
        return toResponse(saved, templateFor(saved));
    }

    @Override
    public ChecklistItemResponse updateItem(UUID itemId, UpdateChecklistItemRequest request, UUID userId) {
        var item = findOwnedOrThrow(itemId, userId);

        // C2: template items cannot have itemText or category changed
        if (item.getTemplateItemId() != null) {
            if (request.itemText() != null || request.category() != null) {
                throw new BusinessException(HttpStatus.valueOf(422), "CHECKLIST-006",
                        "CHECKLIST-006: Cannot modify itemText or category of a template-imported item");
            }
        }

        if (request.itemText() != null) item.setItemText(request.itemText());
        if (request.category() != null) item.setCategory(request.category());
        if (request.itemOrder() != null) item.setItemOrder(request.itemOrder());

        var saved = checklistRepository.save(item);
        auditService.log(AuditAction.CHECKLIST_ITEM_UPDATED, userId,
                "UserChecklistItem", itemId.toString(), "updated");
        return toResponse(saved, templateFor(saved));
    }

    @Override
    public void deleteItem(UUID itemId, UUID userId) {
        var item = findOwnedOrThrow(itemId, userId);
        checklistRepository.delete(item);
        auditService.log(AuditAction.CHECKLIST_ITEM_DELETED, userId,
                "UserChecklistItem", itemId.toString(), "deleted");
        log.info("Checklist item deleted: itemId={}, userId={}", itemId, userId);
    }

    // ── Private ────────────────────────────────────────────────────

    private UserChecklistItem findOwnedOrThrow(UUID itemId, UUID userId) {
        return checklistRepository.findByIdAndOwnerUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CHECKLIST-005: Checklist item not found"));
    }

    private ChecklistItem templateFor(UserChecklistItem item) {
        if (item.getTemplateItemId() == null) return null;
        return templateItemRepository.findAllWithTemplateByIdIn(List.of(item.getTemplateItemId()))
                .stream().findFirst().orElse(null);
    }

    private ChecklistItemResponse toResponse(UserChecklistItem item) {
        return toResponse(item, templateFor(item));
    }

    private ChecklistItemResponse toResponse(UserChecklistItem item, ChecklistItem template) {
        return new ChecklistItemResponse(
                item.getId(),
                item.getOwnerUserId(),
                item.getJourneyId(),
                item.getBabyId(),
                item.getTemplateItemId(),
                template == null || template.getTemplate() == null ? null : template.getTemplate().getName(),
                template == null ? null : template.getIsRequired(),
                item.getItemText(),
                item.getCategory() != null ? item.getCategory().name() : null,
                item.isCompleted(),
                item.getCompletedAt(),
                item.getItemOrder(),
                item.getCreatedAt()
        );
    }
}
