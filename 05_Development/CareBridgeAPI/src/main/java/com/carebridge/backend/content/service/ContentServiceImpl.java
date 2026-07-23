package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ContentFilterRequest;
import com.carebridge.backend.content.dto.request.ContentSearchRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.dto.response.ContentSearchResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ContentRepository;
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
                ? checklistTemplateRepository.findByStatus(ContentStatus.APPROVED)
                : checklistTemplateRepository.findByStageAndStatus(stage, ContentStatus.APPROVED);

        return templates.stream()
                .map(template -> {
                    List<ChecklistItem> items =
                            checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId());
                    return contentMapper.toChecklistTemplateResponse(template, items);
                })
                .toList();
    }
}
