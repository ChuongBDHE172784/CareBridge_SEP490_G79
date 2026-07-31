package com.carebridge.backend.checklist.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.dto.*;
import com.carebridge.backend.checklist.entity.ChecklistCategory;
import com.carebridge.backend.checklist.entity.UserChecklistItem;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.service.IUserChecklistItemService;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
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
    private final UnifiedTaskMutationPolicy mutationPolicy;

    @Override
    public ChecklistItemResponse addItem(AddChecklistItemRequest request, UUID userId) {
        throw retiredMutation();
    }

    @Override
    public List<ChecklistItemResponse> importFromTemplate(ImportFromTemplateRequest request, UUID userId) {
        throw retiredMutation();
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
        throw retiredMutation();
    }

    @Override
    public ChecklistItemResponse updateItem(UUID itemId, UpdateChecklistItemRequest request, UUID userId) {
        throw retiredMutation();
    }

    @Override
    public void deleteItem(UUID itemId, UUID userId) {
        throw retiredMutation();
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

    private ChecklistOrigin originOf(UserChecklistItem item) {
        return item.getTemplateItemId() == null
                ? ChecklistOrigin.USER_CREATED
                : ChecklistOrigin.SYSTEM_TEMPLATE;
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
                item.getCreatedAt(),
                null,
                originOf(item).name()
        );
    }

    private static BusinessException retiredMutation() {
        return new BusinessException(HttpStatus.GONE, "CHECKLIST_LEGACY_ROUTE_RETIRED",
                "Use the unified Today task APIs");
    }
}
