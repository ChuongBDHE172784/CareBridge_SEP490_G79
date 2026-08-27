package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ContentFilterRequest;
import com.carebridge.backend.content.dto.request.ContentSearchRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.dto.response.ContentSearchResponse;
import com.carebridge.backend.content.dto.response.LifecycleContentEnvelope;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateResponse;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContentService {

    /**
     * Returns paginated APPROVED content filtered by type, stage, topicId.
     * Always enforces status=APPROVED (BR-RBAC). authorId never exposed (BR-PRIVACY).
     */
    Page<ContentListResponse> getContents(ContentFilterRequest filter, Pageable pageable);

    /**
     * Returns detail of a single APPROVED content item.
     *
     * @throws ContentException(CNT-003) if not found or not APPROVED
     */
    ContentDetailResponse getContentById(UUID id);

    /**
     * Returns all checklist templates for a given stage.
     *
     * @param stage nullable — if null, returns templates for all stages
     */
    List<ChecklistTemplateResponse> getChecklists(ContentStage stage);

    /**
     * Searches APPROVED content by keyword, stage, topicId, type.
     * Keyword is sanitized (trim + LIKE wildcard escape) before query (ADR-004).
     * Always enforces status=APPROVED (BR-RBAC, C1).
     * Returns empty page (HTTP 200) when no results found — never 404 (C4).
     */
    Page<ContentSearchResponse> searchContent(ContentSearchRequest request, Pageable pageable);

    LifecycleContentEnvelope<Page<ContentListResponse>> getLifecycleContents(
            UUID ownerId, com.carebridge.backend.content.entity.ContentType type,
            UUID topicId, Pageable pageable);

    LifecycleContentEnvelope<List<ChecklistTemplateResponse>> getLifecycleChecklists(UUID ownerId);

    LifecycleContentEnvelope<ContentDetailResponse> getLifecycleContentById(UUID ownerId, UUID id);

    Page<AdminChecklistTemplateResponse> getAdminChecklists(
            ContentStage stage, ChecklistTemplateStatus status, Pageable pageable);

    Page<AdminChecklistTemplateResponse> getAdminChecklists(
            ContentStage stage, ChecklistTemplateStatus status, String keyword, Pageable pageable);
}
