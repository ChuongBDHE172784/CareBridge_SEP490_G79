package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ContentFilterRequest;
import com.carebridge.backend.content.dto.request.ContentSearchRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.dto.response.ContentSearchResponse;
import com.carebridge.backend.content.dto.response.LifecycleContentEnvelope;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.repository.TemplateItemCount;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final ContentRepository contentRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ContentMapper contentMapper;
    private final LifecycleContentStageResolver lifecycleContentStageResolver;

    @Override
    @Transactional(readOnly = true)
    public Page<ContentListResponse> getContents(ContentFilterRequest filter, Pageable pageable) {
        Page<ContentItem> items = contentRepository.findByFilters(
                filter.getType(),
                filter.getStage(),
                filter.getTopicId(),
                ContentStatus.APPROVED,  // always APPROVED — BR-RBAC (ADR-002)
                pageable);
        return items.map(contentMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentDetailResponse getContentById(UUID id) {
        ContentItem item = contentRepository.findByIdAndStatus(id, ContentStatus.APPROVED)
                .orElseThrow(ContentException::contentNotFound);
        return contentMapper.toDetailResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentSearchResponse> searchContent(ContentSearchRequest request, Pageable pageable) {
        String sanitized = sanitizeKeyword(request.getKeyword());
        Page<ContentItem> items = contentRepository.searchByFilters(
                sanitized,
                request.getType(),
                request.getStage(),
                request.getTopicId(),
                ContentStatus.APPROVED,  // C1: always APPROVED — BR-RBAC (ADR-003)
                pageable);
        return items.map(contentMapper::toSearchResponse);
    }

    // ADR-004: trim whitespace; escape LIKE wildcards % and _ to prevent unintended matches
    private String sanitizeKeyword(String keyword) {
        if (keyword == null) return null;
        return keyword.trim()
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistTemplateResponse> getChecklists(ContentStage stage) {
        List<ChecklistTemplate> templates = (stage == null)
                ? checklistTemplateRepository.findByStatusOrderByUpdatedAtDesc(ChecklistTemplateStatus.APPROVED)
                : checklistTemplateRepository.findByStageAndStatusOrderByUpdatedAtDesc(
                        stage, ChecklistTemplateStatus.APPROVED);
        return shapeApprovedChecklists(templates);
    }

    @Override
    @Transactional(readOnly = true)
    public LifecycleContentEnvelope<Page<ContentListResponse>> getLifecycleContents(
            UUID ownerId, ContentType type, UUID topicId, Pageable pageable) {
        ContentStage stage = lifecycleContentStageResolver.resolve(ownerId);
        Page<ContentListResponse> payload = contentRepository.findByFilters(
                        type, stage, topicId, ContentStatus.APPROVED, pageable)
                .map(contentMapper::toListResponse);
        return new LifecycleContentEnvelope<>(stage, payload);
    }

    @Override
    @Transactional(readOnly = true)
    public LifecycleContentEnvelope<List<ChecklistTemplateResponse>> getLifecycleChecklists(UUID ownerId) {
        ContentStage stage = lifecycleContentStageResolver.resolve(ownerId);
        List<ChecklistTemplate> templates = checklistTemplateRepository
                .findByStageAndStatusOrderByUpdatedAtDesc(stage, ChecklistTemplateStatus.APPROVED);
        return new LifecycleContentEnvelope<>(stage, shapeApprovedChecklists(templates));
    }

    @Override
    @Transactional(readOnly = true)
    public LifecycleContentEnvelope<ContentDetailResponse> getLifecycleContentById(UUID ownerId, UUID id) {
        ContentStage stage = lifecycleContentStageResolver.resolve(ownerId);
        ContentItem item = contentRepository.findByIdAndStageAndStatus(id, stage, ContentStatus.APPROVED)
                .orElseThrow(ContentException::contentNotFound);
        return new LifecycleContentEnvelope<>(stage, contentMapper.toDetailResponse(item));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminChecklistTemplateResponse> getAdminChecklists(
            ContentStage stage, ChecklistTemplateStatus status, Pageable pageable) {
        Page<ChecklistTemplate> templates = checklistTemplateRepository
                .findAdminByOptionalStageAndStatus(stage, status, pageable);
        Set<UUID> ids = templates.stream().map(ChecklistTemplate::getId).collect(Collectors.toSet());
        Map<UUID, Long> counts = ids.isEmpty() ? Map.of() : checklistItemRepository.countByTemplateIds(ids)
                .stream().collect(Collectors.toMap(TemplateItemCount::getTemplateId, TemplateItemCount::getItemCount));
        return templates.map(template -> new AdminChecklistTemplateResponse(
                template.getId(), template.getName(), template.getStage(), template.getStatus(),
                template.getDescription(), template.getVersionNo(), template.getUpdatedAt(),
                counts.getOrDefault(template.getId(), 0L)));
    }

    private List<ChecklistTemplateResponse> shapeApprovedChecklists(List<ChecklistTemplate> templates) {
        Set<UUID> ids = templates.stream().map(ChecklistTemplate::getId).collect(Collectors.toSet());
        List<ChecklistItem> items = ids.isEmpty() ? List.of() : checklistItemRepository
                .findAllByApprovedTemplateIds(ids, ChecklistTemplateStatus.APPROVED);
        Map<UUID, List<ChecklistItem>> byTemplate = items.stream()
                .collect(Collectors.groupingBy(item -> item.getTemplate().getId()));
        return templates.stream()
                .map(template -> contentMapper.toChecklistTemplateResponse(
                        template, byTemplate.getOrDefault(template.getId(), List.of())))
                .toList();
    }
}
