package com.carebridge.backend.checklist.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.checklist.dto.*;
import com.carebridge.backend.checklist.entity.ChecklistCategory;
import com.carebridge.backend.checklist.entity.UserChecklistItem;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.service.IUserChecklistItemService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserChecklistItemServiceImpl implements IUserChecklistItemService {

    private final UserChecklistItemRepository checklistRepository;
    private final ChecklistItemRepository templateItemRepository;
    private final AuditService auditService;

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
        return request.templateItemIds().stream()
                .map(templateId -> {
                    ChecklistItem template = templateItemRepository.findById(templateId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "CHECKLIST-004: Template item not found: " + templateId));

                    var item = UserChecklistItem.builder()
                            .ownerUserId(userId)
                            .journeyId(request.journeyId())
                            .babyId(request.babyId())
                            .templateItemId(templateId)
                            .itemText(template.getItemText())
                            .category(ChecklistCategory.GENERAL)
                            .itemOrder(template.getOrder() != null ? template.getOrder() : 0)
                            .build();

                    var saved = checklistRepository.save(item);
                    auditService.log(AuditAction.CHECKLIST_ITEM_ADDED, userId,
                            "UserChecklistItem", saved.getId().toString(), "imported");
                    return toResponse(saved);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> listItems(UUID userId, UUID journeyId, UUID babyId) {
        return checklistRepository.findByOwnerFiltered(userId, journeyId, babyId)
                .stream()
                .map(this::toResponse)
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
        return toResponse(saved);
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
        return toResponse(saved);
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

    private ChecklistItemResponse toResponse(UserChecklistItem item) {
        return new ChecklistItemResponse(
                item.getId(),
                item.getOwnerUserId(),
                item.getJourneyId(),
                item.getBabyId(),
                item.getTemplateItemId(),
                item.getItemText(),
                item.getCategory() != null ? item.getCategory().name() : null,
                item.isCompleted(),
                item.getCompletedAt(),
                item.getItemOrder(),
                item.getCreatedAt()
        );
    }
}
